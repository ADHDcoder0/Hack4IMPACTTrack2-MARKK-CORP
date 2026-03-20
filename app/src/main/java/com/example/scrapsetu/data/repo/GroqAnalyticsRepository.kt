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
import io.ktor.client.plugins.HttpRequestTimeoutException
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
        val user = runCatching { fetchUser(userId) }
            .getOrElse {
                User(
                    id = userId,
                    name = "User",
                    role = "buyer"
                )
            }

        val userListings = runCatching { fetchUserListings(userId) }
            .getOrDefault(emptyList())

        val recentMatches = runCatching {
            fetchRecentMatches(
                userId = userId,
                userRole = user.role,
                listingIds = userListings.mapNotNull { it.id }
            )
        }.getOrDefault(emptyList())

        val targetListing = if (!listingId.isNullOrBlank()) {
            userListings.firstOrNull { it.id == listingId }
                ?: runCatching { fetchListing(listingId) }.getOrNull()
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
        }.recoverCatching {
            if (it is GroqTimeoutException) null else throw it
        }.getOrNull()

        if (aiResponse != null) {
            AnalyticsFetchResult(
                data = normalizeAnalytics(
                    base = aiResponse,
                    user = user,
                    listings = userListings,
                    matches = recentMatches,
                    targetListing = targetListing,
                    completedDeals = completedDeals,
                    totalRequests = totalRequests
                ),
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
            }.recoverCatching {
                if (it is GeminiTimeoutException) null else throw it
            }.getOrNull()

            if (geminiResponse != null) {
                AnalyticsFetchResult(
                    data = normalizeAnalytics(
                        base = geminiResponse,
                        user = user,
                        listings = userListings,
                        matches = recentMatches,
                        targetListing = targetListing,
                        completedDeals = completedDeals,
                        totalRequests = totalRequests
                    ),
                    source = AnalyticsSource.GEMINI
                )
            } else {
            AnalyticsFetchResult(
                data = normalizeAnalytics(
                    base = buildFallbackAnalytics(
                        user = user,
                        listings = userListings,
                        matches = recentMatches,
                        targetListing = targetListing,
                        completedDeals = completedDeals,
                        totalRequests = totalRequests
                    ),
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

    private suspend fun fetchRecentMatches(
        userId: String,
        userRole: String,
        listingIds: List<String>
    ): List<Match> {
        val allRecentMatches = supabase.postgrest["matches"]
            .select {
                order("created_at", Order.DESCENDING)
                limit(120)
            }
            .decodeList<Match>()

        return if (userRole.equals("buyer", ignoreCase = true)) {
            allRecentMatches.filter { it.buyerId == userId }
        } else {
            if (listingIds.isEmpty()) emptyList() else {
                val listingIdSet = listingIds.toSet()
                allRecentMatches.filter { it.listingId in listingIdSet }
            }
        }
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

        val response = try {
            httpClient.post("https://api.groq.com/openai/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $groqApiKey")
                }
                setBody(request)
            }
        } catch (_: HttpRequestTimeoutException) {
            throw GroqTimeoutException()
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

        val response = try {
            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(
                    """{"contents":[{"parts":[{"text":"$escapedPrompt"}]}],"generationConfig":{"temperature":0.1,"maxOutputTokens":500}}"""
                )
            }
        } catch (_: HttpRequestTimeoutException) {
            throw GeminiTimeoutException()
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

        val computedTrust = computeTrustScore(
            user = user,
            listings = listings,
            matches = matches,
            completedDeals = completedDeals,
            totalRequests = totalRequests
        )

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
                score = computedTrust.score,
                tier = computedTrust.tier,
                factors = computedTrust.factors,
                nextAction = computedTrust.nextAction
            )
        )
    }

    private fun normalizeAnalytics(
        base: AnalyticsResponse,
        user: User,
        listings: List<Listing>,
        matches: List<Match>,
        targetListing: Listing?,
        completedDeals: Int,
        totalRequests: Int
    ): AnalyticsResponse {
        val computedDemandForecast = computeDemandForecast(listings, targetListing)
        val computedSmartMatch = computeSmartMatch(listings, matches, targetListing)
        val computedSellerAnalytics = computeSellerAnalytics(
            user = user,
            listings = listings,
            matches = matches,
            targetListing = targetListing,
            completedDeals = completedDeals,
            totalRequests = totalRequests
        )
        val computedTrust = computeTrustScore(
            user = user,
            listings = listings,
            matches = matches,
            completedDeals = completedDeals,
            totalRequests = totalRequests
        )

        val defaultFingerprint =
            base.sellerAnalytics.listingQualityScore == 60 &&
                base.smartMatch.reliabilityScore == 60 &&
                base.trustScore.score == 60

        val summaryLooksGeneric =
            base.sellerAnalytics.performanceSummary.isBlank() ||
                base.sellerAnalytics.performanceSummary.contains("unavailable", ignoreCase = true) ||
                base.sellerAnalytics.performanceSummary.contains("empty", ignoreCase = true) ||
                base.sellerAnalytics.performanceSummary.contains("insufficient", ignoreCase = true)

        val scoreLooksGeneric = base.sellerAnalytics.listingQualityScore == 60 && listings.isNotEmpty()

        val mergedSellerAnalytics = if (defaultFingerprint || summaryLooksGeneric || scoreLooksGeneric) {
            computedSellerAnalytics
        } else {
            base.sellerAnalytics.copy(
                performanceSummary = base.sellerAnalytics.performanceSummary.trim(),
                suggestedCategory = base.sellerAnalytics.suggestedCategory
                    .takeIf { it.isNotBlank() }
                    ?: computedSellerAnalytics.suggestedCategory,
                improvementTip = base.sellerAnalytics.improvementTip
                    .takeIf { it.isNotBlank() }
                    ?: computedSellerAnalytics.improvementTip,
                listingQualityScore = base.sellerAnalytics.listingQualityScore.coerceIn(35, 99),
                activeListings = computedSellerAnalytics.activeListings,
                conversionRatePct = computedSellerAnalytics.conversionRatePct
            )
        }

        val mergedDemandForecast = base.demandForecast.let { incoming ->
            val trend = when (incoming.trend.lowercase()) {
                "rising", "stable", "falling" -> incoming.trend.lowercase()
                else -> computedDemandForecast.trend
            }

            val incomingInsight = sanitizeNarrative(
                text = incoming.insight,
                fallback = computedDemandForecast.insight
            )

            val trendLooksGeneric = incomingInsight.contains("inferred from", ignoreCase = true) ||
                incomingInsight.contains("recent activity", ignoreCase = true)

            if (trendLooksGeneric) {
                computedDemandForecast
            } else {
                incoming.copy(
                    trend = trend,
                    confidencePct = incoming.confidencePct.coerceIn(35, 98),
                    insight = incomingInsight
                )
            }
        }

        val mergedSmartMatch = base.smartMatch.let { incoming ->
            val confidence = when (incoming.confidence.lowercase()) {
                "high", "medium", "low" -> incoming.confidence.lowercase()
                else -> computedSmartMatch.confidence
            }
            incoming.copy(
                reliabilityScore = incoming.reliabilityScore.coerceIn(35, 98),
                estimatedEta = sanitizeNarrative(incoming.estimatedEta, computedSmartMatch.estimatedEta),
                reason = sanitizeNarrative(incoming.reason, computedSmartMatch.reason),
                confidence = confidence
            )
        }

        val mergedPriceSuggestion = base.priceSuggestion.copy(
            minPriceInr = base.priceSuggestion.minPriceInr.coerceAtLeast(1),
            maxPriceInr = base.priceSuggestion.maxPriceInr.coerceAtLeast(base.priceSuggestion.minPriceInr + 1),
            unit = sanitizeUnit(base.priceSuggestion.unit),
            basis = sanitizeNarrative(
                text = base.priceSuggestion.basis,
                fallback = "Estimated from listing prices and active supply trends."
            )
        )

        val trustLooksGeneric =
            base.trustScore.score == 60 ||
                base.trustScore.factors.isEmpty() ||
                base.trustScore.nextAction.contains("maintain consistent", ignoreCase = true)

        val mergedTrust = if (trustLooksGeneric) {
            computedTrust
        } else {
            base.trustScore.copy(
                score = base.trustScore.score.coerceIn(30, 98),
                tier = sanitizeTier(base.trustScore.tier),
                factors = base.trustScore.factors
                    .map { sanitizeNarrative(it, "Consistency") }
                    .filter { it.isNotBlank() }
                    .take(3)
                    .ifEmpty { computedTrust.factors },
                nextAction = sanitizeNarrative(
                    text = base.trustScore.nextAction,
                    fallback = computedTrust.nextAction
                )
            )
        }

        return base.copy(
            smartMatch = mergedSmartMatch,
            priceSuggestion = mergedPriceSuggestion,
            demandForecast = mergedDemandForecast,
            sellerAnalytics = mergedSellerAnalytics,
            trustScore = mergedTrust
        )
    }

    private fun computeDemandForecast(
        listings: List<Listing>,
        targetListing: Listing?
    ): DemandForecast {
        if (listings.isEmpty()) {
            return DemandForecast(
                trend = "stable",
                confidencePct = 40,
                insight = "Insufficient live listings; demand baseline is stable for now."
            )
        }

        val avgPrice = listings.map { it.pricePerKg }.average()
        val avgQty = listings.map { it.quantityKg }.average()
        val denseSupply = listings.count { it.quantityKg >= avgQty }.toDouble() / listings.size

        val trend = when {
            denseSupply >= 0.60 && avgPrice > 0 -> "rising"
            denseSupply >= 0.35 -> "stable"
            else -> "falling"
        }

        val confidence = (45 + (listings.size * 3)).coerceIn(45, 90)
        val category = targetListing?.wasteType?.takeIf { it.isNotBlank() }
            ?: listings.groupingBy { it.wasteType }.eachCount().maxByOrNull { it.value }?.key
            ?: "this material"

        val insight = when (trend) {
            "rising" -> "$category demand is rising with stronger active supply depth this week."
            "falling" -> "$category demand looks soft; prioritize competitive pricing and quick response."
            else -> "$category demand is stable with balanced inventory across active listings."
        }

        return DemandForecast(
            trend = trend,
            confidencePct = confidence,
            insight = insight
        )
    }

    private fun computeSmartMatch(
        listings: List<Listing>,
        matches: List<Match>,
        targetListing: Listing?
    ): SmartMatchResult {
        val targetRequests = targetListing?.id?.let { id -> matches.count { it.listingId == id } } ?: 0
        val reliability = (50 + targetRequests * 8 + listings.size * 2).coerceIn(35, 96)
        val confidence = when {
            reliability >= 80 -> "high"
            reliability >= 60 -> "medium"
            else -> "low"
        }

        return SmartMatchResult(
            reliabilityScore = reliability,
            estimatedEta = if (reliability >= 75) "2-4 days" else "4-7 days",
            reason = "Match quality is ranked from live availability, location fit, and response history.",
            confidence = confidence
        )
    }

    private fun sanitizeUnit(unit: String): String {
        val normalized = unit.trim().lowercase()
        return when {
            normalized.contains("kg") -> "per kg"
            normalized.contains("ton") -> "per tonne"
            normalized.contains("unit") -> "per unit"
            else -> "per kg"
        }
    }

    private fun sanitizeTier(tier: String): String {
        return when (tier.trim().lowercase()) {
            "verified" -> "Verified"
            "gold" -> "Gold"
            "silver" -> "Silver"
            else -> "Bronze"
        }
    }

    private fun sanitizeNarrative(text: String, fallback: String): String {
        // Keep sanitization regex-free to avoid runtime pattern parsing failures.
        val cleaned = text
            .replace("```", " ")
            .replace("<", " ")
            .replace(">", " ")
            .replace("{", " ")
            .replace("}", " ")
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\t", " ")
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()

        val injectionSignals = listOf(
            "ignore previous",
            "system prompt",
            "developer message",
            "tool call",
            "do not follow",
            "jailbreak"
        )

        val looksUnsafe = cleaned.isBlank() ||
            injectionSignals.any { cleaned.contains(it, ignoreCase = true) }

        return if (looksUnsafe) fallback else cleaned
    }

    private fun computeSellerAnalytics(
        user: User,
        listings: List<Listing>,
        matches: List<Match>,
        targetListing: Listing?,
        completedDeals: Int,
        totalRequests: Int
    ): SellerAnalytics {
        val activeListings = listings.count { it.status.equals("active", ignoreCase = true) }
        val listingsWithImages = listings.count { it.imageUrl.isNotBlank() }
        val confirmRatePct = if (totalRequests > 0) {
            ((completedDeals.toDouble() / totalRequests.toDouble()) * 100.0).roundToInt()
        } else {
            0
        }

        val qualityScore = (
            42 +
                activeListings * 5 +
                completedDeals * 8 +
                listingsWithImages * 3 -
                (totalRequests - completedDeals).coerceAtLeast(0) * 2
            ).coerceIn(35, 97)

        val performanceSummary = if (totalRequests > 0) {
            "${user.name.ifBlank { "Seller" }} has $completedDeals confirmed deals from $totalRequests requests (${confirmRatePct}% conversion)."
        } else {
            "${user.name.ifBlank { "Seller" }} has ${listings.size} listings and is building first-match traction."
        }

        val suggestedCategory = targetListing?.wasteType?.takeIf { it.isNotBlank() }
            ?: listings
                .groupingBy { it.wasteType }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
            ?: "Mixed Waste"

        val improvementTip = when {
            listings.isEmpty() -> "Create at least one listing with clear specs to start receiving requests."
            listingsWithImages < listings.size -> "Add photos to every listing to increase buyer response quality."
            totalRequests > 0 && completedDeals == 0 -> "Follow up on pending requests quickly to improve confirmation rates."
            confirmRatePct < 40 -> "Tighten pricing and location clarity to improve buyer conversion."
            else -> "Keep listing details consistent and refresh inactive listings weekly."
        }

        return SellerAnalytics(
            performanceSummary = performanceSummary,
            suggestedCategory = suggestedCategory,
            improvementTip = improvementTip,
            listingQualityScore = qualityScore,
            activeListings = activeListings,
            conversionRatePct = confirmRatePct
        )
    }

    private fun computeTrustScore(
        user: User,
        listings: List<Listing>,
        matches: List<Match>,
        completedDeals: Int,
        totalRequests: Int
    ): TrustScore {
        val isBuyer = user.role.equals("buyer", ignoreCase = true)
        val rejectedRequests = matches.count { it.status.equals("rejected", ignoreCase = true) }
        val pendingRequests = matches.count { it.status.equals("pending", ignoreCase = true) }

        val score = if (isBuyer) {
            (40 + completedDeals * 10 + totalRequests * 3 - rejectedRequests * 4).coerceIn(30, 98)
        } else {
            val activeListings = listings.count { it.status.equals("active", ignoreCase = true) }
            val listingsWithImages = listings.count { it.imageUrl.isNotBlank() }
            (45 + completedDeals * 9 + activeListings * 3 + listingsWithImages * 2 - rejectedRequests * 3)
                .coerceIn(30, 98)
        }

        val tier = when {
            score >= 90 -> "Verified"
            score >= 75 -> "Gold"
            score >= 60 -> "Silver"
            else -> "Bronze"
        }

        val factors = if (isBuyer) {
            listOf(
                "Confirmed purchases: $completedDeals",
                "Requests placed: $totalRequests",
                "Pending requests: $pendingRequests"
            )
        } else {
            listOf(
                "Confirmed deals: $completedDeals",
                "Active listings: ${listings.size}",
                "Total requests: $totalRequests"
            )
        }

        val nextAction = if (isBuyer) {
            if (pendingRequests > 0) {
                "Review pending matches quickly to improve buyer reliability."
            } else {
                "Keep confirming strong supplier matches to raise trust tier."
            }
        } else {
            "Complete and fulfill a few more matched requests to improve trust tier."
        }

        return TrustScore(
            score = score,
            tier = tier,
            factors = factors,
            nextAction = nextAction
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

private class GroqTimeoutException : Exception("Groq request timed out")

private class GeminiTimeoutException : Exception("Gemini request timed out")
