package com.example.scrapsetu.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BuyerRequest(
    val id: String? = null,
    @SerialName("buyer_id") val buyerId: String = "",
    @SerialName("waste_type") val wasteType: String = "",
    @SerialName("waste_category_id") val wasteCategoryId: String? = null,
    @SerialName("quantity_kg") val quantityKg: Double = 0.0,
    @SerialName("max_price_per_kg") val maxPricePerKg: Double = 0.0,
    @SerialName("state_code") val stateCode: String? = null,
    @SerialName("district_id") val districtId: String? = null,
    @SerialName("town_city") val townCity: String? = null,
    val location: String? = null,
    val description: String? = null,
    val status: String = "open",
    @SerialName("created_at") val createdAt: String? = null
)
