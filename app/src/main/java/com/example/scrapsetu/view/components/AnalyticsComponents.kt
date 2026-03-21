package com.example.scrapsetu.view.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.scrapsetu.data.model.AnalyticsResponse
import com.example.scrapsetu.data.model.BuyerMatch
import com.example.scrapsetu.data.model.BuyerSuggestions
import com.example.scrapsetu.data.model.DemandForecast
import com.example.scrapsetu.data.model.PriceSuggestion
import com.example.scrapsetu.data.model.SellerAnalytics
import com.example.scrapsetu.data.model.SmartMatchResult
import com.example.scrapsetu.data.model.TrustScore
import com.example.scrapsetu.vm.AnalyticsUiState
import com.example.scrapsetu.data.repo.GroqAnalyticsRepository.AnalyticsSource
import com.example.scrapsetu.ui.theme.EcoDeepForest
import com.example.scrapsetu.ui.theme.EcoInteractionWhite
import com.example.scrapsetu.ui.theme.EcoSageGrowth
import com.example.scrapsetu.ui.theme.EcoSectionMint

@Composable
fun AnalyticsShell(
    state: AnalyticsUiState,
    content: @Composable (AnalyticsResponse) -> Unit
) {
    when (state) {
        AnalyticsUiState.Idle -> Unit
        AnalyticsUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Analysing with AI...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is AnalyticsUiState.Error -> {
            Text(
                text = "AI unavailable: ${state.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp)
            )
        }

        is AnalyticsUiState.Success -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AnalyticsSourceBadge(source = state.source)
                content(state.data)
            }
        }
    }
}

@Composable
private fun AnalyticsSourceBadge(source: AnalyticsSource) {
    val text = when (source) {
        AnalyticsSource.GROQ -> "AI Source: Groq"
        AnalyticsSource.GEMINI -> "AI Source: Gemini"
        AnalyticsSource.FALLBACK -> "AI Source: Fallback"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = EcoDeepForest,
        modifier = Modifier.padding(start = 2.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun SmartMatchCard(result: SmartMatchResult) {
    val confidenceColor = when (result.confidence.lowercase()) {
        "high" -> Color(0xFF2E7D32)
        "medium" -> Color(0xFFF57C00)
        else -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Smart Match", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = confidenceColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = result.confidence.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        color = confidenceColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            ScoreBar(
                label = "Reliability",
                score = result.reliabilityScore,
                color = MaterialTheme.colorScheme.primary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ETA:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(result.estimatedEta, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }

            Text(
                text = result.reason,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun PriceSuggestionChip(suggestion: PriceSuggestion) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI Price Estimate",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "INR ${suggestion.minPriceInr}-${suggestion.maxPriceInr} ${suggestion.unit}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Text(
                text = suggestion.basis,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.widthIn(max = 160.dp),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun DemandForecastChip(forecast: DemandForecast) {
    val trendArrow = when (forecast.trend.lowercase()) {
        "rising" -> "^"
        "falling" -> "v"
        else -> "-"
    }
    val trendColor = when (forecast.trend.lowercase()) {
        "rising" -> Color(0xFF2E7D32)
        "falling" -> Color(0xFFC62828)
        else -> Color(0xFFF57C00)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = trendColor.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(trendArrow, fontSize = 12.sp, color = trendColor, fontWeight = FontWeight.Bold)
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = forecast.trend.replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = trendColor
                    )
                    Text(
                        text = "${forecast.confidencePct}% confidence",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = forecast.insight,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
            CircularPercentGraph(
                percent = forecast.confidencePct,
                color = trendColor,
                modifier = Modifier.size(54.dp)
            )
        }
    }
}

@Composable
fun BuyerSuggestionsPanel(suggestions: BuyerSuggestions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Suggested Buyers", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(
            text = suggestions.summary,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        suggestions.topMatches.forEach { match ->
            BuyerMatchRow(match)
        }

        if (suggestions.topMatches.isEmpty()) {
            Text(
                text = "No match history yet. Share this listing to attract buyers.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BuyerMatchRow(match: BuyerMatch) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${match.matchScore}",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Buyer ...${match.buyerId.takeLast(6)}",
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
            )
            Text(
                text = match.reason,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun SellerAnalyticsCard(analytics: SellerAnalytics) {
    val isDark = isSystemInDarkTheme()
    val cardContainer = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val titleColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.primary
    }
    val metricSurfaceColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    }
    val metricValueColor = if (isDark) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.primary
    }
    val tipSurfaceColor = if (isDark) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    } else {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
    }

    val performanceSummary = analytics.performanceSummary
        .takeIf { it.isNotBlank() && !it.contains("empty", ignoreCase = true) }
        ?: "Keep listings active and complete to improve your match quality."
    val improvementTip = analytics.improvementTip
        .takeIf { it.isNotBlank() }
        ?: "Add clearer photos and exact quantity for better buyer trust."
    val suggestedCategory = analytics.suggestedCategory
        .takeIf { it.isNotBlank() }
        ?: "Plastic"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Your Performance", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = titleColor)

            ScoreBar(
                label = "Listing quality",
                score = analytics.listingQualityScore,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = metricSurfaceColor,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = "Active Listings",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = analytics.activeListings.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = metricValueColor
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = metricSurfaceColor,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = "Conversion",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${analytics.conversionRatePct}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = metricValueColor
                        )
                    }
                }
            }

            Text(
                text = performanceSummary,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = tipSurfaceColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TIP",
                        fontSize = 11.sp,
                        color = titleColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = improvementTip,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Suggested next:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Text(
                        text = suggestedCategory,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun TrustScoreCard(trust: TrustScore) {
    val tierColor = when (trust.tier) {
        "Verified" -> Color(0xFF1565C0)
        "Gold" -> Color(0xFFF9A825)
        "Silver" -> Color(0xFF546E7A)
        else -> Color(0xFF6D4C41)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = tierColor.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Trust Score", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(
                        text = trust.tier,
                        fontSize = 12.sp,
                        color = tierColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(50))
                        .background(tierColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${trust.score}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = tierColor
                    )
                }
            }

            ScoreBar(label = "Score", score = trust.score, color = tierColor)

            trust.factors.forEach { factor ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("+", fontSize = 12.sp, color = tierColor)
                    Text(
                        text = factor,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Next: ${trust.nextAction}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun CircularPercentGraph(
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val clampedPercent = percent.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedPercent / 100f,
        animationSpec = tween(durationMillis = 900),
        label = "trust_graph_progress"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = color.copy(alpha = 0.18f),
            strokeWidth = 7.dp
        )
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = 7.dp
        )
        Text(
            text = "$clampedPercent%",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = color
        )
    }
}

@Composable
fun ScoreBar(label: String, score: Int, color: Color) {
    val clampedScore = score.coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedScore / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "score_bar_$label"
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$clampedScore / 100", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = color)
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}
