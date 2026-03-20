package com.example.scrapsetu.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    val role: String = "",       // "supplier" or "buyer"
    val name: String = "",
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