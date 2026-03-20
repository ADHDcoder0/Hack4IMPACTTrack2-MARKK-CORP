package com.example.scrapsetu.view.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.scrapsetu.data.model.DetectionUiState
import com.example.scrapsetu.data.model.DetectionWithPricing
import com.example.scrapsetu.vm.ImageDetectionViewModel

@Composable
fun ImageDetectionPicker(
    detectionVm: ImageDetectionViewModel,
    onDetected: (
        slug: String,
        label: String,
        description: String,
        minPrice: Int,
        maxPrice: Int,
        unit: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by detectionVm.detectionState.collectAsState()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            detectionVm.onImageSelected(uri)
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .border(
                    width = if (selectedUri == null) 2.dp else 0.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedUri != null) {
                AsyncImage(
                    model = selectedUri,
                    contentDescription = "Selected scrap image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (state is DetectionUiState.Analysing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp
                            )
                            Text("Identifying material...", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(24.dp)
                        )
                    }
                    Text("Scan photo with AI", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Tap to detect material type and suggested price",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Auto-fill waste details", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state is DetectionUiState.Success,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut()
        ) {
            if (state is DetectionUiState.Success) {
                val result = (state as DetectionUiState.Success).result
                DetectionResultCard(
                    result = result,
                    onAccept = {
                        onDetected(
                            result.detection.wasteCategorySlug,
                            result.detection.wasteCategoryLabel,
                            result.detection.description,
                            result.pricing.minPriceInr,
                            result.pricing.maxPriceInr,
                            result.pricing.unit
                        )
                    },
                    onRetry = { launcher.launch("image/*") }
                )
            }
        }

        AnimatedVisibility(visible = state is DetectionUiState.Error) {
            if (state is DetectionUiState.Error) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conciseDetectionError((state as DetectionUiState.Error).message),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { launcher.launch("image/*") }) {
                            Text("Retry", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetectionResultCard(
    result: DetectionWithPricing,
    onAccept: () -> Unit,
    onRetry: () -> Unit
) {
    val confColor = when {
        result.detection.confidence >= 75 -> Color(0xFF2E7D32)
        result.detection.confidence >= 50 -> Color(0xFFF57C00)
        else -> Color(0xFFC62828)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("AI detected", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DetectionPill("${result.detection.confidence}% sure", confColor)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(result.detection.wasteCategoryLabel, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        result.detection.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
                DetectionQualityBadge(result.detection.quality)
            }

            result.detection.quantityHint?.let {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Est. quantity:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(it, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Suggested price", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "INR ${result.pricing.minPriceInr}-${result.pricing.maxPriceInr} ${result.pricing.unit}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    result.pricing.basis,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(max = 150.dp),
                    lineHeight = 14.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Change image", fontSize = 13.sp)
                }
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Use this", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun DetectionPill(
    text: String,
    color: Color,
    containerColor: Color = color.copy(alpha = 0.12f)
) {
    Surface(shape = RoundedCornerShape(20.dp), color = containerColor) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            fontSize = 11.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetectionQualityBadge(quality: String) {
    val (label, color) = when (quality.lowercase()) {
        "high" -> "Good condition" to Color(0xFF2E7D32)
        "medium" -> "Fair condition" to Color(0xFFF57C00)
        else -> "Poor condition" to Color(0xFFC62828)
    }
    DetectionPill(label, color)
}

private fun conciseDetectionError(rawMessage: String): String {
    val msg = rawMessage.trim()
    val lower = msg.lowercase()
    return when {
        lower.contains("not found for api version") || lower.contains("not supported for generatecontent") ->
            "Gemini model is unavailable for this API key. Please retry with updated model settings."
        lower.contains("api key") || lower.contains("permission") ->
            "Gemini API key is invalid or missing required access."
        lower.contains("quota") || lower.contains("rate") || lower.contains("429") ->
            "Gemini quota limit reached. Please try again shortly."
        msg.length > 180 -> msg.take(180) + "..."
        else -> msg
    }
}
