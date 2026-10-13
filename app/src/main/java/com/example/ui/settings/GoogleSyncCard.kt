package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.CloudSyncState
import com.example.ui.localization.LocalAppLanguage

@Composable
fun GoogleSyncCard(state: CloudSyncState, onSignIn: () -> Unit, onSync: () -> Unit, onSignOut: () -> Unit) {
    val ui = LocalAppLanguage.current
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)), modifier = Modifier.testTag("google_sync_card")) {
        Column(Modifier.fillMaxWidth().padding(16.dp).testTag("google_sync_content"),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (state.signedIn) Icons.Default.CloudDone else Icons.Default.AccountCircle, null)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(ui.text("Google račun i sinkronizacija", "Google-Konto und Synchronisierung"),
                        style = MaterialTheme.typography.titleMedium)
                    if (state.signedIn) Text(state.displayName.ifBlank { state.email }, style = MaterialTheme.typography.bodySmall)
                }
                if (state.busy) CircularProgressIndicator(Modifier.size(24.dp))
            }
            Text(ui.text(
                "Opcionalno. Priče, kronike, jezik, AI glas, brzina i muzika prenose se na drugi telefon. API ključevi, PIN i audio nikada se ne šalju.",
                "Optional. Geschichten, Chroniken, Sprache, KI-Stimme, Geschwindigkeit und Musik werden auf ein anderes Telefon übertragen. API-Schlüssel, PIN und Audio werden niemals hochgeladen."),
                style = MaterialTheme.typography.bodySmall)
            if (!state.signedIn) {
                Button(onClick = onSignIn, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("google_sign_in")) {
                    Text(ui.text("Dodaj Google račun", "Google-Konto hinzufügen"))
                }
            } else {
                Button(onClick = onSync, enabled = !state.busy, modifier = Modifier.fillMaxWidth().testTag("google_sync_now")) {
                    Text(ui.text("Sinkroniziraj sada", "Jetzt synchronisieren"))
                }
                OutlinedButton(onClick = onSignOut, enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().testTag("google_sign_out")) {
                    Text(ui.text("Odspoji račun", "Konto trennen"))
                }
            }
            if (state.messageBs.isNotBlank() || state.messageDe.isNotBlank()) {
                Text(ui.text(state.messageBs, state.messageDe), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
