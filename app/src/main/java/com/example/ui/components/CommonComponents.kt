package com.example.ui.components

import com.example.ui.localization.LocalAppLanguage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun DreamTopBar(
    title: String = "",
    isNightMode: Boolean,
    savedCount: Int = 0,
    onOpenSettings: () -> Unit,
    onOpenLibrary: () -> Unit,
    onReplayIntro: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui = LocalAppLanguage.current
    Surface(
        color = if (isNightMode) DreamNightSurface.copy(alpha = 0.95f) else DreamDaySurface.copy(alpha = 0.95f),
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand title with magic wand icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onReplayIntro() }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(MagicPurple, MagicGold)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✨", fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { ui.text("Svijet snova", "Traumwelt") },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isNightMode) DreamNightText else MagicPurple
                        )
                    )
                    Text(
                        text = ui.text("Mjesto gdje mašta postaje priča", "Wo Fantasie zur Geschichte wird"),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isNightMode) DreamNightTextSec else DreamDayTextSec
                        )
                    )
                }
            }

            // Quick actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All appearance and parent controls live in Settings.
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .testTag("open_settings")
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isNightMode) MagicPurple.copy(alpha = 0.25f)
                            else MagicGold.copy(alpha = 0.2f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = ui.text("Postavke", "Einstellungen"),
                        tint = if (isNightMode) MagicGold else MagicPurple
                    )
                }

                // Library button with badge
                IconButton(
                    onClick = onOpenLibrary,
                    modifier = Modifier
                        .testTag("library_button")
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MagicSky.copy(alpha = 0.2f))
                ) {
                    BadgedBox(
                        badge = {
                            if (savedCount > 0) {
                                Badge(
                                    containerColor = MagicPink,
                                    contentColor = Color.White
                                ) {
                                    Text(text = savedCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = ui.text("Biblioteka sačuvanih priča", "Bibliothek gespeicherter Geschichten"),
                            tint = MagicSky
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MagicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = Icons.Default.AutoAwesome,
    emoji: String? = null,
    gradientColors: List<Color> = listOf(MagicPurple, MagicPink),
    enabled: Boolean = true,
    testTag: String = "magic_button"
) {
    val shape = RoundedCornerShape(22.dp)

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = Color.Transparent,
        modifier = modifier
            .testTag(testTag)
            .shadow(if (enabled) 6.dp else 0.dp, shape = shape)
            .heightIn(min = 52.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (enabled) Brush.horizontalGradient(gradientColors)
                    else Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.4f), Color.LightGray.copy(alpha = 0.4f)))
                )
                .padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (emoji != null) {
                    Text(text = emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SparkleBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = textColor
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DeleteConfirmationDialog(
    storyTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val ui = LocalAppLanguage.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = ui.text("Obrisati priču?", "Geschichte löschen?"),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = ui.text("Jeste li sigurni da želite ukloniti priču „$storyTitle“ iz vaše biblioteke?", "Möchtest du die Geschichte „$storyTitle“ wirklich aus deiner Bibliothek löschen?"),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MagicPink)
            ) {
                Text(ui.text("Obriši", "Löschen"), color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(ui.text("Otkaži", "Abbrechen"))
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
