package com.example

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.model.*
import com.example.tts.*
import com.example.ui.MainViewModel
import com.example.ui.localization.AppLanguagePreferences
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class MusicPlaybackTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var player: StoryAudioPlayer
    private lateinit var originalSettings: MusicSettings
    private lateinit var languagePreferences: AppLanguagePreferences
    private lateinit var originalLanguage: com.example.ui.localization.AppLanguage

    @Before
    fun prepare() {
        compose.runOnIdle {
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            languagePreferences = AppLanguagePreferences(compose.activity)
            originalLanguage = languagePreferences.get()
            player = StoryAudioPlayer(compose.activity) { _, _, _ -> Result.success(silence()) }
            originalSettings = player.musicSettings.value
            player.setMusicEnabled(true)
            player.setMusicVolume(0.30f)
            player.selectMusic(null)
        }
    }

    @After
    fun cleanup() {
        compose.runOnIdle {
            player.stop()
            player.setMusicEnabled(originalSettings.enabled)
            player.setMusicVolume(originalSettings.volume)
            player.selectMusic(originalSettings.selectedMood)
            player.destroy()
            languagePreferences.set(originalLanguage)
        }
    }

    @Test
    fun pauseResumeMusicToggleAndParagraphDuckingWorkTogether() {
        compose.runOnIdle {
            player.playStory("Test ${System.nanoTime()}.\n\nDrugi odlomak.", "bs",
                GeminiVoices.VOICE_AOEDE, MusicMood.MAGIC)
        }
        compose.waitUntil(10000) { player.isBackgroundMusicPlaying && player.backgroundMusicGain > 0.09f }
        compose.runOnIdle {
            assertEquals(AudioEngineType.GEMINI_NATURAL, player.audioEngine.value)
            player.setMusicEnabled(false)
            assertFalse(player.isBackgroundMusicPlaying)
            assertEquals(TtsState.PLAYING, player.ttsState.value)
            player.setMusicEnabled(true)
        }
        compose.waitUntil(5000) { player.isBackgroundMusicPlaying }
        compose.waitUntil(6000) { player.progress.value.inGap && player.backgroundMusicGain > 0.26f }
        compose.runOnIdle {
            assertTrue(player.backgroundMusicGain <= 0.30f)
            player.pause()
            assertFalse(player.isBackgroundMusicPlaying)
        }
        SystemClock.sleep(2300) // Longer than the entire gap: the paragraph timer must stay frozen.
        compose.runOnIdle {
            assertEquals(TtsState.PAUSED, player.ttsState.value)
            assertEquals(1, player.progress.value.paragraph)
            assertTrue(player.progress.value.inGap)
            player.resume()
        }
        compose.waitUntil(6000) { player.progress.value.paragraph == 2 && player.backgroundMusicGain < 0.12f }
        compose.runOnIdle { player.stop() }
        SystemClock.sleep(300)
        compose.runOnIdle {
            assertEquals(TtsState.IDLE, player.ttsState.value)
            assertFalse(player.isBackgroundMusicPlaying)
            assertNull(player.errorMessage.value)
        }
    }

    @Test
    fun stoppingCancelsPrefetchedVoiceAndNeverRestartsMusic() {
        val requests = AtomicInteger()
        val cancelled = AtomicBoolean()
        compose.runOnIdle {
            player.destroy()
            player = StoryAudioPlayer(compose.activity) { _, _, _ ->
                if (requests.incrementAndGet() == 1) {
                    delay(100)
                    Result.success(silence())
                } else {
                    try { awaitCancellation() } finally { cancelled.set(true) }
                }
            }
            player.playStory("Prvi test ${System.nanoTime()}.\n\nNaredni test ${System.nanoTime()}.",
                "bs", GeminiVoices.VOICE_AOEDE, MusicMood.CALM)
        }
        compose.waitUntil(10000) { requests.get() == 2 && player.isBackgroundMusicPlaying }
        compose.runOnIdle { player.stop() }
        compose.waitUntil(5000) { cancelled.get() }
        SystemClock.sleep(300)
        compose.runOnIdle {
            assertEquals(TtsState.IDLE, player.ttsState.value)
            assertFalse(player.isBackgroundMusicPlaying)
            assertEquals(0, player.progress.value.total)
            assertNull(player.errorMessage.value)
        }
    }

    @Test
    fun readerShowsMusicChoicesInBosnianAndGerman() {
        val vm = ViewModelProvider(compose.activity)[MainViewModel::class.java]
        compose.runOnIdle {
            vm.setAppLanguage(com.example.ui.localization.AppLanguage.BOSNIAN)
            vm.openStoryFromLibrary(ActiveStory(
                title = "Hana i čarobna zvijezda", config = StoryCreationConfig(),
                chapters = listOf(Chapter(1, "Svjetlo u šumi",
                    "Hana je pronašla zvijezdu među lišćem.\n\nZvijezda je zasjala toplom svjetlošću.",
                    "Dobrota osvjetljava put.", musicMood = MusicMood.MAGIC))
            ))
        }
        compose.onNodeWithTag("music_controls").performScrollTo()
        compose.onNodeWithText("🎵 Muzika u pozadini").assertIsDisplayed()
        compose.onNodeWithTag("music_settings_toggle").performClick()
        compose.onNodeWithTag("music_volume_slider").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("music_mood_selector").performScrollTo().performClick()
        compose.onNodeWithTag("music_mood_lullaby").performClick()
        compose.onNodeWithText("🌙 Uspavanka").assertIsDisplayed()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), "music-reader-verification.png")
            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.runOnIdle { vm.setAppLanguage(com.example.ui.localization.AppLanguage.GERMAN) }
        compose.onNodeWithText("🎵 Hintergrundmusik").assertIsDisplayed()
        compose.onNodeWithText("🌙 Schlaflied").assertIsDisplayed()
    }

    // Two short real speech requests, only when explicitly enabled for device verification.
    @Test
    fun liveGeminiReadsTwoParagraphsWithMusic() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("music_live") == "true")
        compose.runOnIdle {
            player.destroy()
            player = StoryAudioPlayer(compose.activity)
            player.setMusicEnabled(true)
            player.setMusicVolume(0.30f)
            player.selectMusic(null)
            player.playStory("Hana je pronašla čarobnu zvijezdu.\n\nZvijezda joj je poželjela laku noć.",
                "bs", GeminiVoices.VOICE_AOEDE, MusicMood.MAGIC)
        }
        compose.waitUntil(180000) { player.progress.value.paragraph == 1 && player.isBackgroundMusicPlaying }
        compose.runOnIdle {
            assertEquals(AudioEngineType.GEMINI_NATURAL, player.audioEngine.value)
            assertNull(player.errorMessage.value)
        }
        compose.waitUntil(180000) { player.progress.value.paragraph == 2 }
        compose.runOnIdle {
            assertEquals(AudioEngineType.GEMINI_NATURAL, player.audioEngine.value)
            assertNull(player.errorMessage.value)
        }
        compose.waitUntil(90000) { player.ttsState.value == TtsState.IDLE }
        compose.runOnIdle { assertFalse(player.isBackgroundMusicPlaying) }
    }

    private fun silence(): ByteArray {
        val dataSize = 24000 * 2 * 2
        return ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + dataSize); put("WAVEfmt ".toByteArray())
            putInt(16); putShort(1); putShort(1); putInt(24000); putInt(48000)
            putShort(2); putShort(16); put("data".toByteArray()); putInt(dataSize)
        }.array()
    }
}
