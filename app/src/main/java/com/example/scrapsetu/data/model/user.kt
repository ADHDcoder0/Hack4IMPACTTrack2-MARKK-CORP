package com.example.scrapsetu.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    val role: String = "",       // "supplier" or "buyer"
    val name: String = "",
    val location: String = "",
    @SerialName("created_at")
    val createdAt: String = ""
)