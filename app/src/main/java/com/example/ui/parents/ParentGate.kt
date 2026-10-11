package com.example.ui.parents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.security.ParentPinStore
import com.example.ui.localization.LocalAppLanguage
import kotlinx.coroutines.*

@Composable
fun ParentGate(onDismiss: () -> Unit, onUnlock: () -> Unit, changePin: Boolean = false, pinStore: ParentPinStore? = null) {
    val ui = LocalAppLanguage.current
    val context = LocalContext.current
    val store = remember { pinStore ?: ParentPinStore(context) }
    val scope = rememberCoroutineScope()
    var setup by remember { mutableStateOf(!store.exists() || changePin) }
    var reset by remember { mutableStateOf(false) }
    var taps by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableLongStateOf(0L) }
    var answer by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("PIN", modifier = Modifier.testTag("pin_reset_label").clickable(enabled = !busy) {
            val now = System.currentTimeMillis()
            taps = if (now - lastTap <= 2500) taps + 1 else 1
            lastTap = now
            if (taps == 7) { reset = true; taps = 0 }
        }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (setup) ui.text("Kreiraj roditeljski PIN od 4 cifre.", "Lege eine Eltern-PIN mit 4 Ziffern fest.")
                    else ui.text("Unesi roditeljski PIN.", "Gib die Eltern-PIN ein."))
                OutlinedTextField(answer, { answer = it.filter { c -> c in '0'..'9' }.take(4); error = "" },
                    modifier = Modifier.testTag("parent_gate_answer"), singleLine = true, enabled = !busy,
                    label = { Text("PIN") }, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), isError = error.isNotEmpty())
                if (setup) OutlinedTextField(confirmation, { confirmation = it.filter { c -> c in '0'..'9' }.take(4); error = "" },
                    modifier = Modifier.testTag("pin_confirmation"), singleLine = true, enabled = !busy,
                    label = { Text(ui.text("Ponovi PIN", "PIN wiederholen")) }, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = { TextButton(enabled = !busy && answer.length == 4 && (!setup || confirmation.length == 4), onClick = {
            if (setup && answer != confirmation) error = ui.text("PIN-ovi se ne podudaraju.", "Die PINs stimmen nicht überein.")
            else scope.launch {
                busy = true
                try {
                    val ok = withContext(Dispatchers.IO) { if (setup) { store.set(answer); true } else store.verify(answer) }
                    if (ok) onUnlock() else error = if (store.waitSeconds() > 0)
                        ui.text("Sačekaj ${store.waitSeconds()} sekundi pa pokušaj ponovo.", "Warte ${store.waitSeconds()} Sekunden und versuche es erneut.")
                        else ui.text("Pogrešan PIN. Pokušaj ponovo.", "Falsche PIN. Versuche es erneut.")
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { error = ui.text("PIN nije spremljen. Pokušaj ponovo.", "PIN konnte nicht gespeichert werden.") }
                finally { busy = false }
            }
        }, modifier = Modifier.testTag("parent_gate_unlock")) { Text(if (setup) ui.text("Spremi PIN", "PIN speichern") else ui.text("Otvori", "Öffnen")) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(ui.text("Odustani", "Abbrechen")) } })
    if (reset) AlertDialog(onDismissRequest = { reset = false },
        title = { Text(ui.text("Resetovati PIN?", "PIN zurücksetzen?")) },
        text = { Text(ui.text("Možeš kreirati novi PIN. Priče i API ključevi ostaju sačuvani.", "Du kannst eine neue PIN festlegen. Geschichten und API-Schlüssel bleiben erhalten.")) },
        confirmButton = { TextButton(onClick = { reset = false; setup = true; answer = ""; confirmation = ""; error = "" },
            modifier = Modifier.testTag("confirm_pin_reset")) { Text(ui.text("Kreiraj novi PIN", "Neue PIN erstellen")) } },
        dismissButton = { TextButton(onClick = { reset = false }) { Text(ui.text("Odustani", "Abbrechen")) } })
}
