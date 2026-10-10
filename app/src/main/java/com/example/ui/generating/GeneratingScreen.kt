package com.example.ui.generating

import com.example.ui.localization.LocalAppLanguage

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MagicBackground
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun GeneratingScreen(
    categoryEmoji: String,
    categoryTitle: String,
    isNightMode: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui = LocalAppLanguage.current
    var messageIndex by remember { mutableIntStateOf(0) }
    val messages = remember(ui) {
        listOf(
            ui.text("Čarolija je počela — stvaramo tvoju priču...", "Der Zauber beginnt – deine Geschichte entsteht..."),
            ui.text("Zvijezde šapuću najljepše avanture...", "Die Sterne flüstern die schönsten Abenteuer..."),
            ui.text("Crtamo čarobne svjetove pune čuda...", "Wir erschaffen magische Welten voller Wunder..."),
            ui.text("Još samo tren, priča se već otvara...", "Nur noch einen Moment, die Geschichte beginnt bald...")
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2800)
            messageIndex = (messageIndex + 1) % messages.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "generating_book")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gen_pulse"
    )

    val spin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing)
        ),
        label = "gen_spin"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isNightMode) listOf(DreamNightBg, Color(0xFF1E1B4B), DreamNightCard)
                    else listOf(DreamDayBg, Color(0xFFFEF3C7), Color(0xFFEDE9FE))
                )
            )
            .testTag("generating_screen"),
        contentAlignment = Alignment.Center
    ) {
        MagicBackground(isNightMode = isNightMode)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            // Rotating & Pulsing Magical Book
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(pulse),
                contentAlignment = Alignment.Center
            ) {
                // Orbiting sparkles halo
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .rotate(spin)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    MagicGold.copy(alpha = 0.6f),
                                    MagicPink.copy(alpha = 0.5f),
                                    MagicSky.copy(alpha = 0.5f),
                                    MagicPurple.copy(alpha = 0.5f),
                                    MagicGold.copy(alpha = 0.6f)
                                )
                            )
                        )
                )

                // Open Book Badge
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = if (isNightMode) DreamNightSurface else Color.White,
                    shadowElevation = 14.dp,
                    modifier = Modifier.size(120.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "📖", fontSize = 56.sp)
                    }
                }

                // Category emoji floating
                Text(
                    text = categoryEmoji,
                    fontSize = 32.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-8).dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main magical message
            Text(
                text = messages[messageIndex],
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isNightMode) Color.White else MagicPurple
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = ui.text("Kategorija: $categoryTitle", "Kategorie: $categoryTitle"),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = if (isNightMode) MagicGoldLight else MagicCoral
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Linear smooth progress indicator
            LinearProgressIndicator(
                color = MagicPurple,
                trackColor = MagicPurple.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.testTag("cancel_generation_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isNightMode) Color.White else MagicPurple
                )
            ) {
                Text(ui.text("Prekini stvaranje", "Erstellung abbrechen"))
            }
            Text(
                text = ui.text(
                    "Tvoji izbori ostaju sačuvani.",
                    "Deine Einstellungen bleiben erhalten."
                ),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = if (isNightMode) DreamNightTextSec else DreamDayTextSec
            )
        }
    }
}
