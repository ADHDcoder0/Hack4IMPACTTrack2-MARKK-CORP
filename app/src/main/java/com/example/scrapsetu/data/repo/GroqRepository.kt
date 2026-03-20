package com.example.scrapsetu.data.repo



import com.example.scrapsetu.BuildConfig
import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.data.model.PriceEstimate
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*

class GroqRepository {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
    }

    suspend fun getSmartMatchSuggestion(listing: Listing): String {
        val materialType = listing.wasteType.trim()
        val quantity = listing.quantityKg
        val price = listing.pricePerKg
        val description = listing.description.trim()
        val city = listing.townCity?.trim().orEmpty().ifBlank { listing.location.substringBefore(',').trim() }
        val state = listing.stateCode?.trim().orEmpty().ifBlank { listing.location.substringAfter(',', "").trim() }

        if (materialType.isBlank() || quantity <= 0.0 || price <= 0.0 || city.isBlank() || state.isBlank() || description.isBlank()) {
            return localSmartMatchFallback(listing)
        }

        val prompt = """
            You are a B2B industrial waste matching engine for the Indian market.

            A supplier has listed the following waste material:
            - Material: $materialType
            - Quantity: $quantity kg
            - Price: ₹$price/kg
            - Location: $city, $state
            - Description: $description

            Based on this listing, provide:
            1. Ideal buyer type (recycler / manufacturer / aggregator)
            2. Industries in India that typically purchase this material
            3. One-line reason why this is a strong match opportunity
            4. Demand level: High / Medium / Low

            Keep response under 80 words. Be specific to Indian industrial context.
        """.trimIndent()

        val response = try {
            client.post("https://api.groq.com/openai/v1/chat/completions") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${BuildConfig.GROQ_API_KEY}")
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(
                    buildJsonObject {
                        put("model", "llama-3.3-70b-versatile")
                        put("max_tokens", 150)
                        putJsonArray("messages") {
                            addJsonObject {
                                put("role", "user")
                                put("content", prompt)
                            }
                        }
                    }.toString()
                )
            }
        } catch (_: HttpRequestTimeoutException) {
            return localSmartMatchFallback(listing)
        } catch (_: Exception) {
            return localSmartMatchFallback(listing)
        }

        val json = Json { ignoreUnknownKeys = true }
        val body = json.parseToJsonElement(response.bodyAsText())
        return body
            .jsonObject["choices"]
            ?.jsonArray?.get(0)
            ?.jsonObject?.get("message")
            ?.jsonObject?.get("content")
            ?.jsonPrimitive?.content
            ?.takeIf { it.isNotBlank() }
            ?: localSmartMatchFallback(listing)
    }

    suspend fun getPriceEstimate(materialType: String, quantity: Double, state: String): PriceEstimate {
        val cleanMaterial = materialType.trim()
        val cleanState = state.trim()
        if (cleanMaterial.isBlank() || quantity <= 0.0 || cleanState.isBlank()) {
            return localPriceEstimateFallback(cleanMaterial, quantity)
        }

        val prompt = """
            You are a scrap material pricing engine for the Indian B2B market.
            Material: $cleanMaterial
            Quantity: $quantity kg
            Location: $cleanState
            Return ONLY a JSON object, no explanation, no markdown:
            {"min": 50, "max": 100, "unit": "per kg", "confidence": "medium"}
            Confidence must be: high, medium, or low.
            Base on current Indian scrap market rates.
        """.trimIndent()

        val response = try {
            client.post("https://api.groq.com/openai/v1/chat/completions") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${BuildConfig.GROQ_API_KEY}")
                    append(HttpHeaders.ContentType, "application/json")
                }
                setBody(
                    buildJsonObject {
                        put("model", "llama-3.3-70b-versatile")
                        put("max_tokens", 120)
                        putJsonArray("messages") {
                            addJsonObject {
                                put("role", "user")
                                put("content", prompt)
                            }
                        }
                    }.toString()
                )
            }
        } catch (_: HttpRequestTimeoutException) {
            return localPriceEstimateFallback(cleanMaterial, quantity)
        } catch (_: Exception) {
            return localPriceEstimateFallback(cleanMaterial, quantity)
        }

        val parser = Json { ignoreUnknownKeys = true }
        val rawText = parser.parseToJsonElement(response.bodyAsText())
            .jsonObject["choices"]
            ?.jsonArray?.getOrNull(0)
            ?.jsonObject?.get("message")
            ?.jsonObject?.get("content")
            ?.jsonPrimitive?.content
            ?.trim()
            ?: return localPriceEstimateFallback(cleanMaterial, quantity)

        return runCatching {
            val jsonBody = extractJsonObject(rawText)
            parser.decodeFromString<PriceEstimate>(jsonBody)
        }.getOrElse {
            localPriceEstimateFallback(cleanMaterial, quantity)
        }
    }

    private fun localSmartMatchFallback(listing: Listing): String {
        val material = listing.wasteType.ifBlank { "industrial scrap" }
        val qtyText = if (listing.quantityKg > 0) "${listing.quantityKg} kg" else "flexible volume"
        return "Likely buyers: recyclers and processors for $material. Prioritize nearby verified partners. This listing ($qtyText) has medium demand potential in current market flow."
    }

    private fun localPriceEstimateFallback(materialType: String, quantity: Double): PriceEstimate {
        val material = materialType.lowercase()
        val base = when {
            material.contains("aluminium") || material.contains("aluminum") -> 120
            material.contains("copper") -> 450
            material.contains("steel") || material.contains("iron") -> 35
            material.contains("plastic") || material.contains("hdpe") || material.contains("pet") -> 65
            material.contains("paper") -> 22
            material.contains("textile") -> 28
            else -> 55
        }

        val qtyFactor = when {
            quantity >= 10000 -> 0.94
            quantity >= 3000 -> 0.97
            quantity > 0 -> 1.0
            else -> 1.0
        }

        val min = (base * qtyFactor * 0.92).toInt().coerceAtLeast(1)
        val max = (base * qtyFactor * 1.12).toInt().coerceAtLeast(min + 1)

        return PriceEstimate(
            min = min,
            max = max,
            unit = "per kg",
            confidence = "medium"
        )
    }

    private fun extractJsonObject(raw: String): String {
        val cleaned = raw.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        if (start == -1 || end <= start) {
            throw IllegalStateException("No JSON object found in Groq response")
        }
        return cleaned.substring(start, end + 1)
    }
}