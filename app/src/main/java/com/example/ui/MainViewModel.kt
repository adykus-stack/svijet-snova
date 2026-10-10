package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.StoryEntity
import com.example.data.model.*
import com.example.data.repository.StoryRepository
import com.example.tts.AudioEngineType
import com.example.tts.StoryAudioPlayer
import com.example.tts.TtsState
import com.example.tts.MusicSettings
import com.example.tts.NarrationProgress
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppLanguagePreferences

sealed interface AppScreen {
    data object Intro : AppScreen
    data object Home : AppScreen
    data object Setup : AppScreen
    data object Generating : AppScreen
    data object Reader : AppScreen
    data object Library : AppScreen
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val languagePreferences = AppLanguagePreferences(application)
    private val _appLanguage = MutableStateFlow(languagePreferences.get())
    val appLanguage = _appLanguage.asStateFlow()
    private var generationJob: Job? = null
    private var generationReturnScreen: AppScreen = AppScreen.Setup

    fun setAppLanguage(language: AppLanguage) {
        languagePreferences.set(language)
        _appLanguage.value = language
    }

    private val repository = StoryRepository(
        storyDao = AppDatabase.getDatabase(application).storyDao()
    )

    val audioPlayer = StoryAudioPlayer(application)
    val ttsState: StateFlow<TtsState> = audioPlayer.ttsState
    val audioEngine: StateFlow<AudioEngineType> = audioPlayer.audioEngine
    val currentVoice: StateFlow<GeminiVoice> = audioPlayer.currentVoice
    val ttsErrorMessage: StateFlow<String?> = audioPlayer.errorMessage
    val isPreviewingVoice: StateFlow<Boolean> = audioPlayer.isPreviewing
    val musicSettings: StateFlow<MusicSettings> = audioPlayer.musicSettings
    val narrationProgress: StateFlow<NarrationProgress> = audioPlayer.progress

    fun setMusicEnabled(enabled: Boolean) = audioPlayer.setMusicEnabled(enabled)
    fun setMusicVolume(volume: Float) = audioPlayer.setMusicVolume(volume)
    fun selectMusic(mood: MusicMood?) = audioPlayer.selectMusic(mood)

