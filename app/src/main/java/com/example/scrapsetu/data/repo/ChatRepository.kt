package com.example.scrapsetu.data.repo

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.scrapsetu.data.model.MESSAGE_LIMIT
import com.example.scrapsetu.data.model.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationContext private val context: Context
) {

    private val retentionHours = 24

    suspend fun loadMessages(matchId: String): List<Message> =
        supabase.from("messages")
            .select {
                filter {
                    eq("match_id", matchId)
                    gte("created_at", utcNowMinusHoursIso(retentionHours))
                }
                order("created_at", Order.ASCENDING)
                limit(MESSAGE_LIMIT.toLong())
            }
            .decodeList<Message>()

    suspend fun getMessageCount(matchId: String): Int =
        supabase.from("messages")
            .select {
                filter {
                    eq("match_id", matchId)
                    gte("created_at", utcNowMinusHoursIso(retentionHours))
                }
            }
            .decodeList<Message>()
            .size

    suspend fun purgeExpiredMessages(matchId: String) {
        runCatching {
            supabase.from("messages").delete {
                filter {
                    eq("match_id", matchId)
                    lt("created_at", utcNowMinusHoursIso(retentionHours))
                }
            }
        }
    }

    suspend fun sendText(
        matchId: String,
        senderId: String,
        content: String
    ): Result<Message> = runCatching {
        supabase.from("messages").insert(
            buildJsonObject {
                put("match_id", matchId)
                put("sender_id", senderId)
                put("content", content)
            }
        ) { select() }.decodeSingle<Message>()
    }

    suspend fun sendImage(
        matchId: String,
        senderId: String,
        imageUri: Uri
    ): Result<Message> = runCatching {
        val bytes = context.contentResolver
            .openInputStream(imageUri)
            ?.use { it.readBytes() }
            ?: throw IllegalStateException("Could not read image")

        val extension = imageExtension(imageUri)
        val path = "chat-images/$matchId/${UUID.randomUUID()}.$extension"

        supabase.storage.from("images").upload(path, bytes) { upsert = false }
        val imageUrl = supabase.storage.from("images").publicUrl(path)

        supabase.from("messages").insert(
            buildJsonObject {
                put("match_id", matchId)
                put("sender_id", senderId)
                put("image_url", imageUrl)
            }
        ) { select() }.decodeSingle<Message>()
    }

    suspend fun markAllRead(matchId: String, currentUserId: String) {
        runCatching {
            supabase.from("messages").update(
                buildJsonObject { put("read_at", utcNowIso()) }
            ) {
                filter {
                    eq("match_id", matchId)
                    neq("sender_id", currentUserId)
                }
            }
        }
    }

    fun subscribeToMessages(matchId: String): Flow<Message> {
        val channel = supabase.channel("chat:$matchId")
        return channel
            .postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "messages"
            }
            .mapNotNull {
                val message = it.decodeRecord<Message>()
                message.takeIf { msg -> msg.matchId == matchId }
            }
    }

    suspend fun unreadCount(matchId: String, currentUserId: String): Int =
        supabase.from("messages")
            .select {
                filter {
                    eq("match_id", matchId)
                    neq("sender_id", currentUserId)
                    gte("created_at", utcNowMinusHoursIso(retentionHours))
                }
            }
            .decodeList<Message>()
            .count { it.readAt == null }

    private fun imageExtension(uri: Uri): String {
        val mime = context.contentResolver.getType(uri).orEmpty().lowercase(Locale.ROOT)
        return when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            else -> "jpg"
        }
    }

    private fun utcNowIso(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }

    private fun utcNowMinusHoursIso(hours: Int): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        val cutoffMillis = Date().time - (hours * 60L * 60L * 1000L)
        return fmt.format(Date(cutoffMillis))
    }
}
