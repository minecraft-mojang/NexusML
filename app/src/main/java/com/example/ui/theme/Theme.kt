package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexusDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF9CF0FF),
    secondary = NeonViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3B1E82),
    onSecondaryContainer = Color(0xFFE9DDFF),
    tertiary = EmeraldSuccess,
    onTertiary = Color(0xFF00391A),
    background = CyberDarkBg,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun NexusMLTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NexusDarkColorScheme,
        typography = Typography,
        content = content
    )
}
