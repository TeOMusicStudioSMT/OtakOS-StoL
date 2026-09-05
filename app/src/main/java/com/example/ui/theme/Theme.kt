package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = OtakCyanLight,
    onPrimary = Color(0xFF003544),
    primaryContainer = Color(0xFF004D63),
    onPrimaryContainer = Color(0xFFBAEAFF),
    secondary = OtakGoldLight,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF634000),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = OtakPurpleLight,
    onTertiary = Color(0xFF381E72),
    background = DarkMarbleBackground,
    surface = DarkMarbleSurface,
    surfaceVariant = DarkMarbleCard,
    onBackground = DarkMarbleText,
    onSurface = DarkMarbleText,
    onSurfaceVariant = DarkMarbleSubtext,
    outline = DarkMarbleBorder
)

private val LightColorScheme = lightColorScheme(
    primary = OtakCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF001F29),
    secondary = OtakGold,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF2A1800),
    tertiary = OtakPurple,
    onTertiary = Color.White,
    background = PearlWhite,
    surface = PearlIvory,
    surfaceVariant = PearlSurfaceLight,
    onBackground = PearlTextDark,
    onSurface = PearlTextDark,
    onSurfaceVariant = PearlTextSecondary,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
