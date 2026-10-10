package com.example.ui.intro

import com.example.ui.localization.LocalAppLanguage

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MagicBackground
import com.example.ui.components.MagicButton
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun IntroScreen(
    onStartAdventure: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui = LocalAppLanguage.current
    var animationStage by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(600)
        animationStage = 1 // Book & Glow
        delay(800)
        animationStage = 2 // Welcome message
        delay(800)
        animationStage = 3 // Start button
    }

    val infiniteTransition = rememberInfiniteTransition(label = "intro_glow")
    val bookPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "book_pulse"
    )

    val haloRotate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        ),
        label = "halo_rotate"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF3B185F),
                        Color(0xFF1E293B)
                    )
                )
            )
    ) {
        MagicBackground(isNightMode = true)

        // Skip button at top right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, end = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.testTag("skip_intro_button")
            ) {
                Text(
                    text = ui.text("Preskoči", "Überspringen"),
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        // Center animation content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Magical glowing book illustration
            AnimatedVisibility(
                visible = animationStage >= 1,
                enter = fadeIn(tween(800)) + scaleIn(tween(800, easing = OvershootInterpolator))
            ) {
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .scale(bookPulse),
                    contentAlignment = Alignment.Center
                ) {
                    // Golden radiating halo
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MagicGold.copy(alpha = 0.65f),
                                        MagicPurple.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Open Book Emoji / Card
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        shadowElevation = 12.dp,
                        modifier = Modifier.size(110.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "📖", fontSize = 54.sp)
                        }
                    }

                    // Floating sparkles around book
                    Text(
                        text = "✨",
                        fontSize = 24.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp)
                    )
                    Text(
                        text = "⭐",
                        fontSize = 20.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 10.dp)
                    )
                    Text(
                        text = "🌙",
                        fontSize = 22.sp,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Welcome Text
            AnimatedVisibility(
                visible = animationStage >= 2,
                enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { 40 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = ui.text("Dobro došli u", "Willkommen in"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MagicGoldLight,
                            letterSpacing = 1.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = ui.text("Svijet snova", "Traumwelt"),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 36.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = ui.text("Mjesto gdje svaka mašta postaje priča.", "Wo jede Fantasie zu einer Geschichte wird."),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFFE2E8F0),
                            fontSize = 18.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(38.dp))

            // Start adventure button
            AnimatedVisibility(
                visible = animationStage >= 3,
                enter = fadeIn(tween(600)) + scaleIn(tween(600))
            ) {
                MagicButton(
                    text = ui.text("Započni avanturu ✨", "Abenteuer starten ✨"),
                    onClick = onStartAdventure,
                    icon = Icons.Default.PlayArrow,
                    gradientColors = listOf(MagicGold, MagicPink),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .testTag("start_adventure_button")
                )
            }
        }
    }
}

private val OvershootInterpolator = androidx.compose.animation.core.Easing { fraction ->
    val tension = 2.0f
    val f = fraction - 1.0f
    f * f * ((tension + 1) * f + tension) + 1.0f
}
