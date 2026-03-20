package com.example.scrapsetu.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.scrapsetu.data.model.DetectionWithPricing
import com.example.scrapsetu.data.model.ImageDetectionResult
import com.example.scrapsetu.data.model.PriceSuggestion
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ImageDetectionRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: HttpClient,
    @Named("gemini_api_key") private val geminiApiKey: String,
    @Named("groq_api_key") private val groqApiKey: String
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun detectAndPrice(imageUri: Uri): Result<DetectionWithPricing> = runCatching {
        if (geminiApiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is missing")
        }
        val base64 = uriToBase64(imageUri)
        val detection = callGeminiVision(base64)
        val pricing = callGroqPricing(detection)
        DetectionWithPricing(detection = detection, pricing = pricing)
    }

    private suspend fun callGeminiVision(base64Image: String): ImageDetectionResult {
        val prompt = """
You are a scrap material classifier for an Indian B2B waste marketplace.
Respond ONLY with a single valid JSON object and no markdown.

Allowed waste_category_slug values:
metal_scrap | e_waste | plastic | paper | glass | rubber | textile | organic | mixed

{
  "waste_category_slug": "<slug>",
  "waste_category_label": "<human label>",
  "quality": "<high|medium|low>",
  "confidence": <int 0-100>,
  "description": "<1 sentence>",
  "quantity_hint": "<example ~50 kg or null>"
}
Return only JSON.
        """.trimIndent()

        val escaped = prompt
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

        val body =
            """{"contents":[{"parts":[{"inline_data":{"mime_type":"image/jpeg","data":"$base64Image"}},{"text":"$escaped"}]}],"generationConfig":{"temperature":0.1,"maxOutputTokens":400}}"""

        val modelCandidates = listOf(
            "gemini-2.0-flash",
            "gemini-1.5-flash-latest",
            "gemini-1.5-flash"
        )

        var lastError: Throwable? = null
        for (model in modelCandidates) {
            val attempt = runCatching {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$geminiApiKey"
                val response = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }

                val payload = response.body<JsonObject>()
                val raw = extractGeminiText(payload)
                json.decodeFromString<ImageDetectionResult>(raw.extractAndParseJson())
            }

            val parsed = attempt.getOrNull()
            if (parsed != null) return parsed
            lastError = attempt.exceptionOrNull()
        }

        throw IllegalStateException(lastError?.message ?: "No Gemini model accepted generateContent request")
    }

    private suspend fun callGroqPricing(detection: ImageDetectionResult): PriceSuggestion {
        val prompt = """
You are a scrap price estimator for the Indian market (2024-2025 rates).
Respond ONLY with a single valid JSON object.

MATERIAL: ${detection.wasteCategoryLabel}, quality: ${detection.quality}
DESCRIPTION: ${detection.description}

Indian ranges: Metal=25-80/kg, E-waste=10-200/kg, Plastic=5-25/kg,
Paper=8-15/kg, Glass=1-5/kg, Rubber=8-20/kg, Textile=3-12/kg, Organic=1-4/kg

{ "min_price_inr":<int>, "max_price_inr":<int>, "unit":"<per kg|per tonne|per unit>", "basis":"<1 sentence>" }
Return only JSON.
        """.trimIndent()

        @Serializable
        data class GroqMessage(val role: String, val content: String)

        @Serializable
        data class GroqReq(
            val model: String,
            val messages: List<GroqMessage>,
            @SerialName("max_tokens") val maxTokens: Int,
            val temperature: Double
        )

        val response = httpClient.post("https://api.groq.com/openai/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            headers { append(HttpHeaders.Authorization, "Bearer $groqApiKey") }
            setBody(
                GroqReq(
                    model = "llama-3.3-70b-versatile",
                    messages = listOf(GroqMessage("user", prompt)),
                    maxTokens = 300,
                    temperature = 0.1
                )
            )
        }

        val raw = response.body<JsonObject>()
            .getValue("choices").jsonArray[0]
            .jsonObject.getValue("message")
            .jsonObject.getValue("content")
            .jsonPrimitive.content

        return json.decodeFromString(raw.extractAndParseJson())
    }

    private fun uriToBase64(uri: Uri): String {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val sample = maxOf(1, maxOf(opts.outWidth, opts.outHeight) / 768)

        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IllegalStateException("Could not read selected image")

        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        bitmap.recycle()
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}

private fun extractGeminiText(payload: JsonObject): String {
    val apiError = (payload["error"] as? JsonObject)
        ?.get("message")
        ?.jsonPrimitive
        ?.contentOrNull

    if (!apiError.isNullOrBlank()) {
        throw IllegalStateException("Gemini error: $apiError")
    }

    val candidates = payload["candidates"] as? JsonArray
    if (candidates.isNullOrEmpty()) {
        val blockReason = (payload["promptFeedback"] as? JsonObject)
            ?.get("blockReason")
            ?.jsonPrimitive
            ?.contentOrNull
        val reason = if (blockReason.isNullOrBlank()) {
            "Gemini returned no candidates"
        } else {
            "Gemini blocked the request: $blockReason"
        }
        throw IllegalStateException(reason)
    }

    val text = candidates
        .mapNotNull { it as? JsonObject }
        .mapNotNull { candidate ->
            val parts = (candidate["content"] as? JsonObject)?.get("parts") as? JsonArray
            parts
                ?.mapNotNull { part ->
                    (part as? JsonObject)
                        ?.get("text")
                        ?.jsonPrimitive
                        ?.contentOrNull
                }
                ?.firstOrNull { it.isNotBlank() }
        }
        .firstOrNull()
        ?.trim()

    if (text.isNullOrBlank()) {
        throw IllegalStateException("Gemini response did not include usable text")
    }

    return text
}

private fun String.extractAndParseJson(): String {
    val cleaned = this.trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    val start = cleaned.indexOf('{')
    val end = cleaned.lastIndexOf('}')
    if (start == -1 || end <= start) {
        throw IllegalStateException("No JSON object found in model response")
    }
    return cleaned.substring(start, end + 1)
}
