package com.example.scrapsetu.data.repo

import com.example.scrapsetu.data.remote.SupabaseClientProvider
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.util.UUID

class StorageRepository {

    private val client = SupabaseClientProvider.client
    private val bucket = "images"

    suspend fun uploadImage(imageBytes: ByteArray, mimeType: String, supplierId: String): String {
        val extension = when (mimeType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic" -> "heic"
            else -> "jpg"
        }
        val filePath = "$supplierId/${UUID.randomUUID()}.$extension"
        android.util.Log.d("StorageRepo", "Uploading: ${imageBytes.size} bytes, file: $filePath")
        try {
            val parsedContentType = runCatching { ContentType.parse(mimeType) }.getOrNull()
            client.storage[bucket].upload(filePath, imageBytes) {
                upsert = false
                this.contentType = parsedContentType
            }
            val url = client.storage[bucket].publicUrl(filePath)
            android.util.Log.d("StorageRepo", "Upload success: $url")
            return url
        } catch (e: Exception) {
            android.util.Log.e("StorageRepo", "Upload failed: ${e.message}")
            throw e
        }
    }
}