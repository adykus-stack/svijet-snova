package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.localization.*
import com.example.ui.theme.*
import com.example.data.model.GeminiVoice
import com.example.data.model.GeminiVoices
import com.example.ui.CloudSyncState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(language: AppLanguage, theme: AppTheme, onLanguageChange: (AppLanguage) -> Unit,
    onThemeChange: (AppTheme) -> Unit, onOpenParents: () -> Unit, onBack: () -> Unit,
    voice: GeminiVoice = GeminiVoices.VOICE_AOEDE, onVoiceChange: (GeminiVoice) -> Unit = {},
    playbackSpeed: Float = 0.90f, onPlaybackSpeedChange: (Float) -> Unit = {},
    onImport: () -> Unit = {}, onBackup: () -> Unit = {}, onRestore: () -> Unit = {},
    backupBusy: Boolean = false, backupMessage: String = "",
    cloudSyncState: CloudSyncState = CloudSyncState(), onGoogleSignIn: () -> Unit = {},
    onGoogleSync: () -> Unit = {}, onGoogleSignOut: () -> Unit = {}) {
    val ui = LocalAppLanguage.current
    Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground, topBar = {
        TopAppBar(title = { Text(ui.text("Postavke", "Einstellungen")) },
            navigationIcon = { IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, ui.text("Nazad", "Zurück"))
            } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(22.dp)) { AppLanguageSelector(language, onLanguageChange) }
            OutlinedButton(onClick = onOpenParents, shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("open_parent_settings")) {
                Icon(Icons.Default.Lock, null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(ui.text("Roditeljski nadzor", "Elternkontrolle"), fontWeight = FontWeight.Bold)
                    Text(ui.text("Privatni API ključevi i PIN", "Private API-Schlüssel und PIN"),
                        style = MaterialTheme.typography.bodySmall)
                }
                Text("›", fontSize = 28.sp)
            }
            DefaultVoicePicker(voice, onVoiceChange)
            PlaybackSpeedControl(playbackSpeed, onPlaybackSpeedChange)
            HorizontalDivider()
            OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth().testTag("open_story_import")) {
                Text(ui.text("Uvezi priču iz ChatGPT-a", "Geschichte aus ChatGPT importieren"))
            }
            GoogleSyncCard(cloudSyncState, onGoogleSignIn, onGoogleSync, onGoogleSignOut)
            Text(ui.text("Sigurnosna kopija na Google Drive", "Sicherung auf Google Drive"), style = MaterialTheme.typography.titleMedium)
            Text(ui.text("Pri spremanju izaberi Google Drive i svoj račun u sistemskom prozoru. Ako ga nema, instaliraj Drive i prijavi se. Kopija čuva priče, kronike i originalni razgovor. API ključevi, PIN i audio nisu uključeni. Kopiju pokrećeš ručno.",
                "Wähle beim Speichern Google Drive und dein Konto im Systemfenster. Falls Drive fehlt, installiere es und melde dich an. Geschichten, Chroniken und das Originalgespräch werden gesichert. API-Schlüssel, PIN und Audio sind nicht enthalten. Du startest die Sicherung manuell."),
                style = MaterialTheme.typography.bodySmall)
            Button(onClick = onBackup, enabled = !backupBusy, modifier = Modifier.fillMaxWidth().testTag("drive_backup")) {
                Text(ui.text("Spremi sigurnosnu kopiju", "Sicherung speichern"))
            }
            OutlinedButton(onClick = onRestore, enabled = !backupBusy, modifier = Modifier.fillMaxWidth().testTag("drive_restore")) {
                Text(ui.text("Vrati kopiju s Google Drivea", "Sicherung aus Google Drive wiederherstellen"))
            }
            if (backupBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (backupMessage.isNotEmpty()) Text(backupMessage, style = MaterialTheme.typography.bodySmall)
            Text(ui.text("Izgled aplikacije", "Aussehen der App"), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Text(ui.text("Pri svakom novom pokretanju aplikacija počinje tamnom temom. Ovdje izaberi izgled za trenutnu sesiju.",
                "Bei jedem neuen Start beginnt die App im dunklen Design. Wähle hier das Design für die aktuelle Sitzung."),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppTheme.entries.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { option ->
                        val selected = theme == option
                        Card(onClick = { onThemeChange(option) }, shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(if (selected) 2.dp else 1.dp,
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                            modifier = Modifier.weight(1f).semantics { this.selected = selected }.testTag("app_theme_${option.name}")) {
                            Box(Modifier.fillMaxWidth().height(100.dp)) {
                                Canvas(Modifier.fillMaxSize()) {
                                    drawRect(when (option) { AppTheme.LIGHT -> Color(0xFFF9F3E7); AppTheme.DARK -> DreamNightBg; AppTheme.MOONLIGHT -> Color(0xFF0B1734) })
                                    if (option == AppTheme.MOONLIGHT) {
                                        listOf(.15f to .24f, .37f to .58f, .81f to .76f, .25f to .80f, .49f to .12f).forEach { (x,y) ->
                                            drawCircle(Color(0xFFCFE6FF), 2.dp.toPx(), Offset(size.width*x, size.height*y))
                                        }
                                    }
                                }
                                Text(option.emoji, fontSize = 32.sp, modifier = Modifier.align(Alignment.Center))
                                if (selected) Icon(Icons.Default.CheckCircle, ui.text("Izabrano", "Ausgewählt"),
                                    tint = MagicMintLight, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(22.dp))
                            }
                            Text(ui.text(option.titleBs, option.titleDe), modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Text(ui.text("Tamna i mjesečeva tema čuvaju nježan izgled za večernje čitanje.",
                "Dunkles Design und Mondschein schaffen eine ruhige Atmosphäre beim Vorlesen am Abend."),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
