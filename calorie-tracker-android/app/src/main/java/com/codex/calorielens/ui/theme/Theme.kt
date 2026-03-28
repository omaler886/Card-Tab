package com.codex.calorielens.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFCB5C35),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF4F7B53),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFD7972B),
    background = Color(0xFFF9F3EC),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF251A17),
    surfaceVariant = Color(0xFFF2E3D4),
    onSurfaceVariant = Color(0xFF5B4A41)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFA47A),
    secondary = Color(0xFF99C68E),
    tertiary = Color(0xFFF7CB73),
    background = Color(0xFF181210),
    surface = Color(0xFF211917),
    onSurface = Color(0xFFF9EDE7),
    surfaceVariant = Color(0xFF493A33),
    onSurfaceVariant = Color(0xFFD9C3B7)
)

@Composable
fun CalorieLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}
