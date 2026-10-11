package com.example.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

enum class AppTheme(val titleBs: String, val titleDe: String, val emoji: String, val isDark: Boolean) {
    DARK("Tamna", "Dunkel", "🌑", true),
    LIGHT("Svijetla", "Hell", "☀️", false),
    MOONLIGHT("Mjesečina", "Mondschein", "🌙", true)
}

val LocalAppTheme = staticCompositionLocalOf { AppTheme.DARK }
