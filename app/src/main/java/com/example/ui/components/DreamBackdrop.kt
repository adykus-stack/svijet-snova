package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.testTag
import com.example.ui.theme.*
import kotlin.math.sin
import kotlin.random.Random

/** Native drawing, no downloaded images or network requests. */
@Composable
fun DreamBackdrop(theme: AppTheme, modifier: Modifier = Modifier) {
    val stars = remember {
        val random = Random(810)
        List(48) { floatArrayOf(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat() * 6.28f) }
    }
    val pulse = if (theme == AppTheme.MOONLIGHT) {
        rememberInfiniteTransition(label = "moonlight").animateFloat(0f, 6.28f,
            infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "twinkle")
    } else null
    Canvas(modifier.fillMaxSize().testTag("theme_background_${theme.name}")) {
        val colors = when (theme) {
            AppTheme.DARK -> listOf(DreamNightBg, Color(0xFF151B32), Color(0xFF0D1527))
            AppTheme.LIGHT -> listOf(DreamDayBg, Color(0xFFF5F0FF), Color(0xFFEFF7FF))
            AppTheme.MOONLIGHT -> listOf(Color(0xFF070D21), Color(0xFF12203E), Color(0xFF0A142B))
        }
        drawRect(Brush.verticalGradient(colors))
        if (theme == AppTheme.MOONLIGHT) {
            val center = Offset(size.width * 0.82f, size.height * 0.16f)
            val radius = size.width * 0.087f
            drawCircle(Brush.radialGradient(listOf(Color(0x44CFE7FF), Color.Transparent), center, radius * 4.4f),
                radius * 4.4f, center)
            val moon = Path().apply { addOval(Rect(center - Offset(radius, radius), center + Offset(radius, radius))) }
            val cutCenter = center + Offset(radius * 0.50f, -radius * 0.28f)
            val cutout = Path().apply { addOval(Rect(cutCenter - Offset(radius, radius), cutCenter + Offset(radius, radius))) }
            drawPath(Path.combine(PathOperation.Difference, moon, cutout),
                Brush.linearGradient(listOf(Color(0xFFFFF5CA), Color(0xFFCDE7FF))))
            stars.forEachIndexed { index, star ->
                val position = Offset(size.width * star[0], size.height * star[1])
                val alpha = 0.25f + (sin((pulse?.value ?: 0f) + star[3]) + 1f) * 0.27f
                val starSize = (0.8f + star[2] * 1.5f) * density
                val color = if (index % 4 == 0) MagicGoldLight.copy(alpha = alpha) else Color(0xFFDCEBFF).copy(alpha = alpha)
                drawCircle(color, starSize, position)
                if (index % 7 == 0) {
                    drawLine(color, position - Offset(starSize * 3, 0f), position + Offset(starSize * 3, 0f), density)
                    drawLine(color, position - Offset(0f, starSize * 3), position + Offset(0f, starSize * 3), density)
                }
            }
        }
    }
}
