package com.example.scrapsetu.data.repo

import com.example.scrapsetu.data.model.IndiaDistrict
import com.example.scrapsetu.data.model.IndiaState
import com.example.scrapsetu.data.model.WasteCategory
import com.example.scrapsetu.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class MasterDataRepository {

    private val client = SupabaseClientProvider.client

    suspend fun getWasteCategories(): List<WasteCategory> {
        return client.postgrest["waste_categories"]
            .select {
                filter { eq("active", true) }
                order("sort_order", Order.ASCENDING)
            }
            .decodeList<WasteCategory>()
    }

    suspend fun getIndiaStates(): List<IndiaState> {
        return client.postgrest["india_states"]
            .select {
                filter { eq("active", true) }
                order("state_name", Order.ASCENDING)
            }
            .decodeList<IndiaState>()
    }

    suspend fun getDistrictsByState(stateCode: String): List<IndiaDistrict> {
        if (stateCode.isBlank()) return emptyList()

        return client.postgrest["india_districts"]
            .select {
                filter {
                    eq("active", true)
                    eq("state_code", stateCode)
                }
                order("district_name", Order.ASCENDING)
            }
            .decodeList<IndiaDistrict>()
    }
}
