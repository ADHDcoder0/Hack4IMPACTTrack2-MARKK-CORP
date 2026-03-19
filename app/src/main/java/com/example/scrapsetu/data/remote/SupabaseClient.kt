package com.example.scrapsetu.data.remote

import com.example.scrapsetu.BuildConfig.SUPABASE_KEY
import com.example.scrapsetu.BuildConfig.SUPABASE_URL
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseClientProvider {
    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY,
    ) {
        install(Postgrest) {
            serializer = KotlinXSerializer(
                Json {
                    coerceInputValues = true
                    ignoreUnknownKeys = true
                }
            )
        }
        install(Auth)
        install(Storage)
    }
}