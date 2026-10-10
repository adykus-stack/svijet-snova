package com.example.ui.localization

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun AppLanguageSelector(language: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                language.text("Jezik aplikacije", "App-Sprache"),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge
            )
            Box {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.testTag("app_language_selector")
                ) {
                    Text("🌐 ${language.displayName} ▾", maxLines = 1)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    AppLanguage.entries.forEach { choice ->
                        DropdownMenuItem(
                            text = { Text("${choice.displayName}${if (choice == language) " ✓" else ""}") },
                            onClick = {
                                onSelect(choice)
                                expanded = false
                            },
                            modifier = Modifier.testTag("app_language_${choice.code}")
                        )
                    }
                }
            }
        }
    }
}
