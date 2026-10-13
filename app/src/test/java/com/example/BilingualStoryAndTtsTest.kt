package com.example

import com.example.data.local.StoryEntity
import com.example.data.model.*
import com.example.data.remote.FallbackStoryGenerator
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BilingualStoryAndTtsTest {

    @Test
    fun `chapter getEffectiveOptions returns bilingualOptions when available`() {
        val chapter = Chapter(
            chapterNumber = 1,
            chapterTitle = "Čarobna šuma",
            storyText = "Luka je ušao u čarobnu šumu...",
            moral = "Hrabrost otvara sva vrata.",
            continuationOptions = listOf("Otvori vrata", "Zaviri u pećinu"),
            bilingualOptions = listOf(
                StoryOption(
                    primaryText = "Luka odlučuje otvoriti čarobna vrata",
                    translatedText = "Luka beschließt, die magische Tür zu öffnen."
                ),
                StoryOption(
                    primaryText = "Luka slijedi svjetlucavi trag",
                    translatedText = "Luka folgt der glitzernden Spur."
                )
            )
        )

        val options = chapter.getEffectiveOptions()
        assertEquals(2, options.size)
        assertEquals("Luka odlučuje otvoriti čarobna vrata", options[0].primaryText)
        assertEquals("Luka beschließt, die magische Tür zu öffnen.", options[0].translatedText)
        assertEquals("Luka slijedi svjetlucavi trag", options[1].primaryText)
        assertEquals("Luka folgt der glitzernden Spur.", options[1].translatedText)
    }

    @Test
    fun `chapter getEffectiveOptions falls back to legacy continuationOptions if bilingualOptions empty`() {
        val chapter = Chapter(
            chapterNumber = 1,
            chapterTitle = "Poglavlje 1",
            storyText = "Tekst priče...",
            moral = "Pouka.",
            continuationOptions = listOf("Opcija A", "Opcija B")
        )

        val options = chapter.getEffectiveOptions()
        assertEquals(2, options.size)
        assertEquals("Opcija A", options[0].primaryText)
        assertEquals("", options[0].translatedText)
        assertEquals("Opcija B", options[1].primaryText)
        assertEquals("", options[1].translatedText)
    }

    @Test
    fun `story entity serialization and deserialization preserves bilingual options`() {
        val activeStory = ActiveStory(
            id = 42L,
            title = "Mala vila i zvjezdani prah",
            config = StoryCreationConfig(
                language = StoryLanguage.BOSNIAN,
                heroName = "Mia"
            ),
            chapters = listOf(
                Chapter(
                    chapterNumber = 1,
                    chapterTitle = "Buđenje zvijezda",
                    storyText = "Mia je pogledala prema nebu...",
                    moral = "Prijateljstvo sja jače od zvijezda.",
                    continuationOptions = listOf("Mia prati leptira"),
                    bilingualOptions = listOf(
                        StoryOption(
                            primaryText = "Mia prati svjetlucavog leptira",
                            translatedText = "Mia folgt dem leuchtenden Schmetterling."
                        ),
                        StoryOption(
                            primaryText = "Mia doziva malu sovu",
                            translatedText = "Mia ruft die kleine Eule."
                        )
                    )
                )
            )
        )

        val entity = StoryEntity.fromActiveStory(activeStory)
        val restoredStory = entity.toActiveStory()

        assertEquals("Mala vila i zvjezdani prah", restoredStory.title)
        assertEquals(1, restoredStory.chapters.size)
        val restoredChapter = restoredStory.chapters[0]
        assertEquals(2, restoredChapter.bilingualOptions.size)
        assertEquals("Mia prati svjetlucavog leptira", restoredChapter.bilingualOptions[0].primaryText)
        assertEquals("Mia folgt dem leuchtenden Schmetterling.", restoredChapter.bilingualOptions[0].translatedText)
        assertEquals("Mia doziva malu sovu", restoredChapter.bilingualOptions[1].primaryText)
        assertEquals("Mia ruft die kleine Eule.", restoredChapter.bilingualOptions[1].translatedText)
    }

    @Test
    fun `fallback story generator produces bilingual options for bosnian stories`() {
        val config = StoryCreationConfig(
            language = StoryLanguage.BOSNIAN,
            heroName = "Tarik"
        )

        val chapterResponse = FallbackStoryGenerator.generateFallbackChapter(
            config = config,
            chapterNum = 1,
            continuationChoice = null,
            isFinalChapter = false
        )

        assertNotNull(chapterResponse)
        val firstChapter = chapterResponse.chapter
        assertTrue("Bilingual options should not be empty", firstChapter.bilingualOptions.isNotEmpty())

        for (option in firstChapter.bilingualOptions) {
            assertTrue("Primary text should not be empty", option.primaryText.isNotBlank())
            assertTrue("Translated text (German) should not be empty", option.translatedText.isNotBlank())
            assertNotEquals("Primary and translated text should be different languages", option.primaryText, option.translatedText)
        }
    }

    @Test
    fun `fallback story generator produces bilingual options for german stories`() {
        val config = StoryCreationConfig(
            language = StoryLanguage.GERMAN,
            heroName = "Felix"
        )

        val chapterResponse = FallbackStoryGenerator.generateFallbackChapter(
            config = config,
            chapterNum = 1,
            continuationChoice = null,
            isFinalChapter = false
        )

        assertNotNull(chapterResponse)
        val firstChapter = chapterResponse.chapter
        assertTrue("Bilingual options should not be empty", firstChapter.bilingualOptions.isNotEmpty())

        for (option in firstChapter.bilingualOptions) {
            assertTrue("Primary text should not be empty", option.primaryText.isNotBlank())
            assertTrue("Translated text (Bosnian) should not be empty", option.translatedText.isNotBlank())
            assertNotEquals("Primary and translated text should be different languages", option.primaryText, option.translatedText)
        }
    }

    @Test
    fun `voice list contains only AI voices and keeps Aoede as fallback`() {
        val voices = GeminiVoices.allVoices
        assertTrue("Expected Gemini and OpenAI voices", voices.size >= 6)

        val voiceIds = voices.map { it.id }
        assertTrue(voiceIds.contains("Aoede"))
        assertTrue(voiceIds.contains("Puck"))
        assertTrue(voiceIds.contains("Charon"))
        assertTrue(voiceIds.contains("Fenrir"))
        assertTrue(voiceIds.contains("Leda"))
        assertTrue(voiceIds.contains("Kore"))
        assertFalse(voiceIds.contains("OFFLINE_DEVICE"))

        val defaultVoice = GeminiVoices.getById("unknown_voice_id")
        assertEquals("Aoede", defaultVoice.id)
    }
}
