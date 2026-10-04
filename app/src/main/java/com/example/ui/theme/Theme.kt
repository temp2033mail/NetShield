package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF041B19),
    primaryContainer = Color(0xFF003830),
    onPrimaryContainer = CyberCyan,
    secondary = CyberElectric,
    onSecondary = Color(0xFF00202E),
    secondaryContainer = Color(0xFF003D5B),
    onSecondaryContainer = Color(0xFFBEEBFF),
    tertiary = ShieldGreen,
    onTertiary = Color(0xFF00220F),
    error = ThreatRed,
    onError = Color.White,
    background = CyberBackgroundDark,
    onBackground = TextPrimary,
    surface = CyberSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = CyberPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCEF5FF),
    onPrimaryContainer = Color(0xFF001F29),
    secondary = CyberSecondaryLight,
    onSecondary = Color.White,
    error = ThreatRed,
    onError = Color.White,
    background = CyberBackgroundLight,
    onBackground = Color(0xFF0F172A),
    surface = CyberSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun NetShieldTheme(
    darkTheme: Boolean = true, // Default to cyber dark theme for security monitor
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
