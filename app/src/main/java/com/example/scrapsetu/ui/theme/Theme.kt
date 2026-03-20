package com.example.scrapsetu.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ScrapSetuColorScheme = lightColorScheme(
    primary = EcoDeepForest,
    secondary = EcoSageGrowth,
    background = EcoMintVapor,
    surface = EcoInteractionWhite,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = EcoOnSurface,
    onSurface = EcoOnSurface,
    primaryContainer = EcoSectionMint,
    onPrimaryContainer = EcoDeepForest,
    secondaryContainer = EcoSectionMint,
    onSecondaryContainer = EcoDeepForest,
    tertiary = EcoSageGrowth,
    onTertiary = Color.White,
    tertiaryContainer = EcoSectionMint,
    onTertiaryContainer = EcoDeepForest,
    errorContainer = RejectedBackground,
    onErrorContainer = RejectedText,
    surfaceVariant = EcoSurfaceVariant,
    onSurfaceVariant = EcoOnSurfaceVariant,
    outline = EcoSageGrowth.copy(alpha = 0.38f),
    outlineVariant = EcoSectionMint,
    inversePrimary = EcoSageGrowth,
    inverseSurface = EcoDeepForest,
    inverseOnSurface = EcoInteractionWhite
)

@Composable
fun ScrapSetuTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = EcoDeepForest.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = ScrapSetuColorScheme,
        typography = Typography,
        content = content
    )
}