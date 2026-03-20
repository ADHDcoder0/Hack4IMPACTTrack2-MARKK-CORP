package com.example.scrapsetu.data.repo

import com.example.scrapsetu.data.model.Match
import com.example.scrapsetu.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class MatchRepository {

    private val client = SupabaseClientProvider.client

    suspend fun createMatch(listingId: String, buyerId: String) {
        client.postgrest["matches"].insert(
            Match(listingId = listingId, buyerId = buyerId)
        )
    }

    suspend fun getBuyerMatches(buyerId: String): List<Match> {
        return client.postgrest["matches"]
            .select {
                filter { eq("buyer_id", buyerId) }
                order("created_at", Order.DESCENDING)
            }
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
        if (supplierListingIds.isEmpty()) return emptyList()

        val listingIds = supplierListingIds.distinct().toSet()
        return client.postgrest["matches"]
            .select { order("created_at", Order.DESCENDING) }
            .decodeList<Match>()
            .filter {
                it.listingId in listingIds &&
                    !it.status.equals("rejected", ignoreCase = true)
            }
    }

    suspend fun deleteBuyerMatch(matchId: String, buyerId: String) {
        client.postgrest["matches"]
            .delete {
                filter {
                    eq("id", matchId)
                    eq("buyer_id", buyerId)
                    eq("status", "pending")
                }
            }
    }
}