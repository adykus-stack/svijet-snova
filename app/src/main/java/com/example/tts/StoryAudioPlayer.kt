package com.example.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.model.*
import com.example.data.remote.GeminiService
import com.example.ui.localization.AppLanguagePreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class TtsState { IDLE, PREPARING, PLAYING, PAUSED, ERROR }
enum class AudioEngineType { GEMINI_NATURAL, OFFLINE_DEVICE }

class StoryAudioPlayer(
    private val context: Context,
    private val speechGenerator: (suspend (String, StoryLanguage, String) -> Result<ByteArray>)? = null
) : TextToSpeech.OnInitListener {
    private val prefs = context.getSharedPreferences("story_voice_prefs", Context.MODE_PRIVATE)
    private val appLanguagePreferences = AppLanguagePreferences(context)
    private val geminiService = GeminiService()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val music = BackgroundMusicPlayer(context, scope)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        scope.launch {
            if (change == AudioManager.AUDIOFOCUS_LOSS) stop()
            else if (change < 0) pause()
        }
    }
    private val ttsReady = CompletableDeferred<Boolean>()
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var mediaPlayer: MediaPlayer? = null
    private var speechJob: Job? = null
    private var offlineUtterance: Pair<String, CompletableDeferred<Boolean>>? = null
    private val paused = MutableStateFlow(false)

    private val _ttsState = MutableStateFlow(TtsState.IDLE)
    val ttsState = _ttsState.asStateFlow()
    private val _audioEngine = MutableStateFlow(AudioEngineType.GEMINI_NATURAL)
    val audioEngine = _audioEngine.asStateFlow()
    private val _currentVoice = MutableStateFlow(
        GeminiVoices.getById(prefs.getString("selected_voice_id", GeminiVoices.VOICE_AOEDE.id).orEmpty())
    )
    val currentVoice = _currentVoice.asStateFlow()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    private val _isPreviewing = MutableStateFlow(false)
    val isPreviewing = _isPreviewing.asStateFlow()
    private val _progress = MutableStateFlow(NarrationProgress())
    val progress = _progress.asStateFlow()
    private val _musicSettings = MutableStateFlow(MusicSettings(
        enabled = prefs.getBoolean("music_enabled", true),
        volume = prefs.getFloat("music_volume", 0.30f).coerceIn(0f, 0.60f),
        selectedMood = MusicMood.fromCode(prefs.getString("music_mood", null))
    ))
    val musicSettings = _musicSettings.asStateFlow()
    private val _automaticMood = MutableStateFlow(MusicMood.CALM)
    val automaticMood = _automaticMood.asStateFlow()
    internal val isBackgroundMusicPlaying: Boolean get() = music.isPlaying
    internal val backgroundMusicGain: Float get() = music.gain
    private val cacheDir = File(context.cacheDir, "gemini_tts_cache").apply { mkdirs() }

    private fun uiText(bs: String, de: String) = appLanguagePreferences.get().text(bs, de)

    override fun onInit(status: Int) {
        ttsReady.complete(status == TextToSpeech.SUCCESS)
        if (status != TextToSpeech.SUCCESS) return
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = completeOffline(utteranceId, true)
            override fun onStop(utteranceId: String?, interrupted: Boolean) = completeOffline(utteranceId, false)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = failOffline(utteranceId)
            override fun onError(utteranceId: String?, errorCode: Int) = failOffline(utteranceId)
        })
    }

    private fun completeOffline(id: String?, done: Boolean) {
        scope.launch { offlineUtterance?.takeIf { it.first == id }?.second?.complete(done) }
    }
    private fun failOffline(id: String?) {
        scope.launch {
            offlineUtterance?.takeIf { it.first == id }?.second
                ?.completeExceptionally(IOException(uiText("Glas uređaja nije dostupan.", "Die Gerätestimme ist nicht verfügbar.")))
        }
    }

    fun selectVoice(voice: GeminiVoice) {
        if (_currentVoice.value.id != voice.id) stop()
        _currentVoice.value = voice
        prefs.edit().putString("selected_voice_id", voice.id).putBoolean("has_selected_voice_ever", true).apply()
    }
    fun hasUserSelectedVoiceEver() = prefs.getBoolean("has_selected_voice_ever", false)
    fun setHasUserSelectedVoiceEver(value: Boolean) {
        prefs.edit().putBoolean("has_selected_voice_ever", value).apply()
    }
    fun clearError() { _errorMessage.value = null }

    fun setMusicEnabled(enabled: Boolean) = updateMusic(_musicSettings.value.copy(enabled = enabled))
    fun setMusicVolume(volume: Float) = updateMusic(_musicSettings.value.copy(volume = volume.coerceIn(0f, 0.60f)))
    fun selectMusic(mood: MusicMood?) = updateMusic(_musicSettings.value.copy(selectedMood = mood))
    private fun updateMusic(settings: MusicSettings) {
        _musicSettings.value = settings
        prefs.edit().putBoolean("music_enabled", settings.enabled).putFloat("music_volume", settings.volume)
            .putString("music_mood", settings.selectedMood?.code).apply()
        music.update(settings)
    }

    fun playText(text: String, langCode: String) = playStory(text, langCode)

    fun playStory(text: String, langCode: String, forceVoice: GeminiVoice? = null,
                  mood: MusicMood = MusicMood.CALM) {
        val segments = NarrationPlan.segments(text)
        stop()
        if (segments.isEmpty()) return
        val voice = forceVoice ?: _currentVoice.value
        _audioEngine.value = if (voice.isOfflineDevice) AudioEngineType.OFFLINE_DEVICE else AudioEngineType.GEMINI_NATURAL
        _automaticMood.value = mood
        startSession(preview = false) {
            var useOffline = voice.isOfflineDevice
            suspend fun prepare(index: Int): Clip {
                if (useOffline) return Clip(null)
                val clip = prepareClip(segments[index], langCode, voice)
                if (clip.file == null) useOffline = true
                return clip
            }
            // Only one paragraph is prefetched. Stop cancels both playback and that request.
            var prepared = async { prepare(0) }
            for (index in segments.indices) {
                val clip = prepared.await()
                ensureActive()
                val next = if (index < segments.lastIndex) async { prepare(index + 1) } else null
                awaitResumed()
                if (index == 0) music.start(mood, _musicSettings.value)
                _progress.value = NarrationProgress(index + 1, segments.size)
                music.setSpeaking(true)
                _ttsState.value = TtsState.PLAYING
                playClip(clip, segments[index], langCode)
                if (next != null) {
                    _progress.value = _progress.value.copy(inGap = true)
                    music.setSpeaking(false)
                    paragraphGap()
                    prepared = next
                }
            }
        }
    }

    private data class Clip(val file: File?, val error: String? = null)

    private suspend fun prepareClip(text: String, langCode: String, voice: GeminiVoice): Clip {
        val cacheFile = File(cacheDir, "story_${cacheKey(voice.id, langCode, text)}.wav")
        if (withContext(Dispatchers.IO) { validWav(cacheFile) }) return Clip(cacheFile)
        val language = StoryLanguage.fromCode(langCode)
        val result = speechGenerator?.invoke(text, language, voice.id)
            ?: geminiService.generateSpeech(text, language, voice.id)
        currentCoroutineContext().ensureActive()
        val bytes = result.getOrNull()
            ?: return Clip(null, result.exceptionOrNull()?.message)
        return try {
            withContext(Dispatchers.IO) {
                val temp = File.createTempFile("speech_", ".wav", cacheDir)
                try {
                    temp.writeBytes(bytes)
                    ensureActive()
                    check(validWav(temp)) { "Invalid WAV audio" }
                    check(temp.renameTo(cacheFile)) { "Could not store speech" }
                } finally { temp.delete() }
            }
            Clip(cacheFile)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { Clip(null, e.message) }
    }

    private fun validWav(file: File): Boolean = runCatching {
        if (file.length() <= 44) false else file.inputStream().use {
            val header = ByteArray(12)
            it.read(header) == 12 && String(header, 0, 4, Charsets.US_ASCII) == "RIFF"
                && String(header, 8, 4, Charsets.US_ASCII) == "WAVE"
        }
    }.getOrDefault(false)

    private suspend fun playClip(clip: Clip, text: String, langCode: String) {
        awaitResumed()
        if (clip.file == null) {
            _audioEngine.value = AudioEngineType.OFFLINE_DEVICE
            if (clip.error != null) {
                _errorMessage.value = uiText(
                    "Gemini glas nije dostupan (${clip.error}). Nastavljam glasom uređaja.",
                    "Die Gemini-Stimme ist nicht verfügbar (${clip.error}). Die Gerätestimme wird verwendet."
                )
            }
            playOffline(text, langCode)
        } else {
            _audioEngine.value = AudioEngineType.GEMINI_NATURAL
            try { playWav(clip.file) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _errorMessage.value = uiText("Zvuk nije moguće pokrenuti. Nastavljam glasom uređaja.",
                    "Der Ton konnte nicht abgespielt werden. Die Gerätestimme wird verwendet.")
                _audioEngine.value = AudioEngineType.OFFLINE_DEVICE
                playOffline(text, langCode)
            }
        }
    }

    private suspend fun playWav(file: File) {
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                try {
                    player.setAudioAttributes(attributes)
                    player.setDataSource(file.absolutePath)
                    player.setOnCompletionListener { if (continuation.isActive) continuation.resume(Unit) }
                    player.setOnErrorListener { _, _, _ ->
                        if (continuation.isActive) continuation.resumeWithException(IOException("Audio playback failed"))
                        true
                    }
                    player.prepare()
                    player.start()
                } catch (e: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
            }
        } finally {
            if (mediaPlayer == player) mediaPlayer = null
            runCatching { player.release() }
        }
    }

    private suspend fun playOffline(text: String, langCode: String) {
        if (withTimeoutOrNull(10000) { ttsReady.await() } != true) {
            throw IOException(uiText("Glas uređaja nije dostupan.", "Die Gerätestimme ist nicht verfügbar."))
        }
        val locale = if (langCode.equals("de", true)) Locale.GERMAN else {
            val bosnian = Locale.forLanguageTag("bs-BA")
            if ((tts?.isLanguageAvailable(bosnian) ?: -1) >= TextToSpeech.LANG_AVAILABLE) bosnian
            else Locale.forLanguageTag("hr-HR")
        }
        tts?.language = locale
        tts?.setPitch(1.05f)
        tts?.setSpeechRate(0.92f)
        while (true) {
            awaitResumed()
            val signal = CompletableDeferred<Boolean>()
            val id = "paragraph_${System.nanoTime()}"
            offlineUtterance = id to signal
            if (tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) {
                throw IOException(uiText("Glas uređaja nije dostupan.", "Die Gerätestimme ist nicht verfügbar."))
            }
            val completed = signal.await()
            if (offlineUtterance?.first == id) offlineUtterance = null
            if (completed) return
            // Device TTS cannot seek; after Pause only the current paragraph is restarted.
        }
    }

    private suspend fun awaitResumed() { paused.first { !it }; currentCoroutineContext().ensureActive() }
    private suspend fun paragraphGap() {
        var remaining = NarrationPlan.GAP_MS
        while (remaining > 0) {
            awaitResumed()
            delay(50)
            if (!paused.value) remaining -= 50
        }
    }

    private fun startSession(preview: Boolean, operation: suspend CoroutineScope.() -> Unit) {
        _errorMessage.value = null
        _isPreviewing.value = preview
        if (!requestFocus()) {
            _ttsState.value = TtsState.ERROR
            _isPreviewing.value = false
            _errorMessage.value = uiText("Zvuk je trenutno zauzet. Pokušaj ponovo.",
                "Der Ton ist gerade belegt. Bitte versuche es erneut.")
            return
        }
        _ttsState.value = TtsState.PREPARING
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try { operation() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _errorMessage.value = e.message ?: uiText("Čitanje nije uspjelo.", "Das Vorlesen ist fehlgeschlagen.")
                _ttsState.value = TtsState.ERROR
            } finally {
                if (speechJob == coroutineContext.job) {
                    releaseVoicePlayer()
                    offlineUtterance = null
                    tts?.stop()
                    music.stop()
                    abandonFocus()
                    _isPreviewing.value = false
                    _progress.value = NarrationProgress()
                    if (_ttsState.value != TtsState.ERROR) _ttsState.value = TtsState.IDLE
                    speechJob = null
                }
            }
        }
        speechJob = job
        job.start()
    }

    fun pause() {
        if (_ttsState.value != TtsState.PLAYING && _ttsState.value != TtsState.PREPARING) return
        paused.value = true
        mediaPlayer?.let { runCatching { if (it.isPlaying) it.pause() } }
        offlineUtterance?.second?.complete(false)
        tts?.stop()
        music.pause()
        abandonFocus()
        _ttsState.value = TtsState.PAUSED
    }
    fun resume() {
        if (_ttsState.value != TtsState.PAUSED) return
        if (!hasFocus && !requestFocus()) return
        paused.value = false
        mediaPlayer?.let { runCatching { it.start() } }
        music.resume()
        _ttsState.value = if (_progress.value.paragraph > 0) TtsState.PLAYING else TtsState.PREPARING
    }
    fun stop() {
        val oldJob = speechJob
        speechJob = null
        oldJob?.cancel()
        releaseVoicePlayer()
        offlineUtterance = null
        tts?.stop()
        music.stop()
        abandonFocus()
        paused.value = false
        _ttsState.value = TtsState.IDLE
        _isPreviewing.value = false
        _progress.value = NarrationProgress()
    }
    private fun releaseVoicePlayer() {
        mediaPlayer?.let { runCatching { it.release() } }
        mediaPlayer = null
    }

    fun previewVoice(voice: GeminiVoice, langCode: String) {
        stop()
        _audioEngine.value = if (voice.isOfflineDevice) AudioEngineType.OFFLINE_DEVICE else AudioEngineType.GEMINI_NATURAL
        val sample = if (langCode.equals("de", true)) "Willkommen in der Traumwelt! Ich bin dein Geschichtenerzähler."
            else "Dobrodošli u Svijet snova! Ja sam tvoj pripovjedač."
        startSession(preview = true) {
            val clip = if (voice.isOfflineDevice) Clip(null) else prepareClip(sample, langCode, voice)
            awaitResumed()
            _ttsState.value = TtsState.PLAYING
            playClip(clip, sample, langCode)
        }
    }
    fun stopPreview() = stop()

    @Suppress("DEPRECATION")
    private fun requestFocus(): Boolean {
        if (hasFocus) return true
        val result = if (Build.VERSION.SDK_INT >= 26) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(focusListener).build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }
    @Suppress("DEPRECATION")
    private fun abandonFocus() {
        if (!hasFocus) return
        if (Build.VERSION.SDK_INT >= 26) focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        else audioManager.abandonAudioFocus(focusListener)
        hasFocus = false
        focusRequest = null
    }

    private fun cacheKey(voiceId: String, langCode: String, text: String): String =
        MessageDigest.getInstance("MD5").digest("gemini-3.8-wav-v2:$voiceId:$langCode:$text".toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun destroy() {
        stop()
        scope.cancel()
        tts?.shutdown()
        tts = null
    }
}
