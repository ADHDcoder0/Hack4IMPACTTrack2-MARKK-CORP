package com.example.scrapsetu.data.repo

import com.example.scrapsetu.data.model.AnalyticsResponse
import com.example.scrapsetu.data.model.BuyerMatch
import com.example.scrapsetu.data.model.BuyerSuggestions
import com.example.scrapsetu.data.model.DemandForecast
import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.data.model.Match
import com.example.scrapsetu.data.model.PriceSuggestion
import com.example.scrapsetu.data.model.SellerAnalytics
import com.example.scrapsetu.data.model.SmartMatchResult
import com.example.scrapsetu.data.model.TrustScore
import com.example.scrapsetu.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class GroqAnalyticsRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val httpClient: HttpClient,
    @Named("groq_api_key") private val groqApiKey: String,
    @Named("gemini_api_key") private val geminiApiKey: String
) {

    enum class AnalyticsSource {
        GROQ,
        GEMINI,
        FALLBACK
    }

    data class AnalyticsFetchResult(
        val data: AnalyticsResponse,
        val source: AnalyticsSource
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun fetchAnalytics(userId: String, listingId: String? = null): Result<AnalyticsFetchResult> = runCatching {
        val user = fetchUser(userId)
        val userListings = fetchUserListings(userId)
        val recentMatches = fetchRecentMatches(userListings.mapNotNull { it.id })
        val targetListing = if (!listingId.isNullOrBlank()) {
            userListings.firstOrNull { it.id == listingId } ?: fetchListing(listingId)
        } else {
            userListings.firstOrNull()
        }

        val completedDeals = recentMatches.count { it.status.equals("confirmed", ignoreCase = true) }
        val totalRequests = recentMatches.size

        val prompt = buildPrompt(
            user = user,
            listings = userListings,
            matches = recentMatches,
            targetListing = targetListing,
            completedDeals = completedDeals,
            totalRequests = totalRequests
        )

        val aiResponse = runCatching {
            val rawJson = callGroq(prompt)
            parseAnalyticsOrNull(rawJson)
        }.getOrNull()

        if (aiResponse != null) {
            AnalyticsFetchResult(
                data = aiResponse,
                source = AnalyticsSource.GROQ
            )
        } else {
            val geminiResponse = runCatching {
                if (geminiApiKey.isBlank()) {
                    null
                } else {
                    val rawJson = callGemini(prompt)
                    parseAnalyticsOrNull(rawJson)
                }
            }.getOrNull()

            if (geminiResponse != null) {
                AnalyticsFetchResult(
                    data = geminiResponse,
                    source = AnalyticsSource.GEMINI
                )
            } else {
            AnalyticsFetchResult(
                data = buildFallbackAnalytics(
                    user = user,
                    listings = userListings,
                    matches = recentMatches,
                    targetListing = targetListing,
                    completedDeals = completedDeals,
                    totalRequests = totalRequests
                ),
                source = AnalyticsSource.FALLBACK
            )
            }
        }
    }

    private suspend fun fetchUser(userId: String): User {
        return supabase.postgrest["users"]
            .select { filter { eq("id", userId) } }
            .decodeSingle<User>()
    }

    private suspend fun fetchUserListings(userId: String): List<Listing> {
        return supabase.postgrest["listings"]
            .select {
                filter { eq("supplier_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(50)
            }
            .decodeList<Listing>()
    }

    private suspend fun fetchListing(listingId: String): Listing? {
        return supabase.postgrest["listings"]
            .select { filter { eq("id", listingId) } }
            .decodeSingleOrNull<Listing>()
    }

    private suspend fun fetchRecentMatches(listingIds: List<String>): List<Match> {
        if (listingIds.isEmpty()) return emptyList()

        val listingIdSet = listingIds.toSet()
        return supabase.postgrest["matches"]
            .select {
                order("created_at", Order.DESCENDING)
                limit(100)
            }
            .decodeList<Match>()
            .filter { it.listingId in listingIdSet }
    }

    private fun buildPrompt(
        user: User,
        listings: List<Listing>,
        matches: List<Match>,
        targetListing: Listing?,
        completedDeals: Int,
        totalRequests: Int
    ): String {
        val listingSummary = listings.take(5).joinToString("\n") { listing ->
            val listingId = listing.id.orEmpty().take(8)
            "- [$listingId] ${listing.wasteType}, qty=${listing.quantityKg}, loc=${listing.location}, status=${listing.status}, requests=${matches.count { it.listingId == listing.id }}"
        }

        val matchSummary = matches.take(10).joinToString("\n") { match ->
            "- listing=${match.listingId.take(8)}, buyer=${match.buyerId.take(8)}, status=${match.status}"
        }

        val targetBlock = if (targetListing != null) {
            val targetId = targetListing.id.orEmpty()
            """
TARGET LISTING (use for smart_match + price_suggestion + buyer_suggestions):
  id: $targetId
  waste_type: ${targetListing.wasteType}
  quantity: ${targetListing.quantityKg}
  location: ${targetListing.location}
  status: ${targetListing.status}
  image_available: ${targetListing.imageUrl.isNotBlank()}
  request_count: ${matches.count { it.listingId == targetListing.id }}
""".trimIndent()
        } else {
            "TARGET LISTING: none"
        }

        val wasteContext = targetListing?.wasteType?.ifBlank { "this waste category" } ?: "this waste category"

        return """
You are an analytics engine for ScrapSetu, a B2B waste marketplace in India.
Respond ONLY with a single valid JSON object with double quotes.

CONTEXT:
USER:
  id: ${user.id}
  name: ${user.name}
  role: ${user.role}
  location: ${user.location.ifBlank { "unknown" }}
  completed_deals: $completedDeals
  total_requests: $totalRequests
  total_listings: ${listings.size}

$targetBlock

RECENT LISTINGS (up to 5):
$listingSummary

RECENT MATCH ACTIVITY (up to 10):
$matchSummary

TASK:
Return a JSON object with exactly these keys:
{
  "smart_match": {
    "reliability_score": <int 0-100>,
    "estimated_eta": "<string like '2-4 days'>",
    "reason": "<1 sentence>",
    "confidence": "<'high'|'medium'|'low'>"
  },
  "price_suggestion": {
    "min_price_inr": <int>,
    "max_price_inr": <int>,
    "unit": "<'per kg'|'per tonne'|'per unit'>",
    "basis": "<1 sentence why>"
  },
  "demand_forecast": {
    "trend": "<'rising'|'stable'|'falling'>",
    "confidence_pct": <int 0-100>,
    "insight": "<1 sentence about $wasteContext demand in India>"
  },
  "buyer_suggestions": {
    "top_matches": [
      { "buyer_id": "<real buyer id from match data>", "match_score": <int 0-100>, "reason": "<1 sentence>" }
    ],
    "summary": "<1 sentence overall>"
  },
  "seller_analytics": {
    "performance_summary": "<1 sentence with numbers>",
    "suggested_category": "<waste category name>",
    "improvement_tip": "<1 actionable sentence>",
    "listing_quality_score": <int 0-100>
  },
  "trust_score": {
    "score": <int 0-100>,
    "tier": "<'Bronze'|'Silver'|'Gold'|'Verified'>",
    "factors": ["<factor 1>", "<factor 2>", "<factor 3>"],
    "next_action": "<1 actionable sentence>"
  }
}

Rules:
- Base all numbers on real context above.
- Use real buyer ids from match data for top_matches, max 3.
- If no match data exists, return an empty array for top_matches.
- Keep all text fields concise and direct. No fluff.
- For analysis text, provide only 2-3 short key points total across the response.
- Use trust_score.factors for those key points (2 or 3 items, each <= 10 words).
- Keep each narrative string to one short sentence (<= 14 words).
- Return only JSON.
""".trimIndent()
    }

    private suspend fun callGroq(prompt: String): String {
        @Serializable
        data class GroqMessage(val role: String, val content: String)

        @Serializable
        data class ResponseFormat(val type: String)

        @Serializable
        data class GroqRequest(
            val model: String,
            val messages: List<GroqMessage>,
            @SerialName("max_tokens") val maxTokens: Int,
            val temperature: Double,
            @SerialName("response_format") val responseFormat: ResponseFormat
        )

        val request = GroqRequest(
            model = "llama-3.3-70b-versatile",
            messages = listOf(
                GroqMessage(
                    role = "system",
                    content = "You are a strict JSON generator. Return only one JSON object and no extra text."
                ),
                GroqMessage("user", prompt)
            ),
            maxTokens = 450,
            temperature = 0.1,
            responseFormat = ResponseFormat(type = "json_object")
        )

        val response = httpClient.post("https://api.groq.com/openai/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            headers {
                append(HttpHeaders.Authorization, "Bearer $groqApiKey")
            }
            setBody(request)
        }

        val body = response.body<JsonObject>()
        val firstChoiceMessage = body["choices"]
            ?.jsonArray?.getOrNull(0)
            ?.jsonObject?.get("message")
            ?.jsonObject

        val content = firstChoiceMessage
            ?.get("content")
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()

        if (!content.isNullOrBlank()) {
            return sanitizeGroqJson(content)
        }

        // Some models occasionally return wrapped JSON in non-content fields.
        val fallbackSerialized = body.toString()
        val fallbackJson = extractFirstJsonObject(fallbackSerialized)
        if (fallbackJson != null) {
            return fallbackJson
        }

        throw IllegalStateException("Groq returned an empty response")
    }

    private suspend fun callGemini(prompt: String): String {
        val escapedPrompt = prompt
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
            "gemini-1.5-flash:generateContent?key=$geminiApiKey"

        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(
                """{"contents":[{"parts":[{"text":"$escapedPrompt"}]}],"generationConfig":{"temperature":0.1,"maxOutputTokens":500}}"""
            )
        }

        val body = response.body<JsonObject>()
        val content = body["candidates"]
            ?.jsonArray?.getOrNull(0)
            ?.jsonObject?.get("content")
            ?.jsonObject?.get("parts")
            ?.jsonArray?.getOrNull(0)
            ?.jsonObject?.get("text")
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()

        if (content.isNullOrBlank()) {
            throw IllegalStateException("Gemini returned an empty response")
        }

        return sanitizeGroqJson(content)
    }

    private fun parseAnalyticsOrNull(rawContent: String): AnalyticsResponse? {
        val sanitized = sanitizeGroqJson(rawContent)
        val jsonCandidate = extractFirstJsonObject(sanitized) ?: sanitized

        return runCatching {
            json.decodeFromString<AnalyticsResponse>(jsonCandidate)
        }.getOrNull()
    }

    private fun extractFirstJsonObject(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null

        return text.substring(start, end + 1).trim()
    }

    private fun buildFallbackAnalytics(
        user: User,
        listings: List<Listing>,
        matches: List<Match>,
        targetListing: Listing?,
        completedDeals: Int,
        totalRequests: Int
    ): AnalyticsResponse {
        val averagePrice = listings
            .map { it.pricePerKg }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?: 80.0

        val chosenPrice = (targetListing?.pricePerKg ?: averagePrice).coerceAtLeast(1.0)
        val minPrice = (chosenPrice * 0.9).roundToInt()
        val maxPrice = (chosenPrice * 1.1).roundToInt().coerceAtLeast(minPrice + 1)

        val reliability = (55 + completedDeals * 8 - (totalRequests - completedDeals).coerceAtLeast(0) * 2)
            .coerceIn(35, 95)
        val confidence = when {
            reliability >= 80 -> "high"
            reliability >= 60 -> "medium"
            else -> "low"
        }

        val confirmRatio = if (totalRequests > 0) {
            completedDeals.toDouble() / totalRequests
        } else {
            0.0
        }

        val trend = when {
            confirmRatio >= 0.5 -> "rising"
            confirmRatio >= 0.25 -> "stable"
            else -> "falling"
        }

        val topBuyerMatches = matches
            .groupBy { it.buyerId }
            .map { (buyerId, buyerMatches) ->
                val buyerCompleted = buyerMatches.count { it.status.equals("confirmed", ignoreCase = true) }
                val score = (50 + buyerCompleted * 10 + buyerMatches.size * 5).coerceIn(40, 95)
                BuyerMatch(
                    buyerId = buyerId,
                    matchScore = score,
                    reason = "Matched in ${buyerMatches.size} requests with ${buyerCompleted} confirmed deals."
                )
            }
            .sortedByDescending { it.matchScore }
            .take(3)

        val trustScore = (45 + completedDeals * 10 + listings.size * 2).coerceIn(30, 98)
        val trustTier = when {
            trustScore >= 90 -> "Verified"
            trustScore >= 75 -> "Gold"
            trustScore >= 60 -> "Silver"
            else -> "Bronze"
        }

        val wasteContext = targetListing?.wasteType?.ifBlank { "selected category" } ?: "selected category"

        return AnalyticsResponse(
            smartMatch = SmartMatchResult(
                reliabilityScore = reliability,
                estimatedEta = if (reliability >= 75) "2-4 days" else "4-7 days",
                reason = "Generated from recent listing and match history while live AI output is unavailable.",
                confidence = confidence
            ),
            priceSuggestion = PriceSuggestion(
                minPriceInr = minPrice,
                maxPriceInr = maxPrice,
                unit = "per kg",
                basis = "Estimated from your active listing prices and recent match outcomes."
            ),
            demandForecast = DemandForecast(
                trend = trend,
                confidencePct = (50 + listings.size * 2 + completedDeals * 4).coerceIn(45, 90),
                insight = "Demand for $wasteContext is inferred from your marketplace activity patterns."
            ),
            buyerSuggestions = BuyerSuggestions(
                topMatches = topBuyerMatches,
                summary = if (topBuyerMatches.isEmpty()) {
                    "No strong buyer pattern yet. Keep listings active to improve match quality."
                } else {
                    "Top buyers were ranked by historical request volume and confirmations."
                }
            ),
            sellerAnalytics = SellerAnalytics(
                performanceSummary = "${user.name.ifBlank { "Seller" }} has $completedDeals confirmed deals out of $totalRequests requests.",
                suggestedCategory = targetListing?.wasteType?.ifBlank { "Mixed Waste" } ?: "Mixed Waste",
                improvementTip = "Add clear photos and precise quantity/location details to increase confirmation rates.",
                listingQualityScore = (55 + listings.size + completedDeals * 6).coerceIn(40, 95)
            ),
            trustScore = TrustScore(
                score = trustScore,
                tier = trustTier,
                factors = listOf(
                    "Confirmed deals: $completedDeals",
                    "Active listings: ${listings.size}",
                    "Total requests: $totalRequests"
                ),
                nextAction = "Complete and fulfill a few more matched requests to improve trust tier."
            )
        )
    }

    private fun sanitizeGroqJson(content: String): String {
        val cleaned = content.trim()
        if (!cleaned.startsWith("```") ) return cleaned

        return cleaned
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
