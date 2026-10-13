package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.security.AiProvider
import com.example.ui.localization.LocalAppLanguage

@Composable
fun DefaultVoicePicker(voice: GeminiVoice, onChange: (GeminiVoice) -> Unit) {
    val ui = LocalAppLanguage.current
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(ui.text("Početni glas za čitanje", "Standardstimme zum Vorlesen"), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiProvider.entries.forEach { provider ->
                FilterChip(selected = voice.provider == provider,
                    onClick = { onChange(GeminiVoices.ALL_VOICES.first { it.provider == provider }) },
                    label = { Text(provider.label) }, modifier = Modifier.testTag("voice_provider_${provider.name}"))
            }
        }
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().testTag("default_voice_picker")) {
                Text("${voice.emoji} ${voice.displayName}")
            }
            DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                GeminiVoices.ALL_VOICES.filter { it.provider == voice.provider }
                    .forEach { item -> DropdownMenuItem(text = { Text("${item.emoji} ${item.displayName}") },
                        onClick = { onChange(item); expanded = false }) }
            }
        }
        Text(ui.text("Gemini je početni izbor. Ovaj izbor se pamti. Za AI glas dodaj odgovarajući ključ u roditeljskom nadzoru.",
            "Gemini ist vorausgewählt. Deine Auswahl wird gespeichert. Für KI-Stimmen hinterlege den passenden Schlüssel unter Elternkontrolle."),
            style = MaterialTheme.typography.bodySmall)
    }
}
