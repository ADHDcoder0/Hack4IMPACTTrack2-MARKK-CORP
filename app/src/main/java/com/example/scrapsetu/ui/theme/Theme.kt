package com.example.scrapsetu.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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

private val ScrapSetuDarkColorScheme = darkColorScheme(
    primary = EcoDeepForestDark,
    secondary = EcoSageGrowthDark,
    background = EcoNightBackground,
    surface = EcoNightSurface,
    onPrimary = EcoNightBackground,
    onSecondary = EcoNightBackground,
    onBackground = EcoNightOnSurface,
    onSurface = EcoNightOnSurface,
    primaryContainer = EcoNightSection,
    onPrimaryContainer = EcoNightOnSurface,
    secondaryContainer = EcoNightSection,
    onSecondaryContainer = EcoNightOnSurface,
    tertiary = EcoSageGrowthDark,
    onTertiary = EcoNightBackground,
    tertiaryContainer = EcoNightSection,
    onTertiaryContainer = EcoNightOnSurface,
    errorContainer = EcoNightErrorContainer,
    onErrorContainer = EcoNightOnErrorContainer,
    surfaceVariant = EcoNightSurfaceVariant,
    onSurfaceVariant = EcoNightOnSurfaceVariant,
    outline = EcoNightOutline,
    outlineVariant = EcoNightOutline.copy(alpha = 0.6f),
    inversePrimary = EcoDeepForest,
    inverseSurface = EcoInteractionWhite,
    inverseOnSurface = EcoOnSurface
)

@Composable
fun ScrapSetuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ScrapSetuDarkColorScheme else ScrapSetuColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}