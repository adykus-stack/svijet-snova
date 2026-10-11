package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.StoryImportState
import com.example.data.model.*
import com.example.data.importing.ChatGptImport
import com.example.data.security.AiProvider
import com.example.ui.localization.LocalAppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryImportScreen(state: StoryImportState, provider: AiProvider, onPick: () -> Unit,
    onSelect: (Int) -> Unit, onTitle: (String) -> Unit, onConfig: (StoryCreationConfig) -> Unit,
    onProvider: (AiProvider) -> Unit, onStart: () -> Unit, onBack: () -> Unit) {
    val ui = LocalAppLanguage.current
    Scaffold(containerColor = Color.Transparent, topBar = {
        TopAppBar(title = { Text(ui.text("Uvezi postojeću priču", "Bestehende Geschichte importieren")) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, ui.text("Nazad", "Zurück")) } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(ui.text("Izaberi ZIP izvoz sa svog ChatGPT računa. Uvoz pravi kroniku cijelog razgovora i jedno početno poglavlje za približno 7 minuta čitanja.",
                "Wähle den ZIP-Export deines ChatGPT-Kontos. Der Import erstellt eine Chronik des Gesprächs und ein erstes Kapitel für etwa 7 Minuten Vorlesezeit."))
            OutlinedButton(onClick = onPick, enabled = !state.loading, modifier = Modifier.fillMaxWidth().testTag("pick_chatgpt_export")) {
                Text(ui.text("Izaberi ChatGPT ZIP", "ChatGPT-ZIP auswählen"))
            }
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.selected?.let { selected ->
                Text(ui.text("Razgovor: ${selected.title}", "Gespräch: ${selected.title}"), style = MaterialTheme.typography.titleMedium)
                if (state.conversations.size > 1) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("import_conversation_picker")) {
                            Text(ui.text("Izaberi drugi razgovor", "Anderes Gespräch auswählen"))
                        }
                        DropdownMenu(expanded, { expanded = false }) {
                            state.conversations.forEachIndexed { index, conversation ->
                                DropdownMenuItem(text = { Text(conversation.title) }, onClick = { onSelect(index); expanded = false })
                            }
                        }
                    }
                }
                Text(ui.text("${selected.messageCount} poruka. Provjeri da je ovo tvoja serija:", "${selected.messageCount} Nachrichten. Prüfe, ob dies deine Geschichte ist:"))
                Text(selected.transcript.takeLast(900), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(state.title, onTitle, label = { Text(ui.text("Naslov serije", "Titel der Serie")) },
                    singleLine = true, modifier = Modifier.fillMaxWidth().testTag("import_story_title"))
                var categoriesOpen by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { categoriesOpen = true }, modifier = Modifier.fillMaxWidth().testTag("import_category_picker")) {
                        Text("${state.config.category.emoji} " + ui.text(state.config.category.titleBs, state.config.category.titleDe))
                    }
                    DropdownMenu(categoriesOpen, { categoriesOpen = false }) {
                        StoryCategories.allCategories.forEach { category -> DropdownMenuItem(
                            text = { Text("${category.emoji} " + ui.text(category.titleBs, category.titleDe)) },
                            onClick = { onConfig(state.config.copy(category = category)); categoriesOpen = false },
                            modifier = Modifier.testTag("import_category_${category.id}")) }
                    }
                }
                Text(ui.text("Jezik budućih poglavlja", "Sprache zukünftiger Kapitel"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StoryLanguage.entries.forEach { lang -> FilterChip(state.config.language == lang,
                        onClick = { onConfig(state.config.copy(language = lang)) }, label = { Text("${lang.flag} ${lang.displayName}") }) }
                }
                Text(ui.text("Uzrast djeteta", "Alter des Kindes"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AgeGroupId.entries.forEach { age -> FilterChip(state.config.ageGroup == age,
                        onClick = { onConfig(state.config.copy(ageGroup = age)) }, label = { Text(age.ageRange) }) }
                }
                Text(ui.text("Ko pravi kroniku i nastavlja priču?", "Wer erstellt die Chronik und setzt die Geschichte fort?"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiProvider.entries.forEach { item -> FilterChip(provider == item, onClick = { onProvider(item) }, label = { Text(item.label) }) }
                }
                val requests = remember(selected.fingerprint) { ChatGptImport.chunks(selected.transcript).size + 1 }
                Text(ui.text("Na telefon se sprema originalni razgovor. Samo ovaj izabrani razgovor šalje se ${provider.label}-u: do $requests zahtjeva za kroniku i početno poglavlje. Uvoz troši API kvotu i može potrajati. Prekinuti uvoz možeš nastaviti ponovnim izborom istog ZIP-a i razgovora.",
                    "Das Originalgespräch wird auf dem Telefon gespeichert. Nur das ausgewählte Gespräch wird an ${provider.label} gesendet: bis zu $requests Anfragen für Chronik und erstes Kapitel. Der Import nutzt dein API-Kontingent und kann dauern. Wähle nach einer Unterbrechung denselben ZIP und dasselbe Gespräch erneut."),
                    style = MaterialTheme.typography.bodySmall)
                Button(onClick = onStart, enabled = state.title.isNotBlank() && !state.loading,
                    modifier = Modifier.fillMaxWidth().testTag("start_story_import")) { Text(ui.text("Uvezi i sačuvaj kroniku", "Importieren und Chronik speichern")) }
            }
        }
    }
}
