package com.example

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppLanguagePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLanguageAndGenerationTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private lateinit var application: Application
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        application = ApplicationProvider.getApplicationContext()
        application.getSharedPreferences("app_settings", 0).edit().clear().commit()
        viewModel = MainViewModel(application)
        store.put("main", viewModel)
    }

    @After
    fun tearDown() {
        store.clear()
        dispatcher.scheduler.runCurrent()
        Dispatchers.resetMain()
    }

    @Test
    fun appLanguageIsPersistedWithoutChangingStoryConfiguration() {
        val config = StoryCreationConfig(
            language = StoryLanguage.BOSNIAN,
            heroName = "Hana",
            ageGroup = AgeGroupId.AGE_3_5,
            themeOrItem = "Čarobni fenjer"
        )
        viewModel.updateConfig(config)
        viewModel.setAppLanguage(AppLanguage.GERMAN)
        assertEquals(AppLanguage.GERMAN, viewModel.appLanguage.value)
        assertEquals(AppLanguage.GERMAN, AppLanguagePreferences(application).get())
        assertEquals(config, viewModel.creationConfig.value)
        assertEquals("Deutsch", AppLanguage.GERMAN.displayName)
        assertEquals("Hallo", AppLanguage.GERMAN.text("Zdravo", "Hallo"))
        viewModel.setAppLanguage(AppLanguage.BOSNIAN)
        assertEquals(AppLanguage.BOSNIAN, AppLanguagePreferences(application).get())
        assertEquals(config, viewModel.creationConfig.value)
    }

    @Test
    fun cancelNewStoryReturnsToSetupAndRetainsAllChoices() {
        val config = StoryCreationConfig(
            category = StoryCategories.allCategories.last(),
            heroName = "Hana",
            ageGroup = AgeGroupId.AGE_9_12,
            language = StoryLanguage.GERMAN,
            heroType = HeroType.ANIMAL,
            storyLength = StoryLength.LONG,
            themeOrItem = "Zvijezda",
            targetMoral = "Prijateljstvo"
        )
        viewModel.updateConfig(config)
        viewModel.navigateTo(AppScreen.Setup)
        viewModel.startGeneratingStory()
        assertTrue(viewModel.isGenerating.value)
        assertEquals(AppScreen.Generating, viewModel.currentScreen.value)
        viewModel.cancelGeneration()
        dispatcher.scheduler.runCurrent() // An obsolete job must never open a story later.
        assertFalse(viewModel.isGenerating.value)
        assertEquals(AppScreen.Setup, viewModel.currentScreen.value)
        assertEquals(config, viewModel.creationConfig.value)
        assertNull(viewModel.activeStory.value)
        assertNull(viewModel.errorMessage.value)
        viewModel.cancelGeneration() // Repeated clicks are harmless.
        assertEquals(AppScreen.Setup, viewModel.currentScreen.value)
    }

    @Test
    fun cancelContinuationEndingAndTranslationKeepsTheOriginalStory() {
        val original = ActiveStory(
            title = "Sačuvana priča",
            config = StoryCreationConfig(),
            chapters = listOf(Chapter(1, "Početak", "Bila jednom vila.", "Budi dobar.")),
            isSaved = true
        )
        viewModel.openStoryFromLibrary(original)
        val operations = listOf<() -> Unit>(
            { viewModel.continueStoryWithOption("Prati zvijezdu") },
            { viewModel.finishStory() },
            { viewModel.translateStory() }
        )
        for (operation in operations) {
            operation()
            assertEquals(AppScreen.Generating, viewModel.currentScreen.value)
            viewModel.cancelGeneration()
            dispatcher.scheduler.runCurrent()
            assertEquals(AppScreen.Reader, viewModel.currentScreen.value)
            assertEquals(original, viewModel.activeStory.value)
            assertEquals(original.config, viewModel.creationConfig.value)
            assertFalse(viewModel.isGenerating.value)
            assertNull(viewModel.errorMessage.value)
        }
    }

    @Test
    fun androidBackCancelsGenerationAndAllowsAnotherAttempt() {
        viewModel.navigateTo(AppScreen.Setup)
        viewModel.startGeneratingStory()
        assertTrue(viewModel.navigateBack())
        assertEquals(AppScreen.Setup, viewModel.currentScreen.value)
        viewModel.startGeneratingStory()
        assertTrue(viewModel.isGenerating.value)
        viewModel.cancelGeneration()
        dispatcher.scheduler.runCurrent()
        assertEquals(AppScreen.Setup, viewModel.currentScreen.value)
        assertFalse(viewModel.isGenerating.value)
        assertNull(viewModel.activeStory.value)
    }
}
