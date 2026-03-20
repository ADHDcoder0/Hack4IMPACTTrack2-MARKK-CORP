package com.example.scrapsetu.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageDetectionResult(
    @SerialName("waste_category_slug") val wasteCategorySlug: String,
    @SerialName("waste_category_label") val wasteCategoryLabel: String,
    val quality: String,
    val confidence: Int,
    val description: String,
    @SerialName("quantity_hint") val quantityHint: String? = null
)

data class DetectionWithPricing(
    val detection: ImageDetectionResult,
    val pricing: PriceSuggestion
)

sealed class DetectionUiState {
    object Idle : DetectionUiState()
    object Analysing : DetectionUiState()
    data class Success(val result: DetectionWithPricing) : DetectionUiState()
    data class Error(val message: String) : DetectionUiState()
}
