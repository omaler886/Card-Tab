package com.codex.sleepmonitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = DeepTeal,
    secondary = Lagoon,
    surface = Foam,
    background = Foam,
    onPrimary = Foam,
    onSurface = Ink
)

private val DarkColors = darkColorScheme(
    primary = Lagoon,
    secondary = Color(0xFF87F1E9),
    surface = TwilightNavy,
    background = TwilightNavy,
    onPrimary = TwilightNavy,
    onSurface = Foam
)

@Composable
fun SleepMonitorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = SleepMonitorTypography,
        content = content
    )
}
