package com.example.scrapsetu.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnalyticsResponse(
    @SerialName("smart_match") val smartMatch: SmartMatchResult = SmartMatchResult(),
    @SerialName("price_suggestion") val priceSuggestion: PriceSuggestion = PriceSuggestion(),
    @SerialName("demand_forecast") val demandForecast: DemandForecast = DemandForecast(),
    @SerialName("buyer_suggestions") val buyerSuggestions: BuyerSuggestions = BuyerSuggestions(),
    @SerialName("seller_analytics") val sellerAnalytics: SellerAnalytics = SellerAnalytics(),
    @SerialName("trust_score") val trustScore: TrustScore = TrustScore()
)

@Serializable
data class SmartMatchResult(
    @SerialName("reliability_score") val reliabilityScore: Int = 60,
    @SerialName("estimated_eta") val estimatedEta: String = "3-5 days",
    val reason: String = "Generated from available marketplace context.",
    val confidence: String = "medium"
)

@Serializable
data class PriceSuggestion(
    @SerialName("min_price_inr") val minPriceInr: Int = 70,
    @SerialName("max_price_inr") val maxPriceInr: Int = 90,
    val unit: String = "per kg",
    val basis: String = "Derived from recent listing behavior."
)

@Serializable
data class DemandForecast(
    val trend: String = "stable",
    @SerialName("confidence_pct") val confidencePct: Int = 60,
    val insight: String = "Demand signal inferred from recent activity."
)

@Serializable
data class BuyerSuggestions(
    @SerialName("top_matches") val topMatches: List<BuyerMatch> = emptyList(),
    val summary: String = "Insufficient buyer history for strong ranking yet."
)

@Serializable
data class BuyerMatch(
    @SerialName("buyer_id") val buyerId: String = "",
    @SerialName("match_score") val matchScore: Int = 50,
    val reason: String = "Historical interaction signal."
)

@Serializable
data class SellerAnalytics(
    @SerialName("performance_summary") val performanceSummary: String = "Performance summary unavailable.",
    @SerialName("suggested_category") val suggestedCategory: String = "Mixed Waste",
    @SerialName("improvement_tip") val improvementTip: String = "Add better photos and detailed descriptions.",
    @SerialName("listing_quality_score") val listingQualityScore: Int = 60
)

@Serializable
data class TrustScore(
    val score: Int = 60,
    val tier: String = "Silver",
    val factors: List<String> = emptyList(),
    @SerialName("next_action") val nextAction: String = "Maintain consistent fulfilment to improve trust."
)
