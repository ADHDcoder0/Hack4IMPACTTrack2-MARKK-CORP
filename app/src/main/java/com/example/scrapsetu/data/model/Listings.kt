package com.example.scrapsetu.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Listing(
    val id: String? = null,                          // null = let Supabase generate UUID
    @SerialName("supplier_id") val supplierId: String = "",
    @SerialName("waste_type") val wasteType: String = "",
    @SerialName("waste_category_id") val wasteCategoryId: String? = null,
    @SerialName("quantity_kg") val quantityKg: Double = 0.0,
    @SerialName("price_per_kg") val pricePerKg: Double = 0.0,
    @SerialName("state_code") val stateCode: String? = null,
    @SerialName("district_id") val districtId: String? = null,
    @SerialName("town_city") val townCity: String? = null,
    val location: String = "",
    val status: String = "active",
    val description: String = "",
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("created_at") val createdAt: String? = null  // null = let Supabase use now()
)