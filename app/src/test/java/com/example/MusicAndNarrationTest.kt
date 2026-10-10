package com.example

import com.example.data.local.StoryEntity
import com.example.data.model.*
import com.example.data.remote.FallbackStoryGenerator
import com.example.tts.InstrumentalMusic
import com.example.tts.NarrationPlan
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MusicAndNarrationTest {
    @Test
    fun paragraphsKeepTheirOrderAndWordsInBothLanguages() {
        val paragraphs = listOf("Hana je ugledala čarobnu zvijezdu.", "Die Sterne leuchteten über dem Wald.", "Pouka: Budi dobar.")
        assertEquals(paragraphs, NarrationPlan.segments(paragraphs.joinToString("\r\n\r\n")))
        assertTrue(NarrationPlan.segments(" \n\n ").isEmpty())
        val long = ("Eine kleine Fee ging durch den Wald. ").repeat(180).trim()
        val pieces = NarrationPlan.segments(long)
        assertTrue(pieces.size > 1)
        assertTrue(pieces.all { it.length <= NarrationPlan.MAX_SEGMENT_CHARS })
        assertEquals(long.replace(Regex("\\s+"), " "), pieces.joinToString(" ").replace(Regex("\\s+"), " "))
    }

    @Test
    fun allSixOriginalScoresAreValidDistinctAndHaveHeadroom() {
        val hashes = mutableSetOf<Int>()
        for (mood in MusicMood.entries) {
            val bytes = InstrumentalMusic.render(mood, bars = 2)
            val wav = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals("RIFF", String(bytes, 0, 4))
            assertEquals("WAVE", String(bytes, 8, 4))
            assertEquals(bytes.size - 8, wav.getInt(4))
            assertEquals(InstrumentalMusic.SAMPLE_RATE, wav.getInt(24))
            assertEquals(1, wav.getShort(22).toInt())
            assertEquals(16, wav.getShort(34).toInt())
            assertEquals(bytes.size - 44, wav.getInt(40))
            wav.position(44)
            var peak = 0
            while (wav.hasRemaining()) peak = maxOf(peak, abs(wav.short.toInt()))
            assertTrue(peak in 10000..24000)
            assertTrue(abs(wav.getShort(44).toInt()) < 100)
            assertTrue(abs(wav.getShort(bytes.size - 2).toInt()) < 100)
            hashes += bytes.contentHashCode()
        }
        assertEquals(6, hashes.size)
        val preview = File("build/music-preview").apply { mkdirs() }
        File(preview, "magic.wav").writeBytes(InstrumentalMusic.render(MusicMood.MAGIC))
    }

    @Test
    fun moodSurvivesSavingAndTranslationWhileOldStoriesStillLoad() {
        val story = ActiveStory(title = "Priča", config = StoryCreationConfig(),
            chapters = listOf(Chapter(1, "Početak", "Bila jednom vila.", "Budi dobar.", musicMood = MusicMood.MAGIC)))
        val saved = StoryEntity.fromActiveStory(story)
        assertEquals(MusicMood.MAGIC, saved.toActiveStory().chapters.single().musicMood)
        assertEquals(MusicMood.MAGIC, FallbackStoryGenerator.translateLocally(story, StoryLanguage.GERMAN).chapters.single().musicMood)
        val old = saved.copy(chaptersJson = """[{"chapterNumber":1,"storyText":"Stara priča"}]""")
        assertNull(old.toActiveStory().chapters.single().musicMood)
        assertNull(MusicMood.fromCode("unknown"))
        assertEquals(MusicMood.LULLABY, MusicMood.forCategory("bedtime"))
    }
}
