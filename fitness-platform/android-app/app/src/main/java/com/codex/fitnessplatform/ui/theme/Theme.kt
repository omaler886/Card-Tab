package com.codex.fitnessplatform.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Teal600,
    onPrimary = SurfaceWarm,
    secondary = Gold500,
    tertiary = Navy900,
    background = Sand50,
    surface = SurfaceWarm,
    onSurface = Ink900
)

private val DarkColors = darkColorScheme(
    primary = Gold500,
    secondary = Teal600,
    tertiary = Mint100,
    background = Navy900,
    surface = Ink900,
    onSurface = SurfaceWarm
)

@Composable
fun FitnessPlatformTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
