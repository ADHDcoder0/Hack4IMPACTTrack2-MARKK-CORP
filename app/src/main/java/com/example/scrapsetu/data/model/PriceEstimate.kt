package com.example.scrapsetu.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PriceEstimate(
    val min: Int,
    val max: Int,
    val unit: String,
    val confidence: String
)
