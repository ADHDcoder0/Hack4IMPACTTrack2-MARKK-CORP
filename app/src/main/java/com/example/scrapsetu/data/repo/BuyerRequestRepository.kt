package com.example.scrapsetu.data.repo

import com.example.scrapsetu.data.model.BuyerRequest
import com.example.scrapsetu.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class BuyerRequestRepository {

    private val client = SupabaseClientProvider.client

    suspend fun createRequest(request: BuyerRequest) {
        client.postgrest["buyer_requests"].insert(request)
    }

    suspend fun getMyRequests(buyerId: String): List<BuyerRequest> {
        return client.postgrest["buyer_requests"]
            .select {
                filter { eq("buyer_id", buyerId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<BuyerRequest>()
    }

    suspend fun getOpenRequests(): List<BuyerRequest> {
        return client.postgrest["buyer_requests"]
            .select {
                filter { eq("status", "open") }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<BuyerRequest>()
    }

    suspend fun closeRequest(requestId: String) {
        client.postgrest["buyer_requests"]
            .update({ set("status", "closed") }) {
                filter { eq("id", requestId) }
            }
    }
}
