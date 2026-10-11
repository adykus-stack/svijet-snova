package com.example

import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppLanguagePreferences
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AppLanguageUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var originalLanguage: AppLanguage

    @Before
    fun rememberPreference() {
        originalLanguage = AppLanguagePreferences(compose.activity).get()
        compose.activity.runOnUiThread {
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @After
    fun restorePreference() {
        AppLanguagePreferences(compose.activity).set(originalLanguage)
    }

    private fun chooseLanguage(code: String, closeSettings: Boolean = true) {
        compose.onNodeWithTag("open_settings").performClick()
        compose.onNodeWithTag("app_language_selector").performClick()
        compose.onNodeWithTag("app_language_$code").performClick()
        compose.onNodeWithText(if (code == "de") "App-Sprache" else "Jezik aplikacije").assertIsDisplayed()
        if (closeSettings) compose.onNodeWithTag("settings_back_button").performClick()
    }

    @Test
    fun menusSwitchImmediatelyAndLanguageSurvivesReopening() {
        chooseLanguage("de")
        compose.onNodeWithText("Überspringen").assertIsDisplayed()
        compose.onNodeWithTag("skip_intro_button").performClick()
        compose.onNodeWithTag("proceed_to_setup_button").performClick()
        compose.onNodeWithText("3–5 Jahre").assertIsDisplayed()
        compose.onNodeWithText("Meine Geschichte erstellen ✨").assertIsDisplayed()
        val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), "german-setup-verification.png")
            .outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithTag("lang_card_bs").performScrollTo().assertIsDisplayed()

        chooseLanguage("bs")
        compose.onNodeWithText("Stvori moju priču ✨").assertIsDisplayed()
        compose.onNodeWithTag("lang_card_bs").performScrollTo().assertIsDisplayed()
        chooseLanguage("de", closeSettings = false)
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("App-Sprache").assertIsDisplayed()
        compose.onNodeWithTag("settings_back_button").performClick()
        // Returning from settings retains the story configuration.
        compose.onNodeWithText("Meine Geschichte erstellen ✨").assertIsDisplayed()
    }
}
