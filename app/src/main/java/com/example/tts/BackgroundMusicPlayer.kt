package com.example.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.data.model.MusicMood
import kotlinx.coroutines.*
import java.io.File

internal class BackgroundMusicPlayer(private val context: Context, private val scope: CoroutineScope) {
    private var player: MediaPlayer? = null
    private var prepareJob: Job? = null
    private var fadeJob: Job? = null
    private var sessionActive = false
    private var paused = false
    private var speaking = true
    private var mood = MusicMood.CALM
    private var settings = MusicSettings()
    private var currentGain = 0f
    internal val isPlaying: Boolean get() = player?.let { runCatching { it.isPlaying }.getOrDefault(false) } ?: false
    internal val gain: Float get() = currentGain

    fun start(automaticMood: MusicMood, newSettings: MusicSettings) {
        stop()
        sessionActive = true
        mood = automaticMood
        settings = newSettings
        prepare()
    }

    fun update(newSettings: MusicSettings) {
        val oldMood = settings.selectedMood ?: mood
        val wasEnabled = settings.enabled
        settings = newSettings
        if (!newSettings.enabled) {
            releasePlayer()
            return
        }
        if (sessionActive && (!wasEnabled || oldMood != (settings.selectedMood ?: mood))) {
            releasePlayer()
            prepare()
        } else fadeToTarget()
    }

    private fun prepare() {
        if (!sessionActive || !settings.enabled) return
        val chosenMood = settings.selectedMood ?: mood
        prepareJob = scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val directory = File(context.cacheDir, "original_music").apply { mkdirs() }
                    val output = File(directory, "v1_${chosenMood.code}.wav")
                    if (!output.exists() || output.length() < 44) {
                        val temp = File.createTempFile("score_", ".wav", directory)
                        try {
                            temp.writeBytes(InstrumentalMusic.render(chosenMood))
                            ensureActive()
                            check(temp.renameTo(output)) { "Could not store background music" }
                        } finally { temp.delete() }
                    }
                    output
                }
                ensureActive()
                val created = MediaPlayer()
                player = created
                created.apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    setDataSource(file.absolutePath)
                    isLooping = true
                    setVolume(0f, 0f)
                    setOnErrorListener { _, _, _ -> releasePlayer(); true }
                    prepare()
                }
                if (!paused) created.start()
                fadeToTarget()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { releasePlayer() }
        }
    }

    fun setSpeaking(value: Boolean) { speaking = value; fadeToTarget() }
    fun pause() {
        paused = true
        fadeJob?.cancel()
        player?.let { runCatching { if (it.isPlaying) it.pause() } }
    }
    fun resume() {
        paused = false
        player?.let { runCatching { it.start() } }
        fadeToTarget()
    }

    private fun fadeToTarget() {
        if (paused || !settings.enabled) return
        val activePlayer = player ?: return
        val target = settings.volume * if (speaking) 1f / 3f else 1f
        fadeJob?.cancel()
        val from = currentGain
        fadeJob = scope.launch {
            for (step in 1..20) {
                currentGain = from + (target - from) * step / 20f
                if (player != activePlayer) return@launch
                runCatching { activePlayer.setVolume(currentGain, currentGain) }
                delay(25)
            }
        }
    }

    private fun releasePlayer() {
        prepareJob?.cancel(); prepareJob = null
        fadeJob?.cancel(); fadeJob = null
        player?.let { runCatching { it.release() } }
        player = null
        currentGain = 0f
    }

    fun stop() { sessionActive = false; paused = false; speaking = true; releasePlayer() }
}
