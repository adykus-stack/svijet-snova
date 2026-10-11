package com.example.ui.parents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.security.AiProvider
import com.example.ui.ParentKeyState
import com.example.ui.localization.LocalAppLanguage

@Suppress("DEPRECATION")
@Composable
fun ParentSettingsScreen(
    keyState: ParentKeyState,
    onCheckAndSave: (AiProvider, String) -> Unit,
    onRemove: (AiProvider) -> Unit,
    onCancelCheck: () -> Unit,
    onBack: () -> Unit,
    onChangePin: () -> Unit = {}
) {
    val ui = LocalAppLanguage.current
    val clipboard = LocalClipboardManager.current
    val uri = LocalUriHandler.current
    var provider by remember { mutableStateOf(AiProvider.GEMINI) }
    // Never save drafts in a Bundle, ViewModel, or preferences.
    var draft by remember(provider) { mutableStateOf("") }
    var removal by remember { mutableStateOf<AiProvider?>(null) }
    LaunchedEffect(keyState.savedRevision) { draft = "" }
    DisposableEffect(Unit) { onDispose { draft = "" } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(ui.text("Roditeljski nadzor", "Elternkontrolle"), style = MaterialTheme.typography.headlineSmall)
        Text(ui.text("Privatni API ključevi", "Private API-Schlüssel"), style = MaterialTheme.typography.titleMedium)
        Text(ui.text("Izaberi čiji ključ dodaješ. Možeš spremiti jedan ili oba; nikad ih ne dijeli s drugima.",
            "Wähle den Anbieter des Schlüssels. Du kannst einen oder beide speichern; teile sie niemals mit anderen."))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AiProvider.entries.forEach { item ->
                FilterChip(selected = provider == item, enabled = !keyState.busy,
                    onClick = { onCancelCheck(); provider = item },
                    label = { Text(item.label) }, modifier = Modifier.testTag("key_provider_${item.name}"))
            }
        }
        Text(if (provider in keyState.saved) ui.text("${provider.label} ključ je spremljen. Ovdje ga možeš provjeriti ili zamijeniti.",
            "${provider.label}-Schlüssel gespeichert. Hier kannst du ihn prüfen oder ersetzen.")
            else ui.text("${provider.label} ključ nije dodan.", "Kein ${provider.label}-Schlüssel hinterlegt."),
            modifier = Modifier.testTag("key_status"))
        OutlinedTextField(value = draft, onValueChange = { draft = it.take(512) },
            modifier = Modifier.fillMaxWidth().testTag("api_key_input"), enabled = !keyState.busy,
            label = { Text(ui.text("Zalijepi ${provider.label} API ključ", "${provider.label}-API-Schlüssel einfügen")) },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { draft = clipboard.getText()?.text?.trim()?.take(512).orEmpty() }, enabled = !keyState.busy) {
                Text(ui.text("Zalijepi", "Einfügen"))
            }
            TextButton(onClick = { uri.openUri(provider.keyUrl) }, enabled = !keyState.busy) {
                Text(ui.text("Gdje naći ključ?", "Wo finde ich den Schlüssel?"))
            }
        }
        Button(onClick = { onCheckAndSave(provider, draft) },
            enabled = !keyState.busy && (draft.isNotBlank() || provider in keyState.saved),
            modifier = Modifier.fillMaxWidth().testTag("save_api_key")) {
            Text(ui.text(if (draft.isBlank()) "Provjeri spremljeni ključ" else "Provjeri i spremi ključ",
                if (draft.isBlank()) "Gespeicherten Schlüssel prüfen" else "Schlüssel prüfen und speichern"))
        }
        if (keyState.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            TextButton(onClick = onCancelCheck, modifier = Modifier.testTag("cancel_key_check")) {
                Text(ui.text("Prekini provjeru", "Prüfung abbrechen"))
            }
        }
        if (keyState.messageBs.isNotEmpty()) Text(ui.text(keyState.messageBs, keyState.messageDe),
            modifier = Modifier.testTag("key_message"), style = MaterialTheme.typography.bodySmall)
        if (provider in keyState.saved) OutlinedButton(onClick = { removal = provider }, enabled = !keyState.busy,
            modifier = Modifier.testTag("remove_api_key")) {
            Text(ui.text("Ukloni ${provider.label} ključ", "${provider.label}-Schlüssel entfernen"))
        }
        Text(ui.text("Ključ je šifriran na ovom telefonu i ne ide u sigurnosnu kopiju. Na drugom telefonu unesi ga ponovo. Priče i tekst za čitanje šalju se izabranom AI pružaocu.",
            "Der Schlüssel wird auf diesem Telefon verschlüsselt und nicht gesichert. Gib ihn auf einem anderen Telefon erneut ein. Geschichten und Vorlesetexte werden an den gewählten KI-Anbieter gesendet."),
            style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        OutlinedButton(onClick = onChangePin, modifier = Modifier.fillMaxWidth().testTag("change_parent_pin")) {
            Text(ui.text("Promijeni roditeljski PIN", "Eltern-PIN ändern"))
        }
        Text(ui.text("API potrošnja zavisi od tvog računa i limita. ChatGPT pretplata ne uključuje OpenAI API potrošnju. Glas je AI-generiran, nije ljudski snimak.",
            "API-Kosten hängen von deinem Konto und deinen Limits ab. Ein ChatGPT-Abonnement umfasst keine OpenAI-API-Nutzung. Die Stimme ist KI-generiert, keine menschliche Aufnahme."),
            style = MaterialTheme.typography.bodySmall)
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().testTag("close_parent_settings")) {
            Text(ui.text("Gotovo", "Fertig"))
        }
    }
    removal?.let { item ->
        AlertDialog(onDismissRequest = { removal = null },
            title = { Text(ui.text("Ukloniti ${item.label} ključ?", "${item.label}-Schlüssel entfernen?")) },
            text = { Text(ui.text("Uklanja se samo s ovog telefona. Drugi ključ i spremljene priče ostaju.",
                "Nur von diesem Telefon entfernen. Der andere Schlüssel und gespeicherte Geschichten bleiben erhalten.")) },
            confirmButton = { TextButton(onClick = { onRemove(item); removal = null }) { Text(ui.text("Ukloni", "Entfernen")) } },
            dismissButton = { TextButton(onClick = { removal = null }) { Text(ui.text("Odustani", "Abbrechen")) } })
    }
}
