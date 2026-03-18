package com.example.scrapsetu.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Match(
    val id: String = "",
    @SerialName("listing_id")
    val listingId: String = "",
    @SerialName("buyer_id")
    val buyerId: String = "",
    val status: String = "pending",  // pending / confirmed / rejected
    @SerialName("created_at")
    val createdAt: String = ""
)