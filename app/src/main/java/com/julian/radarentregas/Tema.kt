package com.julian.radarentregas

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Claro = lightColorScheme(
    primary = Color(0xFF2C47D6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE3FF),
    onPrimaryContainer = Color(0xFF0B1A6B),
    background = Color(0xFFF2F4F8),
    onBackground = Color(0xFF141C2B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141C2B),
    surfaceVariant = Color(0xFFE6EAF2),
    onSurfaceVariant = Color(0xFF596579),
    error = Color(0xFFD12C22),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF8096FF),
    onPrimary = Color(0xFF0E131B),
    primaryContainer = Color(0xFF26368F),
    onPrimaryContainer = Color(0xFFDDE3FF),
    background = Color(0xFF0E131B),
    onBackground = Color(0xFFE5EAF2),
    surface = Color(0xFF161D28),
    onSurface = Color(0xFFE5EAF2),
    surfaceVariant = Color(0xFF222B39),
    onSurfaceVariant = Color(0xFF97A3B6),
    error = Color(0xFFFF7A6E),
)

@Composable
fun RadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro,
        content = content,
    )
}
