package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.components.DreamTopBar
import com.example.ui.generating.GeneratingScreen
import com.example.ui.home.HomeScreen
import com.example.ui.intro.IntroScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.reader.StoryReaderScreen
import com.example.ui.setup.StorySetupScreen
import com.example.ui.theme.DreamDayBg
import com.example.ui.theme.DreamNightBg
import com.example.ui.theme.SvijetSnovaTheme
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.localization.AppLanguageSelector

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val isNightMode by viewModel.isNightMode.collectAsStateWithLifecycle()
            val creationConfig by viewModel.creationConfig.collectAsStateWithLifecycle()
            val activeStory by viewModel.activeStory.collectAsStateWithLifecycle()
            val savedStories by viewModel.savedStories.collectAsStateWithLifecycle()
            val ttsState by viewModel.ttsState.collectAsStateWithLifecycle()
            val audioEngine by viewModel.audioEngine.collectAsStateWithLifecycle()
            val currentVoice by viewModel.currentVoice.collectAsStateWithLifecycle()
            val ttsErrorMessage by viewModel.ttsErrorMessage.collectAsStateWithLifecycle()
            val isPreviewingVoice by viewModel.isPreviewingVoice.collectAsStateWithLifecycle()
            val musicSettings by viewModel.musicSettings.collectAsStateWithLifecycle()
            val narrationProgress by viewModel.narrationProgress.collectAsStateWithLifecycle()
            val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(errorMessage) {
                errorMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearError()
                }
            }

            // Proper back navigation handling
            BackHandler(enabled = currentScreen != AppScreen.Home && currentScreen != AppScreen.Intro) {
                viewModel.navigateBack()
            }

            CompositionLocalProvider(LocalAppLanguage provides appLanguage) {
                SvijetSnovaTheme(darkTheme = isNightMode) {
                    Scaffold(
                        topBar = { AppLanguageSelector(appLanguage, viewModel::setAppLanguage) },
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets.safeDrawing,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isNightMode) DreamNightBg else DreamDayBg)
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                        ) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(350)) togetherWith
                                            fadeOut(animationSpec = tween(300))
                                },
                                label = "screen_transition"
                            ) { screen ->
                                when (screen) {
                                    AppScreen.Intro -> {
                                        IntroScreen(
                                            onStartAdventure = { viewModel.navigateTo(AppScreen.Home) },
                                            onSkip = { viewModel.navigateTo(AppScreen.Home) }
                                        )
                                    }

                                    AppScreen.Home -> {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            DreamTopBar(
                                                isNightMode = isNightMode,
                                                savedCount = savedStories.size,
                                                onToggleNightMode = { viewModel.toggleNightMode() },
                                                onOpenLibrary = { viewModel.navigateTo(AppScreen.Library) },
                                                onReplayIntro = { viewModel.navigateTo(AppScreen.Intro) }
                                            )

                                            HomeScreen(
                                                selectedCategory = creationConfig.category,
                                                onSelectCategory = { viewModel.selectCategory(it) },
                                                onProceedToSetup = { viewModel.navigateTo(AppScreen.Setup) },
                                                activeStory = activeStory,
                                                onResumeActiveStory = { viewModel.navigateTo(AppScreen.Reader) },
                                                isNightMode = isNightMode
                                            )
                                        }
                                    }

                                    AppScreen.Setup -> {
                                        StorySetupScreen(
                                            config = creationConfig,
                                            onConfigChange = { viewModel.updateConfig(it) },
                                            onGenerateClick = { viewModel.startGeneratingStory() },
                                            onBackClick = { viewModel.navigateBack() },
                                            isNightMode = isNightMode
                                        )
                                    }

                                    AppScreen.Generating -> {
                                        GeneratingScreen(
                                            categoryEmoji = creationConfig.category.emoji,
                                            categoryTitle = appLanguage.text(creationConfig.category.titleBs, creationConfig.category.titleDe),
                                            isNightMode = isNightMode,
                                            onCancel = viewModel::cancelGeneration
                                        )
                                    }

                                    AppScreen.Reader -> {
                                        activeStory?.let { story ->
                                            StoryReaderScreen(
                                                story = story,
                                                ttsState = ttsState,
                                                audioEngine = audioEngine,
                                                currentVoice = currentVoice,
                                                ttsErrorMessage = ttsErrorMessage,
                                                isPreviewingVoice = isPreviewingVoice,
                                                hasUserSelectedVoiceEver = viewModel.hasUserSelectedVoiceEver(),
                                                onPlayTts = { text, lang, mood -> viewModel.playTts(text, lang, mood) },
                                                musicSettings = musicSettings,
                                                narrationProgress = narrationProgress,
                                                onMusicEnabledChange = viewModel::setMusicEnabled,
                                                onMusicVolumeChange = viewModel::setMusicVolume,
                                                onMusicSelectionChange = viewModel::selectMusic,
                                                onPauseTts = { viewModel.pauseTts() },
                                                onResumeTts = { viewModel.resumeTts() },
                                                onStopTts = { viewModel.stopTts() },
                                                onSelectVoice = { voice -> viewModel.selectVoice(voice) },
                                                onPreviewVoice = { voice, lang -> viewModel.previewVoice(voice, lang) },
                                                onStopPreviewVoice = { viewModel.stopPreviewVoice() },
                                                onSaveStory = { viewModel.saveCurrentStory() },
                                                onNewStoryClick = { viewModel.navigateTo(AppScreen.Home) },
                                                onContinueStoryWithOption = { choice ->
                                                    viewModel.continueStoryWithOption(choice)
                                                },
                                                onFinishStoryClick = { viewModel.finishStory() },
                                                onTranslateStoryClick = { viewModel.translateStory() },
                                                isNightMode = isNightMode,
                                                onToggleNightMode = { viewModel.toggleNightMode() },
                                                onBackClick = { viewModel.navigateBack() }
                                            )
                                        } ?: run {
                                            // If no active story, return to home
                                            LaunchedEffect(Unit) {
                                                viewModel.navigateTo(AppScreen.Home)
                                            }
                                        }
                                    }

                                    AppScreen.Library -> {
                                        LibraryScreen(
                                            savedStories = savedStories,
                                            onSelectStory = { story -> viewModel.openStoryFromLibrary(story) },
                                            onDeleteStory = { id -> viewModel.deleteStory(id) },
                                            onBackClick = { viewModel.navigateBack() },
                                            onCreateNewStory = { viewModel.navigateTo(AppScreen.Home) },
                                            isNightMode = isNightMode
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
