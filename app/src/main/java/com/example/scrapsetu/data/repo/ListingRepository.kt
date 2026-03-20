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

    suspend fun updateListingDetails(
        listingId: String,
        wasteType: String,
        wasteCategoryId: String?,
        quantityKg: Double,
        pricePerKg: Double,
        stateCode: String?,
        districtId: String?,
        townCity: String?,
        location: String,
        description: String,
        imageUrl: String
    ) {
        client.postgrest["listings"]
            .update(
                {
                    set("waste_type", wasteType)
                    set("waste_category_id", wasteCategoryId)
                    set("quantity_kg", quantityKg)
                    set("price_per_kg", pricePerKg)
                    set("state_code", stateCode)
                    set("district_id", districtId)
                    set("town_city", townCity)
                    set("location", location)
                    set("description", description)
                    set("image_url", imageUrl)
                }
            ) {
                filter { eq("id", listingId) }
            }
    }
}