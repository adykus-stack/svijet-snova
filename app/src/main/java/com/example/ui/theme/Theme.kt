package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = MagicPurple,
    onPrimary = Color.White,
    primaryContainer = MagicPurpleLight,
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = MagicGold,
    onSecondary = Color.White,
    secondaryContainer = MagicGoldLight,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = MagicSky,
    onTertiary = Color.White,
    tertiaryContainer = MagicSkyLight,
    onTertiaryContainer = Color(0xFF0369A1),
    background = DreamDayBg,
    onBackground = DreamDayText,
    surface = DreamDaySurface,
    onSurface = DreamDayText,
    surfaceVariant = DreamDayCard,
    onSurfaceVariant = DreamDayTextSec
)

private val DarkColorScheme = darkColorScheme(
    primary = MagicPurpleLight,
    onPrimary = Color(0xFF2E1065),
    primaryContainer = MagicPurple,
    onPrimaryContainer = Color.White,
    secondary = MagicGoldLight,
    onSecondary = Color(0xFF78350F),
    secondaryContainer = MagicGold,
    onSecondaryContainer = Color.White,
    tertiary = MagicSkyLight,
    onTertiary = Color(0xFF0369A1),
    tertiaryContainer = MagicSky,
    onTertiaryContainer = Color.White,
    background = DreamNightBg,
    onBackground = DreamNightText,
    surface = DreamNightSurface,
    onSurface = DreamNightText,
    surfaceVariant = DreamNightCard,
    onSurfaceVariant = DreamNightTextSec
)

val DreamShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SvijetSnovaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = DreamShapes,
        content = content
    )
}
