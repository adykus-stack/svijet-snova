package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.random.Random

data class StarParticle(
    val xRatio: Float,
    val yRatio: Float,
    val size: Float,
    val phaseOffset: Float,
    val color: Color
)

@Composable
fun MagicBackground(
    isNightMode: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "magic_stars")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_pulse"
    )

    val cloudDrift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_drift"
    )

    // Pre-calculate stars once
    val stars = remember {
        val colors = listOf(
            Color(0xFFFFDE59),
            Color(0xFFBAE6FD),
            Color(0xFFDDD6FE),
            Color(0xFFFFFFFF),
            Color(0xFFFCE7F3)
        )
        List(28) {
            StarParticle(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                size = Random.nextFloat() * 4f + 2f,
                phaseOffset = Random.nextFloat() * 3.14f,
                color = colors[Random.nextInt(colors.size)]
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Subtle background clouds in corners
        val cloudColor = if (isNightMode) Color(0x1538BDF8) else Color(0x30FDE68A)

        // Draw soft clouds
        drawCircle(
            color = cloudColor,
            radius = width * 0.45f,
            center = Offset(width * 0.15f + cloudDrift * 0.15f, height * 0.12f)
        )
        drawCircle(
            color = cloudColor,
            radius = width * 0.5f,
            center = Offset(width * 0.9f - cloudDrift * 0.1f, height * 0.85f)
        )

        // Draw twinkling stars
        stars.forEachIndexed { idx, star ->
            val starPulse = (kotlin.math.sin(pulse * 3.14 + star.phaseOffset) * 0.5 + 0.5).toFloat()
            val alpha = if (isNightMode) 0.35f + starPulse * 0.65f else 0.15f + starPulse * 0.4f
            val starSize = star.size * (0.8f + starPulse * 0.5f)

            val cx = star.xRatio * width
            val cy = star.yRatio * height

            // Draw 4-point sparkle for some stars, simple circle for others
            if (idx % 3 == 0) {
                val path = Path().apply {
                    moveTo(cx, cy - starSize * 2f)
                    lineTo(cx + starSize * 0.5f, cy - starSize * 0.5f)
                    lineTo(cx + starSize * 2f, cy)
                    lineTo(cx + starSize * 0.5f, cy + starSize * 0.5f)
                    lineTo(cx, cy + starSize * 2f)
                    lineTo(cx - starSize * 0.5f, cy + starSize * 0.5f)
                    lineTo(cx - starSize * 2f, cy)
                    lineTo(cx - starSize * 0.5f, cy - starSize * 0.5f)
                    close()
                }
                drawPath(path, star.color.copy(alpha = alpha))
            } else {
                drawCircle(
                    color = star.color.copy(alpha = alpha),
                    radius = starSize,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}
