package com.example.scrapsetu.data.repo

import com.example.scrapsetu.data.model.Listing
import com.example.scrapsetu.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class ListingRepository {

    private val client = SupabaseClientProvider.client

    suspend fun createListing(listing: Listing) {
        client.postgrest["listings"].insert(listing)
    }

    suspend fun getActiveListings(): List<Listing> {
        return client.postgrest["listings"]
            .select {
                filter { eq("status", "active") }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<Listing>()
    }

    suspend fun getSupplierListings(supplierId: String): List<Listing> {
        return client.postgrest["listings"]
            .select { filter { eq("supplier_id", supplierId) } }
            .decodeList<Listing>()
    }

    suspend fun updateListingStatus(listingId: String, status: String) {
        client.postgrest["listings"]
            .update({ set("status", status) })
            { filter { eq("id", listingId) } }
    }
}