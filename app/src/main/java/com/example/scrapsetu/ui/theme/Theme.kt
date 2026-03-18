package com.example.scrapsetu.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ScrapSetuColorScheme = lightColorScheme(
    primary = PrimaryDarkGreen,
    secondary = AccentGreen,
    background = LightBackground,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onBackground = PrimaryDarkGreen,
    onSurface = PrimaryDarkGreen,
    tertiary = OrangeAccent
)

@Composable
fun ScrapSetuTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = PrimaryDarkGreen.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = ScrapSetuColorScheme,
        typography = Typography,
        content = content
    )
}