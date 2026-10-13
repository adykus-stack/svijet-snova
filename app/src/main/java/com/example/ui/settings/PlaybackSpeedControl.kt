package com.example.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.tts.StoryAudioPlayer
import com.example.ui.localization.LocalAppLanguage
import java.util.Locale
import kotlin.math.abs

@Composable
fun PlaybackSpeedControl(speed: Float, onSpeedChange: (Float) -> Unit, compact: Boolean = false) {
    val ui = LocalAppLanguage.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("playback_speed_control")) {
        Text(ui.text("Brzina čitanja", "Vorlesegeschwindigkeit"),
            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium)
        LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(StoryAudioPlayer.SPEED_OPTIONS) { option ->
                val label = String.format(Locale.ROOT, if (option % 1f == 0f) "%.1f×" else "%.2f×", option)
                    .replace(".00", ".0")
                FilterChip(selected = abs(speed - option) < 0.001f, onClick = { onSpeedChange(option) },
                    label = { Text(label) }, modifier = Modifier.testTag("playback_speed_${(option * 100).toInt()}"))
            }
        }
        if (!compact) Text(ui.text(
            "Mijenja se samo brzina AI glasa. Muzika ostaje prirodna, a promjena ne troši dodatnu API kvotu.",
            "Nur die KI-Stimme wird schneller oder langsamer. Die Musik bleibt unverändert und es wird kein zusätzliches API-Kontingent verbraucht."),
            style = MaterialTheme.typography.bodySmall)
    }
}
