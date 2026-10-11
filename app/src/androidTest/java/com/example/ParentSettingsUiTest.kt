package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.model.GeminiVoices
import com.example.data.security.*
import com.example.ui.ParentKeyState
import com.example.ui.localization.*
import com.example.ui.parents.*
import com.example.ui.theme.SvijetSnovaTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParentSettingsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun providerChangeClearsDraftAndChoicesAreIndependentInBothLanguages() {
        var language by mutableStateOf(AppLanguage.BOSNIAN)
        var provider by mutableStateOf(AiProvider.GEMINI)
        var voice by mutableStateOf(GeminiVoices.VOICE_AOEDE)
        var submitted: Pair<AiProvider, String>? = null
        compose.setContent {
            CompositionLocalProvider(LocalAppLanguage provides language) {
                SvijetSnovaTheme {
                    ParentSettingsScreen(ParentKeyState(),
                        onCheckAndSave = { item, key -> submitted = item to key }, onRemove = {}, onCancelCheck = {},
                        onBack = {})
                }
            }
        }
        compose.onNodeWithTag("api_key_input").performScrollTo().performTextInput("test-key-never-real")
        compose.onNodeWithTag("key_provider_OPENAI").performScrollTo().performClick()
        compose.onNodeWithTag("api_key_input").performScrollTo()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithTag("api_key_input").performTextInput("another-test-key-never-real")
        compose.onNodeWithTag("save_api_key").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(AiProvider.OPENAI, submitted!!.first); assertEquals("another-test-key-never-real", submitted!!.second) }
        compose.onNodeWithTag("story_provider_OPENAI").assertDoesNotExist()
        compose.onNodeWithTag("voice_provider_OPENAI").assertDoesNotExist()
        compose.runOnIdle { language = AppLanguage.GERMAN }
        compose.onNodeWithText("Private API-Schlüssel").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("close_parent_settings").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Fertig").assertIsDisplayed()
    }

    @Test fun deletionRequiresConfirmationAndBusyCheckCanBeCancelled() {
        var state by mutableStateOf(ParentKeyState(saved = setOf(AiProvider.GEMINI, AiProvider.OPENAI)))
        var removed: AiProvider? = null
        var cancellations = 0
        compose.setContent {
            CompositionLocalProvider(LocalAppLanguage provides AppLanguage.BOSNIAN) {
                SvijetSnovaTheme {
                    ParentSettingsScreen(state,
                        onCheckAndSave = { _, _ -> }, onRemove = { removed = it },
                        onCancelCheck = { cancellations++ }, onBack = {})
                }
            }
        }
        compose.onNodeWithTag("remove_api_key").performScrollTo().performClick()
        compose.runOnIdle { assertNull(removed) }
        compose.onNodeWithText("Odustani").performClick()
        compose.runOnIdle { assertNull(removed) }
        compose.onNodeWithTag("remove_api_key").performClick()
        compose.onNodeWithText("Ukloni", substring = false).performClick()
        compose.runOnIdle { assertEquals(AiProvider.GEMINI, removed); state = state.copy(busy = true) }
        compose.onNodeWithTag("save_api_key").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("cancel_key_check").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, cancellations) }
    }

    @Test fun childGateDoesNotUnlockWithAnIncorrectAnswer() {
        var unlocked = false
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = ParentPinStore(context, "test_parent_gate")
        store.set("2468")
        compose.setContent { SvijetSnovaTheme { ParentGate(onDismiss = {}, onUnlock = { unlocked = true }, pinStore = store) } }
        compose.onNodeWithTag("parent_gate_answer").performTextInput("0000")
        compose.onNodeWithTag("parent_gate_unlock").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Pogrešan PIN. Pokušaj ponovo.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Pogrešan PIN. Pokušaj ponovo.").assertIsDisplayed()
        compose.runOnIdle { assertFalse(unlocked) }
        context.deleteSharedPreferences("test_parent_gate")
    }

    @Test fun androidKeystoreRoundTripDoesNotStorePlainTextAndPreservesTheOtherProvider() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val name = "test_ai_keystore_only"
        val prefs = context.getSharedPreferences(name, 0)
        prefs.edit().clear().commit()
        val store = AiKeyStore(context, name)
        try {
            store.save(AiProvider.GEMINI, "test-gemini-key-not-real")
            store.save(AiProvider.OPENAI, "test-openai-key-not-real")
            assertEquals("test-gemini-key-not-real", AiKeyStore(context, name).read(AiProvider.GEMINI))
            assertFalse(prefs.all.toString().contains("test-gemini-key-not-real"))
            assertFalse(prefs.all.toString().contains("test-openai-key-not-real"))
            store.remove(AiProvider.GEMINI)
            assertEquals("", store.read(AiProvider.GEMINI))
            assertEquals("test-openai-key-not-real", store.read(AiProvider.OPENAI))
        } finally { context.deleteSharedPreferences(name) }
    }
}
