package com.example.scrapsetu.data.repo

import com.example.scrapsetu.data.model.SmartMatchInsight
import com.example.scrapsetu.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class SmartMatchInsightRepository {

    private val client = SupabaseClientProvider.client

    suspend fun getLatestInsightForListing(listingId: String): SmartMatchInsight? {
        if (listingId.isBlank()) return null

        return client.postgrest["smart_match_insights"]
            .select {
                filter { eq("listing_id", listingId) }
                order("generated_at", Order.DESCENDING)
                limit(1)
            }
            .decodeList<SmartMatchInsight>()
            .firstOrNull()
    }
}
