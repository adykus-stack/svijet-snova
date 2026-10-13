package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.*
import com.example.ui.components.DreamTopBar
import com.example.ui.components.DreamBackdrop
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.StoryImportScreen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.generating.GeneratingScreen
import com.example.ui.home.HomeScreen
import com.example.ui.intro.IntroScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.reader.StoryReaderScreen
import com.example.ui.setup.StorySetupScreen
import com.example.ui.theme.SvijetSnovaTheme
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.parents.ParentGate
import com.example.ui.parents.ParentSettingsScreen
import android.view.WindowManager
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val isNightMode by viewModel.isNightMode.collectAsStateWithLifecycle()
            val appTheme by viewModel.appTheme.collectAsStateWithLifecycle()
            DisposableEffect(appTheme) {
                val barStyle = if (appTheme.isDark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose { }
            }
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
            val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
            val cloudSync by viewModel.cloudSync.collectAsStateWithLifecycle()
            val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
            val parentKeys by viewModel.parentKeys.collectAsStateWithLifecycle()
            val storyProvider by viewModel.storyProvider.collectAsStateWithLifecycle()
            val storyImport by viewModel.storyImport.collectAsStateWithLifecycle()
            val generationDetail by viewModel.generationDetail.collectAsStateWithLifecycle()
            val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
            val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()
            val restorePreview by viewModel.restorePreview.collectAsStateWithLifecycle()
            val libraryCategory by viewModel.libraryCategory.collectAsStateWithLifecycle()
            val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::loadImport) }
            val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(viewModel::writeBackup) }
            val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(viewModel::previewBackup) }
            var showParentGate by remember { mutableStateOf(false) }
            var showChangePin by remember { mutableStateOf(false) }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, currentScreen) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        showParentGate = false
                        showChangePin = false
                        if (viewModel.currentScreen.value == AppScreen.Parents) viewModel.navigateBack()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            DisposableEffect(currentScreen, showParentGate, showChangePin) {
                val protectsKeys = currentScreen == AppScreen.Parents || showParentGate || showChangePin
                if (protectsKeys) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                onDispose { if (protectsKeys) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
            }

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
                SvijetSnovaTheme(appTheme = appTheme) {
                    if (showParentGate) ParentGate(onDismiss = { showParentGate = false },
                        onUnlock = { showParentGate = false; viewModel.openParentSettings() })
                    if (showChangePin) ParentGate(onDismiss = { showChangePin = false },
                        onUnlock = { showChangePin = false }, changePin = true)
                    restorePreview?.let { items ->
                        AlertDialog(onDismissRequest = viewModel::cancelRestore,
                            title = { Text(appLanguage.text("Vrati sigurnosnu kopiju?", "Sicherung wiederherstellen?")) },
                            text = { Text(appLanguage.text("Kopija sadrži ${items.size} priča. Dodaj ih u biblioteku; postojeće priče ostaju.",
                                "Die Sicherung enthält ${items.size} Geschichten. Zur Bibliothek hinzufügen; bestehende Geschichten bleiben erhalten.")) },
                            confirmButton = { TextButton(enabled = !backupBusy, onClick = viewModel::confirmRestore) { Text(appLanguage.text("Vrati", "Wiederherstellen")) } },
                            dismissButton = { TextButton(enabled = !backupBusy, onClick = viewModel::cancelRestore) { Text(appLanguage.text("Odustani", "Abbrechen")) } })
                    }
                    Scaffold(
                        topBar = {
                            when (currentScreen) {
                                AppScreen.Home -> DreamTopBar(
                                    isNightMode = isNightMode, savedCount = savedStories.size,
                                    onOpenSettings = { viewModel.navigateTo(AppScreen.Settings) },
                                    onOpenLibrary = { viewModel.openLibrary() },
                                    onReplayIntro = { viewModel.navigateTo(AppScreen.Intro) })
                                AppScreen.Intro -> Row(Modifier.fillMaxWidth().statusBarsPadding(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { viewModel.navigateTo(AppScreen.Settings) }, modifier = Modifier.testTag("open_settings")) {
                                        Icon(Icons.Default.Settings, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(appLanguage.text("Postavke", "Einstellungen"))
                                    }
                                }
                                else -> Unit
                            }
                        },
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets.safeDrawing,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .consumeWindowInsets(innerPadding)
                        ) {
                            DreamBackdrop(appTheme)
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    // Do not keep a key-entry screen visible during a fading transition.
                                    if (initialState == AppScreen.Parents || targetState == AppScreen.Parents) {
                                        EnterTransition.None togetherWith ExitTransition.None
                                    } else {
                                        fadeIn(animationSpec = tween(350)) togetherWith
                                                fadeOut(animationSpec = tween(300))
                                    }
                                },
                                label = "screen_transition"
                            ) { screen ->
                                when (screen) {
                                    AppScreen.Settings -> SettingsScreen(
                                        language = appLanguage, theme = appTheme,
                                        onLanguageChange = viewModel::setAppLanguage,
                                        onThemeChange = viewModel::setAppTheme,
                                        onOpenParents = { showParentGate = true },
                                        onBack = { viewModel.navigateBack() },
                                        voice = currentVoice, onVoiceChange = viewModel::selectVoice,
                                        playbackSpeed = playbackSpeed, onPlaybackSpeedChange = viewModel::setPlaybackSpeed,
                                        onImport = { viewModel.navigateTo(AppScreen.Import) },
                                        onBackup = { backupPicker.launch("Svijet-snova-${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())}.json") },
                                        onRestore = { restorePicker.launch(arrayOf("application/json", "application/octet-stream")) },
                                        backupBusy = backupBusy, backupMessage = backupMessage,
                                        cloudSyncState = cloudSync,
                                        onGoogleSignIn = { viewModel.signInWithGoogle(this@MainActivity) },
                                        onGoogleSync = viewModel::syncGoogleNow,
                                        onGoogleSignOut = viewModel::signOutGoogle)
                                    AppScreen.Import -> StoryImportScreen(state = storyImport, provider = storyProvider,
                                        onPick = { importPicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/json", "application/octet-stream")) },
                                        onSelect = viewModel::selectImportConversation, onTitle = viewModel::setImportTitle,
                                        onConfig = viewModel::setImportConfig, onProvider = viewModel::setStoryProvider,
                                        onStart = viewModel::startImport, onBack = { viewModel.navigateBack() })
                                    AppScreen.Parents -> ParentSettingsScreen(
                                        keyState = parentKeys,
                                        onCheckAndSave = viewModel::checkAndSaveKey, onRemove = viewModel::removeKey,
                                        onCancelCheck = viewModel::cancelKeyCheck,
                                        onChangePin = { showChangePin = true }, onBack = { viewModel.navigateBack() })
                                    AppScreen.Intro -> {
                                        IntroScreen(
                                            onStartAdventure = { viewModel.navigateTo(AppScreen.Home) },
                                            onSkip = { viewModel.navigateTo(AppScreen.Home) }
                                        )
                                    }

                                    AppScreen.Home -> {
                                            HomeScreen(
                                                selectedCategory = creationConfig.category,
                                                onSelectCategory = { viewModel.selectCategory(it) },
                                                onProceedToSetup = { viewModel.navigateTo(AppScreen.Setup) },
                                                activeStory = activeStory,
                                                onResumeActiveStory = { viewModel.navigateTo(AppScreen.Reader) },
                                                isNightMode = isNightMode,
                                                categoryHasStories = savedStories.any { it.categoryId == creationConfig.category.id },
                                                onOpenCategoryStories = { viewModel.openLibrary(creationConfig.category.id) }
                                            )
                                    }

                                    AppScreen.Setup -> {
                                        StorySetupScreen(
                                            config = creationConfig,
                                            onConfigChange = { viewModel.updateConfig(it) },
                                            onGenerateClick = { viewModel.startGeneratingStory() },
                                            onBackClick = { viewModel.navigateBack() },
                                            onOpenSettings = { viewModel.navigateTo(AppScreen.Settings) },
                                            isNightMode = isNightMode,
                                            storyProvider = storyProvider, onStoryProviderChange = viewModel::setStoryProvider
                                        )
                                    }

                                    AppScreen.Generating -> {
                                        GeneratingScreen(
                                            categoryEmoji = creationConfig.category.emoji,
                                            categoryTitle = appLanguage.text(creationConfig.category.titleBs, creationConfig.category.titleDe),
                                            isNightMode = isNightMode,
                                            onCancel = viewModel::cancelGeneration,
                                            detail = generationDetail
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
                                                onOpenSettings = { viewModel.navigateTo(AppScreen.Settings) },
                                                onBackClick = { viewModel.navigateBack() },
                                                onContinueSerial = viewModel::continueSerial,
                                                onNextChapterGuidanceChange = viewModel::updateNextChapterGuidance,
                                                onSaveChronicle = viewModel::saveChronicle,
                                                storyProvider = storyProvider, onStoryProviderChange = viewModel::setStoryProvider,
                                                playbackSpeed = playbackSpeed,
                                                onPlaybackSpeedChange = viewModel::setPlaybackSpeed
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
                                            onOpenSettings = { viewModel.navigateTo(AppScreen.Settings) },
                                            isNightMode = isNightMode,
                                            initialCategory = libraryCategory
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
