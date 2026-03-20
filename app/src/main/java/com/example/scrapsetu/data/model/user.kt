package com.example.scrapsetu.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    val role: String = "",       // "supplier" or "buyer"
    val name: String = "",
    @SerialName("business_name")
    val businessName: String = "",
    val phone: String = "",
    @SerialName("state")
    val state: String = "",
    @SerialName("business_type")
    val businessType: String = "",
    @SerialName("waste_categories")
    val wasteCategories: List<String> = emptyList(),
    @SerialName("monthly_volume")
    val monthlyVolume: Int = 0,
    @SerialName("state_code")
    val stateCode: String? = null,
    @SerialName("district_id")
    val districtId: String? = null,
    @SerialName("town_city")
    val townCity: String? = null,
    val location: String = "",
    @SerialName("created_at")
    val createdAt: String = ""
)