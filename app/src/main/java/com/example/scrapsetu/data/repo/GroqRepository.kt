package com.example.scrapsetu.data.repo



import com.example.scrapsetu.BuildConfig
import com.example.scrapsetu.data.model.Listing
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

class GroqRepository {

    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun getSmartMatchSuggestion(listing: Listing): String {
        val prompt = """
            You are a smart waste marketplace matching assistant.
            
            A supplier has listed the following waste:
            - Waste Type: ${listing.wasteType}
            - Quantity: ${listing.quantityKg} kg
            - Price per kg: ₹${listing.pricePerKg}
            - Location: ${listing.location}
            - Description: ${listing.description}
            
            Suggest the most suitable type of buyer/recycler for this waste,
            explain why they are a good match, and what they would typically
            do with this material. Keep response under 100 words.
        """.trimIndent()

        val response = client.post("https://api.groq.com/openai/v1/chat/completions") {
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

        val json = Json { ignoreUnknownKeys = true }
        val body = json.parseToJsonElement(response.bodyAsText())
        return body
            .jsonObject["choices"]
            ?.jsonArray?.get(0)
            ?.jsonObject?.get("message")
            ?.jsonObject?.get("content")
            ?.jsonPrimitive?.content
            ?: "No suggestion available"
    }
}