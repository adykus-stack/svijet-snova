package com.example

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.importing.ChatConversation
import com.example.data.model.*
import com.example.data.security.*
import com.example.ui.StoryImportState
import com.example.ui.CloudSyncState
import com.example.ui.parents.ParentGate
import com.example.ui.settings.*
import com.example.ui.setup.StorySetupScreen
import com.example.ui.reader.StoryReaderScreen
import com.example.ui.localization.*
import com.example.ui.theme.*
import com.example.tts.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChronicleSettingsUiTest {
    @get:Rule val compose = createComposeRule()
    private fun content(body: @Composable () -> Unit) {
        compose.setContent { CompositionLocalProvider(LocalAppLanguage provides AppLanguage.BOSNIAN) { SvijetSnovaTheme { body() } } }
    }

    @Test fun pinCreationRequiresMatchingFourDigitsAndKeepsLeadingZero() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "test_pin_creation"
        context.deleteSharedPreferences(name)
        val store = ParentPinStore(context, name)
        var unlocked = false
        try {
            content { ParentGate(onDismiss = {}, onUnlock = { unlocked = true }, pinStore = store) }
            compose.onNodeWithTag("parent_gate_answer").performTextInput("0123")
            compose.onNodeWithTag("parent_gate_unlock").assertIsNotEnabled()
            compose.onNodeWithTag("pin_confirmation").performTextInput("1111")
            compose.onNodeWithTag("parent_gate_unlock").performClick()
            compose.onNodeWithText("PIN-ovi se ne podudaraju.").assertIsDisplayed()
            assertFalse(store.exists())
            compose.onNodeWithTag("pin_confirmation").performTextReplacement("0123")
            compose.onNodeWithTag("parent_gate_unlock").performClick()
            compose.waitUntil(5000) { unlocked }
            assertTrue(store.verify("0123"))
        } finally { context.deleteSharedPreferences(name) }
    }

    @Test fun sevenPinTapsOpenResetWithoutChangingThePinUntilConfirmedAndSaved() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "test_pin_seven_taps"
        val store = ParentPinStore(context, name)
        store.set("1234")
        var unlocked = false
        try {
            content { ParentGate(onDismiss = {}, onUnlock = { unlocked = true }, pinStore = store) }
            repeat(6) { compose.onNodeWithTag("pin_reset_label").performClick() }
            compose.onNodeWithTag("confirm_pin_reset").assertDoesNotExist()
            compose.onNodeWithTag("pin_reset_label").performClick()
            compose.onNodeWithTag("confirm_pin_reset").assertIsDisplayed()
            assertTrue(store.verify("1234"))
            compose.onNodeWithTag("confirm_pin_reset").performClick()
            compose.onNodeWithTag("parent_gate_answer").performTextInput("5678")
            compose.onNodeWithTag("pin_confirmation").performTextInput("5678")
            compose.onNodeWithTag("parent_gate_unlock").performClick()
            compose.waitUntil(5000) { unlocked }
            assertTrue(store.verify("5678"))
            assertFalse(store.verify("1234"))
        } finally { context.deleteSharedPreferences(name) }
    }

    @Test fun settingsContainDefaultVoiceImportAndDriveControls() {
        var voice by mutableStateOf(GeminiVoices.VOICE_AOEDE)
        var speed by mutableFloatStateOf(0.90f)
        var imports = 0
        content { SettingsScreen(AppLanguage.BOSNIAN, AppTheme.DARK, {}, {}, {}, {}, voice = voice,
            onVoiceChange = { voice = it }, playbackSpeed = speed, onPlaybackSpeedChange = { speed = it },
            onImport = { imports++ }, cloudSyncState = CloudSyncState(configured = true)) }
        compose.onNodeWithTag("voice_provider_GEMINI").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("voice_provider_OPENAI").performClick()
        compose.runOnIdle { assertEquals(AiProvider.OPENAI, voice.provider) }
        compose.onNodeWithTag("playback_speed_75").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0.75f, speed, 0.001f) }
        compose.onNodeWithTag("google_sync_card").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("google_sign_in").assertIsDisplayed()
        compose.onNodeWithTag("open_story_import").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, imports) }
        compose.onNodeWithTag("drive_backup").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("drive_restore").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("story_provider_GEMINI").assertDoesNotExist()
        compose.onNodeWithText("Offline glas uređaja").assertDoesNotExist()
    }

    @Test fun creatorChoiceLivesOnTheStorySetupScreenAndDoesNotChangeVoice() {
        var provider by mutableStateOf(AiProvider.GEMINI)
        var generatedBy: AiProvider? = null
        content { StorySetupScreen(StoryCreationConfig(), {}, onGenerateClick = { generatedBy = provider },
            onBackClick = {}, isNightMode = true, storyProvider = provider, onStoryProviderChange = { provider = it }) }
        compose.onNodeWithTag("story_provider_OPENAI").performScrollTo().performClick()
        compose.onNodeWithTag("generate_story_button").performClick()
        compose.runOnIdle { assertEquals(AiProvider.OPENAI, generatedBy) }
    }

    @Test fun importUsesFamilyCategoryByDefaultAndLetsTheParentChooseAnotherCategory() {
        var state by mutableStateOf(StoryImportState(conversations = listOf(ChatConversation("test", "Zejd", "PRIPOVJEDAČ: Hana ima ključ.", 1)), title = "Zejd"))
        var selectedCategory: String? = null
        content { StoryImportScreen(state, AiProvider.GEMINI, {}, {}, {}, onConfig = { state = state.copy(config = it) },
            onProvider = {}, onStart = { selectedCategory = state.config.category.id }, onBack = {}) }
        compose.onNodeWithTag("import_category_picker").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("📚 Naša porodična priča").assertIsDisplayed()
        compose.onNodeWithTag("import_category_picker").performClick()
        compose.onNodeWithTag("import_category_magic").performClick()
        compose.onNodeWithTag("start_story_import").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("magic", selectedCategory) }
    }

    @Test fun importedSeriesOffersAutomaticContinuationAndEditableMemory() {
        val story = ActiveStory(title = "Porodična priča", config = StoryCreationConfig(category = StoryCategories.family),
            chapters = listOf(Chapter(1, "Početak", "Hana je našla ključ.", "Dobrota.")), chronicle = "Hana je sestra.", isSerial = true)
        var continuations = 0
        var savedMemory: String? = null
        content { StoryReaderScreen(story, TtsState.IDLE, AudioEngineType.GEMINI_NATURAL, GeminiVoices.VOICE_AOEDE,
            null, false, true, { _, _, _ -> }, MusicSettings(), NarrationProgress(), {}, {}, {}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {},
            true, {}, {}, onContinueSerial = { continuations++ }, onSaveChronicle = { savedMemory = it }) }
        compose.onNodeWithTag("story_reader_content").performScrollToIndex(1)
        compose.onNodeWithTag("continue_serial_story").performClick()
        compose.runOnIdle { assertEquals(1, continuations) }
        compose.onNodeWithTag("custom_idea_toggle").assertDoesNotExist()
        compose.onNodeWithTag("open_chronicle").performClick()
        compose.onNodeWithTag("chronicle_editor").performTextReplacement("Hana je Zejdova sestra.")
        compose.onNodeWithText("Sačuvaj", substring = false).performClick()
        compose.runOnIdle { assertEquals("Hana je Zejdova sestra.", savedMemory) }
    }
}
