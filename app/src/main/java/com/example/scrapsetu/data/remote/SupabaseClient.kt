package com.example.scrapsetu.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth

object SupabaseClientProvider {
    val client = createSupabaseClient(
        supabaseUrl = "https://zriijjrfdkefytkugsow.supabase.co",
        supabaseKey = "sb_publishable_ic_mt5ceerEx_mmwzJ1qSA_Au8Myd3n"
    ) {
        install(Postgrest)
        install(Auth)
    }
}