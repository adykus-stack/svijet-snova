package com.example.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.MusicMood
import com.example.tts.MusicSettings
import com.example.tts.NarrationProgress
import com.example.ui.localization.LocalAppLanguage
import kotlin.math.roundToInt

@Composable
fun MusicControls(
    settings: MusicSettings,
    automaticMood: MusicMood,
    progress: NarrationProgress,
    onEnabledChange: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSelectionChange: (MusicMood?) -> Unit
) {
    val ui = LocalAppLanguage.current
    var expanded by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val mood = settings.selectedMood ?: automaticMood
    val selection = "${mood.emoji} ${ui.text(mood.labelBs, mood.labelDe)}"
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth().testTag("music_controls")
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(ui.text("🎵 Muzika u pozadini", "🎵 Hintergrundmusik"),
                        style = MaterialTheme.typography.titleSmall)
                    if (settings.enabled) {
                        Text(
                            if (settings.selectedMood == null) ui.text("Automatski · $selection", "Automatisch · $selection")
                            else selection,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Switch(
                    checked = settings.enabled, onCheckedChange = onEnabledChange,
                    modifier = Modifier.testTag("music_enabled_switch")
                )
            }
            if (progress.total > 0) {
                Text(
                    if (progress.inGap) ui.text("Muzička pauza između odlomaka", "Musikpause zwischen den Absätzen")
                    else ui.text("Čitanje odlomka ${progress.paragraph} od ${progress.total}",
                        "Absatz ${progress.paragraph} von ${progress.total}"),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag("narration_progress")
                )
            }
            if (settings.enabled) {
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("music_settings_toggle")) {
                    Text(ui.text("Podesi muziku", "Musik einstellen") + if (expanded) " ▴" else " ▾")
                }
            }
            AnimatedVisibility(visible = expanded && settings.enabled) {
                Column {
                    Text(ui.text("Glasnoća muzike", "Musiklautstärke") + ": ${(settings.volume * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = settings.volume, onValueChange = onVolumeChange, valueRange = 0f..0.60f,
                        modifier = Modifier.testTag("music_volume_slider")
                    )
                    Text(ui.text("Tiša tokom govora, izraženija između odlomaka.",
                        "Leiser beim Sprechen, etwas lauter zwischen den Absätzen."),
                        style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Box {
                        OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("music_mood_selector")) {
                            Text(ui.text("Izaberi podlogu", "Musik auswählen") + " ▾")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(ui.text("Automatski prema priči", "Automatisch zur Geschichte")) },
                                onClick = { onSelectionChange(null); menuOpen = false },
                                modifier = Modifier.testTag("music_mood_auto")
                            )
                            MusicMood.entries.forEach { choice ->
                                DropdownMenuItem(
                                    text = { Text("${choice.emoji} ${ui.text(choice.labelBs, choice.labelDe)}") },
                                    onClick = { onSelectionChange(choice); menuOpen = false },
                                    modifier = Modifier.testTag("music_mood_${choice.code}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
