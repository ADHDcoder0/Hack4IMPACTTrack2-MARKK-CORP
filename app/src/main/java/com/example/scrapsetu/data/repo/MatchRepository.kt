package com.example.scrapsetu.data.repo

import com.example.scrapsetu.data.model.Match
import com.example.scrapsetu.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest

class MatchRepository {

    private val client = SupabaseClientProvider.client

    suspend fun createMatch(listingId: String, buyerId: String) {
        client.postgrest["matches"].insert(
            Match(listingId = listingId, buyerId = buyerId)
        )
    }

    suspend fun getBuyerMatches(buyerId: String): List<Match> {
        return client.postgrest["matches"]
            .select { filter { eq("buyer_id", buyerId) } }
            .decodeList<Match>()
    }

    suspend fun updateMatchStatus(matchId: String, status: String) {
        client.postgrest["matches"]
            .update({ set("status", status) })
            { filter { eq("id", matchId) } }
    }

    suspend fun getListingMatches(listingId: String): List<Match> {
        return client.postgrest["matches"]
            .select { filter { eq("listing_id", listingId) } }
            .decodeList<Match>()
    }

    suspend fun getMatchesForSupplier(supplierListingIds: List<String>): List<Match> {
        return client.postgrest["matches"]
            .select()
            .decodeList<Match>()
            .filter { it.listingId in supplierListingIds }
    }
}