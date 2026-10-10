package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.generating.GeneratingScreen
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.theme.SvijetSnovaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GenerationControlsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun cancelButtonIsAvailableInBothInterfaceLanguages() {
        var language by mutableStateOf(AppLanguage.BOSNIAN)
        var cancellations = 0
        compose.setContent {
            CompositionLocalProvider(LocalAppLanguage provides language) {
                SvijetSnovaTheme {
                    GeneratingScreen(
                        categoryEmoji = "✨", categoryTitle = "Test",
                        isNightMode = false, onCancel = { cancellations++ }
                    )
                }
            }
        }
        compose.onNodeWithTag("cancel_generation_button").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Prekini stvaranje").assertIsDisplayed()
        compose.runOnIdle { language = AppLanguage.GERMAN }
        compose.onNodeWithTag("cancel_generation_button").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Erstellung abbrechen").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, cancellations) }
    }
}