    val savedStories: StateFlow<List<StoryEntity>> = repository.allSavedStories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Intro)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val backStack = mutableListOf<AppScreen>()

    private val _isNightMode = MutableStateFlow(false)
    val isNightMode: StateFlow<Boolean> = _isNightMode.asStateFlow()

    private val _creationConfig = MutableStateFlow(StoryCreationConfig())
    val creationConfig: StateFlow<StoryCreationConfig> = _creationConfig.asStateFlow()

    private val _activeStory = MutableStateFlow<ActiveStory?>(null)
    val activeStory: StateFlow<ActiveStory?> = _activeStory.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            if (_currentScreen.value == AppScreen.Reader) audioPlayer.stop()
            backStack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (_isGenerating.value) {
            cancelGeneration()
            return true
        }
        audioPlayer.stop()
        if (backStack.isNotEmpty()) {
            val prev = backStack.removeAt(backStack.size - 1)
            _currentScreen.value = prev
            return true
        } else if (_currentScreen.value != AppScreen.Home) {
            _currentScreen.value = AppScreen.Home
            return true
        }
        return false
    }

    fun toggleNightMode() {
        _isNightMode.value = !_isNightMode.value
    }

    fun selectCategory(category: StoryCategory) {
        _creationConfig.value = _creationConfig.value.copy(category = category)
    }

    fun updateConfig(config: StoryCreationConfig) {
        _creationConfig.value = config
    }

    fun startGeneratingStory() {
        if (_isGenerating.value) return
        val config = _creationConfig.value
        runGeneration(AppScreen.Setup) {
            val result = repository.generateFirstChapter(config)
            coroutineContext.ensureActive()
            result.onSuccess { story ->
                _activeStory.value = story
                _currentScreen.value = AppScreen.Reader
            }.onFailure { err ->
                _errorMessage.value = err.message ?: _appLanguage.value.text("Greška pri stvaranju priče", "Fehler beim Erstellen der Geschichte")
                _currentScreen.value = AppScreen.Setup
            }
        }
    }

    fun continueStoryWithOption(choice: String) {
        val current = _activeStory.value ?: return
        if (_isGenerating.value) return
        runGeneration(AppScreen.Reader) {
            val result = repository.continueStory(current, choice, isFinalChapter = false)
            coroutineContext.ensureActive()
            result.onSuccess { updated ->
                _activeStory.value = updated
                _currentScreen.value = AppScreen.Reader
            }.onFailure { err ->
                _errorMessage.value = err.message ?: _appLanguage.value.text("Greška pri nastavku priče", "Fehler beim Fortsetzen der Geschichte")
                _currentScreen.value = AppScreen.Reader
            }
        }
    }

    fun finishStory() {
        val current = _activeStory.value ?: return
        if (_isGenerating.value) return
        runGeneration(AppScreen.Reader) {
            val result = repository.continueStory(
                current,
                continuationChoice = "Završi priču toplim i sretnim krajem",
                isFinalChapter = true
            )
            coroutineContext.ensureActive()
            result.onSuccess { updated ->
                _activeStory.value = updated
                _currentScreen.value = AppScreen.Reader
            }.onFailure { err ->
                _errorMessage.value = err.message
                _currentScreen.value = AppScreen.Reader
            }
        }
    }

    fun translateStory() {
        val current = _activeStory.value ?: return
        if (_isGenerating.value) return
        val targetLang = if (current.config.language == StoryLanguage.BOSNIAN) StoryLanguage.GERMAN else StoryLanguage.BOSNIAN
        runGeneration(AppScreen.Reader) {
            val result = repository.translateStory(current, targetLang)
            coroutineContext.ensureActive()
            result.onSuccess { translated ->
                _activeStory.value = translated
                _creationConfig.value = translated.config
                _currentScreen.value = AppScreen.Reader
            }.onFailure {
                _currentScreen.value = AppScreen.Reader
            }
        }
    }

    private fun runGeneration(returnScreen: AppScreen, operation: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
        generationReturnScreen = returnScreen
        _isGenerating.value = true
        _errorMessage.value = null
        audioPlayer.stop()
        // Generating is a temporary state, not a screen to revisit using Back.
        _currentScreen.value = AppScreen.Generating
        val task = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                operation()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = _appLanguage.value.text(
                    "Stvaranje priče nije uspjelo. Pokušaj ponovo.",
                    "Die Geschichte konnte nicht erstellt werden. Bitte versuche es erneut."
                )
                _currentScreen.value = returnScreen
            } finally {
                if (generationJob == coroutineContext.job) {
                    _isGenerating.value = false
                    generationJob = null
                }
            }
        }
        generationJob = task
        task.start()
    }

    fun cancelGeneration() {
        if (!_isGenerating.value) return
        generationJob?.cancel()
        generationJob = null
        _isGenerating.value = false
        _errorMessage.value = null
        _currentScreen.value = generationReturnScreen
    }

    fun saveCurrentStory() {
        val current = _activeStory.value ?: return
        viewModelScope.launch {
            val savedId = repository.saveStory(current)
            _activeStory.value = current.copy(id = savedId, isSaved = true)
        }
    }

    fun deleteStory(id: Long) {
        viewModelScope.launch {
            repository.deleteStoryById(id)
            if (_activeStory.value?.id == id) {
                _activeStory.value = _activeStory.value?.copy(isSaved = false, id = 0L)
            }
        }
    }

    fun openStoryFromLibrary(story: ActiveStory) {
        audioPlayer.stop()
        _activeStory.value = story
        _creationConfig.value = story.config
        navigateTo(AppScreen.Reader)
    }

    fun playTts(text: String, langCode: String, mood: MusicMood, forceVoice: GeminiVoice? = null) {
        audioPlayer.playStory(text, langCode, forceVoice, mood)
    }

    fun pauseTts() {
        audioPlayer.pause()
    }

    fun resumeTts() {
        audioPlayer.resume()
    }

    fun stopTts() {
        audioPlayer.stop()
    }

    fun selectVoice(voice: GeminiVoice) {
        audioPlayer.selectVoice(voice)
    }

    fun previewVoice(voice: GeminiVoice, langCode: String) {
        audioPlayer.previewVoice(voice, langCode)
    }

    fun stopPreviewVoice() {
        audioPlayer.stopPreview()
    }

    fun hasUserSelectedVoiceEver(): Boolean {
        return audioPlayer.hasUserSelectedVoiceEver()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.destroy()
    }
}
