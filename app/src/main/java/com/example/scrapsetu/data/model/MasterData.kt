package com.example.scrapsetu.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WasteCategory(
    val id: String = "",
    val slug: String = "",
    val label: String = "",
    val active: Boolean = true,
    @SerialName("sort_order")
    val sortOrder: Int = 0
)

@Serializable
data class IndiaState(
    @SerialName("state_code")
    val stateCode: String = "",
    @SerialName("state_name")
    val stateName: String = "",
    val active: Boolean = true
)

@Serializable
data class IndiaDistrict(
    val id: String = "",
    @SerialName("state_code")
    val stateCode: String = "",
    @SerialName("district_name")
    val districtName: String = "",
    @SerialName("district_code")
    val districtCode: String? = null,
    val active: Boolean = true
)

@Serializable
data class SmartMatchInsight(
    val id: String = "",
    @SerialName("listing_id")
    val listingId: String = "",
    @SerialName("buyer_id")
    val buyerId: String? = null,
    @SerialName("reliability_score")
    val reliabilityScore: Double = 0.0,
    @SerialName("eta_days")
    val etaDays: Int = 0,
    @SerialName("confidence_note")
    val confidenceNote: String? = null,
    @SerialName("model_version")
    val modelVersion: String? = null,
    val source: String = "",
    @SerialName("generated_at")
    val generatedAt: String = ""
)
