package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = EditorialDarkBg,
    secondary = Color(0xFF9E8DBA),
    onSecondary = Color.White,
    tertiary = LowSeverityBlue,
    background = EditorialDarkBg,
    onBackground = TextPrimary,
    surface = EditorialCardBg,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF26213A),
    onSurfaceVariant = TextSecondary,
    outline = EditorialCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = GoldAccentDark,
    onPrimary = Color.White,
    secondary = Color(0xFF5B4A78),
    onSecondary = Color.White,
    tertiary = LowSeverityBlue,
    background = Color(0xFFFAF8F5),
    onBackground = Color(0xFF1C1924),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1924),
    surfaceVariant = Color(0xFFF0EBE3),
    onSurfaceVariant = Color(0xFF5E5770),
    outline = Color(0xFFE0D8CC)
)

@Composable
fun ManuscriptSentinelTheme(
    darkTheme: Boolean = true, // Default to rich Dark Editorial canvas
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ManuscriptSentinelTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
