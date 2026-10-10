package com.example.ui.localization

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

/** Interface language is independent of StoryCreationConfig.language. */
enum class AppLanguage(val code: String, val displayName: String) {
    BOSNIAN("bs", "Bosanski"),
    GERMAN("de", "Deutsch");

    val isGerman: Boolean get() = this == GERMAN
    fun text(bosnian: String, german: String): String = if (isGerman) german else bosnian

    companion object {
        fun fromCode(code: String?): AppLanguage = if (code == "de") GERMAN else BOSNIAN
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.BOSNIAN }

class AppLanguagePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    fun get(): AppLanguage = AppLanguage.fromCode(preferences.getString("app_language", "bs"))
    fun set(language: AppLanguage) {
        preferences.edit().putString("app_language", language.code).apply()
    }
}
