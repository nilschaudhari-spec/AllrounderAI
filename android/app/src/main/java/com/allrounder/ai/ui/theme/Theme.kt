package com.allrounder.ai.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF334155)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

// Trading Financial Accents
val BullishGreen = Color(0xFF10B981) // Green for Buy
val BearishRed = Color(0xFFEF4444)   // Red for Sell
val AccentBlue = Color(0xFF3B82F6)   // Blue for Golden Cross EMA 50
val AccentOrange = Color(0xFFF59E0B) // Indicator line
val NeutralGray = Color(0xFF64748B)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    secondary = BullishGreen,
    tertiary = AccentOrange,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun AllRounderAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography(),
        content = content
    )
}
