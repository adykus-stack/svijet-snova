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
import com.example.data.security.*
import com.example.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.ui.theme.AppTheme
import android.net.Uri
import com.example.data.importing.*

sealed interface AppScreen {
    data object Intro : AppScreen
    data object Home : AppScreen
    data object Setup : AppScreen
    data object Generating : AppScreen
    data object Reader : AppScreen
    data object Library : AppScreen
    data object Parents : AppScreen
    data object Settings : AppScreen
    data object Import : AppScreen
}

data class ParentKeyState(
    val saved: Set<AiProvider> = emptySet(),
    val busy: Boolean = false,
    val messageBs: String = "",
    val messageDe: String = "",
    val savedRevision: Int = 0
)

data class StoryImportState(val conversations: List<ChatConversation> = emptyList(), val selectedIndex: Int = 0,
    val title: String = "", val config: StoryCreationConfig = StoryCreationConfig(category = StoryCategories.family),
    val loading: Boolean = false) {
    val selected: ChatConversation? get() = conversations.getOrNull(selectedIndex)
}

class MainViewModel @JvmOverloads constructor(application: Application,
    private val keys: AiKeyStore = AiKeyStore(application),
    private val validator: ApiKeyValidator = ApiKeyValidator()
) : AndroidViewModel(application) {
    private val aiPreferences = AiPreferences(application)
    private var keyJob: Job? = null
    private val _parentKeys = MutableStateFlow(ParentKeyState())
    val parentKeys = _parentKeys.asStateFlow()
    private val _storyProvider = MutableStateFlow(aiPreferences.storyProvider())
    val storyProvider = _storyProvider.asStateFlow()
    private val checkpoint = ImportCheckpoint(application)
    private var documentJob: Job? = null
    private val _storyImport = MutableStateFlow(StoryImportState())
    val storyImport = _storyImport.asStateFlow()
    private val _generationDetail = MutableStateFlow("")
    val generationDetail = _generationDetail.asStateFlow()
    private val _backupBusy = MutableStateFlow(false)
    val backupBusy = _backupBusy.asStateFlow()
    private val _backupMessage = MutableStateFlow("")
    val backupMessage = _backupMessage.asStateFlow()
    private val _restorePreview = MutableStateFlow<List<StoryEntity>?>(null)
    val restorePreview = _restorePreview.asStateFlow()
    private val _libraryCategory = MutableStateFlow<String?>(null)
    val libraryCategory = _libraryCategory.asStateFlow()
    fun openLibrary(category: String? = null) {
        _libraryCategory.value = category
        navigateTo(AppScreen.Library)
    }

    fun loadImport(uri: Uri) {
        if (_storyImport.value.loading || _backupBusy.value) return
        _storyImport.value = _storyImport.value.copy(loading = true)
        documentJob = viewModelScope.launch {
            try {
                val conversations = withContext(Dispatchers.IO) {
                    checkNotNull(getApplication<Application>().contentResolver.openInputStream(uri)).use { ChatGptImport.read(it) }
                }
                _storyImport.value = _storyImport.value.copy(conversations = conversations, selectedIndex = 0,
                    title = conversations.first().title.take(200), loading = false)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _storyImport.value = _storyImport.value.copy(loading = false)
                _errorMessage.value = e.message ?: _appLanguage.value.text("Izvoz nije otvoren. Izaberi ChatGPT ZIP ili conversations.json.",
                    "Export konnte nicht geöffnet werden. Wähle ChatGPT-ZIP oder conversations.json.")
            }
        }
    }

    fun selectImportConversation(index: Int) {
        val selected = _storyImport.value.conversations.getOrNull(index) ?: return
        _storyImport.value = _storyImport.value.copy(selectedIndex = index, title = selected.title.take(200))
    }
    fun setImportTitle(title: String) { _storyImport.value = _storyImport.value.copy(title = title.take(200)) }
    fun setImportConfig(config: StoryCreationConfig) { _storyImport.value = _storyImport.value.copy(config = config) }

    fun startImport() {
        if (_isGenerating.value || _storyImport.value.loading) return
        val state = _storyImport.value
        val conversation = state.selected ?: return
        if (state.title.isBlank()) return
        val provider = _storyProvider.value
        _creationConfig.value = state.config
        runGeneration(AppScreen.Import) {
            try {
                val story = withContext(Dispatchers.IO) {
                    repository.importConversation(conversation, state.title.trim(), state.config, provider, checkpoint) { done, total, stage ->
                        _generationDetail.value = _appLanguage.value.text("Uvoz: $done / $total — $stage", "Import: $done / $total — $stage")
                    }
                }
                coroutineContext.ensureActive()
                _activeStory.value = story
                _creationConfig.value = story.config
                _storyImport.value = StoryImportState()
                _currentScreen.value = AppScreen.Reader
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _errorMessage.value = (e.message ?: "Uvoz nije uspio / Import fehlgeschlagen") + _appLanguage.value.text(
                    "\nObrađeni dijelovi kronike su sačuvani. Ponovo pokreni uvoz istog razgovora da nastaviš.",
                    "\nVerarbeitete Teile sind gespeichert. Starte denselben Import erneut, um fortzufahren.")
                _currentScreen.value = AppScreen.Import
            }
        }
    }

    fun continueSerial() {
        val current = _activeStory.value ?: return
        if (_isGenerating.value || !current.isSerial) return
        runGeneration(AppScreen.Reader) {
            val updated = repository.continueSerial(current)
            coroutineContext.ensureActive()
            _activeStory.value = updated
            _currentScreen.value = AppScreen.Reader
        }
    }

    fun saveChronicle(text: String) {
        val current = _activeStory.value ?: return
        if (!current.isSerial || _isGenerating.value || text.isBlank() || text.length > 22_000) return
        viewModelScope.launch {
            try {
                val updated = current.copy(chronicle = text)
                val id = repository.saveStory(updated)
                _activeStory.value = updated.copy(id = id, isSaved = true)
            } catch (_: Exception) { _errorMessage.value = _appLanguage.value.text("Kronika nije spremljena.", "Chronik konnte nicht gespeichert werden.") }
        }
    }

    fun writeBackup(uri: Uri) = documentOperation {
        val backup = repository.backup()
        withContext(Dispatchers.IO) {
            checkNotNull(getApplication<Application>().contentResolver.openOutputStream(uri, "wt")).use { it.write(backup.toByteArray()) }
        }
        _backupMessage.value = _appLanguage.value.text("Sigurnosna kopija je spremljena na izabranu lokaciju.", "Sicherung am gewählten Ort gespeichert.")
    }
    fun previewBackup(uri: Uri) = documentOperation {
        _restorePreview.value = withContext(Dispatchers.IO) {
            checkNotNull(getApplication<Application>().contentResolver.openInputStream(uri)).use {
                StoryBackup.decode(ChatGptImport.readLimited(it).toString(Charsets.UTF_8))
            }
        }
    }
    fun cancelRestore() { _restorePreview.value = null }
    fun confirmRestore() {
        val items = _restorePreview.value ?: return
        documentOperation {
            val added = AppDatabase.getDatabase(getApplication()).storyDao().restoreStories(items)
            _restorePreview.value = null
            _backupMessage.value = _appLanguage.value.text("Dodano priča: $added. Postojeće priče su sačuvane.",
                "$added Geschichten hinzugefügt. Bestehende Geschichten bleiben erhalten.")
        }
    }
    private fun documentOperation(operation: suspend () -> Unit) {
        if (_backupBusy.value || _storyImport.value.loading) return
        _backupBusy.value = true
        _backupMessage.value = ""
        documentJob = viewModelScope.launch {
            try { operation() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _backupMessage.value = _appLanguage.value.text(
                "Kopiranje nije uspjelo. Provjeri Drive, internet i izabranu datoteku pa pokušaj ponovo.",
                "Sicherung fehlgeschlagen. Prüfe Drive, Internet und Datei und versuche es erneut.") }
            finally { _backupBusy.value = false }
        }
    }

    fun setStoryProvider(provider: AiProvider) {
        aiPreferences.setStoryProvider(provider)
        _storyProvider.value = provider
    }

    fun openParentSettings() {
        navigateTo(AppScreen.Parents)
        keyJob?.cancel()
        _parentKeys.value = _parentKeys.value.copy(busy = true, messageBs = "", messageDe = "")
        keyJob = viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                AiProvider.entries.filter { runCatching { keys.read(it).isNotBlank() }.getOrDefault(false) }.toSet()
            }
            _parentKeys.value = _parentKeys.value.copy(saved = saved, busy = false)
        }
    }

    fun cancelKeyCheck() {
        keyJob?.cancel()
        keyJob = null
        _parentKeys.value = _parentKeys.value.copy(busy = false, messageBs = "", messageDe = "")
    }

    fun checkAndSaveKey(provider: AiProvider, candidate: String) {
        if (_parentKeys.value.busy) return
        _parentKeys.value = _parentKeys.value.copy(busy = true, messageBs = "", messageDe = "")
        keyJob = viewModelScope.launch {
            try {
                val key = withContext(Dispatchers.IO) { candidate.trim().ifEmpty { keys.read(provider) } }
                validator.validate(provider, key)
                withContext(Dispatchers.IO) {
                    coroutineContext.ensureActive()
                    keys.save(provider, key)
                }
                _parentKeys.value = _parentKeys.value.copy(
                    saved = _parentKeys.value.saved + provider, busy = false,
                    savedRevision = _parentKeys.value.savedRevision + 1,
                    messageBs = "${provider.label} ključ je provjeren i spremljen na ovaj telefon. Provjera ne potvrđuje saldo niti limit za priče i glasove.",
                    messageDe = "${provider.label}-Schlüssel geprüft und auf diesem Telefon gespeichert. Die Prüfung bestätigt weder Guthaben noch Limits für Geschichten und Stimmen."
                )
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val status = (e as? AiRequestException)?.status
                val messages = when {
                    e is IllegalArgumentException -> "Zalijepi cijeli API ključ bez razmaka." to "Füge den vollständigen API-Schlüssel ohne Leerzeichen ein."
                    status == 400 || status == 401 || status == 403 -> "Ključ nije prihvaćen. Provjeri ključ i izabranog pružaoca." to "Schlüssel nicht akzeptiert. Prüfe Schlüssel und Anbieter."
                    status == 429 -> "Previše zahtjeva ili dostignut API limit. Pokušaj kasnije i provjeri svoj račun." to "Zu viele Anfragen oder API-Limit erreicht. Versuche es später und prüfe dein Konto."
                    status != null -> "Servis nije dostupan za ovaj ključ (HTTP $status). Postojeći ključ nije promijenjen." to "Dienst für diesen Schlüssel nicht verfügbar (HTTP $status). Der bisherige Schlüssel wurde nicht geändert."
                    else -> "Provjera ili spremanje nije uspjelo. Provjeri internet i pokušaj ponovo. Postojeći ključ nije promijenjen." to "Prüfung oder Speicherung fehlgeschlagen. Prüfe die Internetverbindung und versuche es erneut. Der bisherige Schlüssel wurde nicht geändert."
                }
                _parentKeys.value = _parentKeys.value.copy(busy = false, messageBs = messages.first, messageDe = messages.second)
            }
        }
    }

    fun removeKey(provider: AiProvider) {
        if (_parentKeys.value.busy) return
        audioPlayer.stop()
        _parentKeys.value = _parentKeys.value.copy(busy = true)
        keyJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { keys.remove(provider) }
                _parentKeys.value = _parentKeys.value.copy(saved = _parentKeys.value.saved - provider, busy = false,
                    savedRevision = _parentKeys.value.savedRevision + 1,
                    messageBs = "${provider.label} ključ je uklonjen s ovog telefona. Nije poništen kod pružaoca.",
                    messageDe = "${provider.label}-Schlüssel von diesem Telefon entfernt. Beim Anbieter wurde er nicht widerrufen.")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _parentKeys.value = _parentKeys.value.copy(busy = false,
                    messageBs = "Ključ nije uklonjen. Pokušaj ponovo.", messageDe = "Schlüssel nicht entfernt. Versuche es erneut.")
            }
        }
    }
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
        storyDao = AppDatabase.getDatabase(application).storyDao(),
        geminiService = GeminiService(textProvider = aiPreferences::storyProvider,
            openAiService = OpenAiService({ keys.read(AiProvider.OPENAI) }),
            apiKeyProvider = { keys.read(AiProvider.GEMINI) })
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

    // Intentionally session-only: every cold start begins with the dark theme.
    private val _appTheme = MutableStateFlow(AppTheme.DARK)
    val appTheme = _appTheme.asStateFlow()
    private val _isNightMode = MutableStateFlow(true)
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
        if (_currentScreen.value == AppScreen.Parents) cancelKeyCheck()
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

    fun setAppTheme(theme: AppTheme) {
        _appTheme.value = theme
        _isNightMode.value = theme.isDark
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
            }.onFailure { err ->
                _errorMessage.value = err.message ?: _appLanguage.value.text("Prevod nije uspio.", "Übersetzung fehlgeschlagen.")
                _currentScreen.value = AppScreen.Reader
            }
        }
    }

    private fun runGeneration(returnScreen: AppScreen, operation: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
        generationReturnScreen = returnScreen
        _isGenerating.value = true
        _errorMessage.value = null
        _generationDetail.value = ""
        audioPlayer.stop()
        // Generating is a temporary state, not a screen to revisit using Back.
        _currentScreen.value = AppScreen.Generating
        val task = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                operation()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: _appLanguage.value.text(
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
        _generationDetail.value = ""
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
        if (!voiceKeyAvailable(forceVoice ?: currentVoice.value)) return
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
        if (!voiceKeyAvailable(voice)) return
        audioPlayer.previewVoice(voice, langCode)
    }

    private fun voiceKeyAvailable(voice: GeminiVoice): Boolean {
        if (voice.isOfflineDevice) return true
        if (runCatching { keys.read(voice.provider).isNotBlank() }.getOrDefault(false)) return true
        _errorMessage.value = _appLanguage.value.text(
            "Dodaj ${voice.provider.label} API ključ u roditeljskom nadzoru za ovaj glas.",
            "Füge unter Elternkontrolle einen ${voice.provider.label}-API-Schlüssel für diese Stimme hinzu.")
        return false
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
