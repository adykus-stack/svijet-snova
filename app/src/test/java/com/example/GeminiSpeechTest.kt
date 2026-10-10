package com.example

import com.example.data.model.StoryLanguage
import com.example.data.remote.GeminiService
import com.example.data.remote.GeminiSpeechProtocol
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiSpeechTest {
    @Test
    fun requestKeepsStyleSeparateFromTranscriptAndUsesSelectedVoice() {
        for (language in StoryLanguage.entries) {
            val text = if (language == StoryLanguage.GERMAN) "Mia sah einen Stern." else "Mia je ugledala zvijezdu."
            val request = GeminiSpeechProtocol.request(text, language, "Aoede")
            assertFalse(request.has("systemInstruction"))
            assertFalse(request.has("system_instruction"))
            assertFalse(request.has("developerInstruction"))
            val turn = request.getJSONArray("input").getJSONObject(0)
            assertEquals("user_input", turn.getString("type"))
            val part = turn.getJSONArray("content").getJSONObject(0)
            assertEquals(text, part.getString("text"))
            val style = part.getJSONArray("annotations").getJSONObject(0)
            assertEquals("speech_metadata", style.getString("type"))
            assertTrue(style.getString("style").isNotBlank())
            assertEquals("Aoede", request.getJSONObject("generation_config")
                .getJSONArray("speech_config").getJSONObject(0).getString("voice"))
            assertEquals("audio/wav", request.getJSONObject("response_format").getString("mime_type"))
        }
    }

    @Test
    fun restAudioIsDecodedWithoutAddingASecondWavHeader() {
        val wav = ByteArray(48)
        "RIFF".toByteArray().copyInto(wav, 0)
        "WAVE".toByteArray().copyInto(wav, 8)
        val response = JSONObject().put("steps", JSONArray().put(
            JSONObject().put("type", "model_output").put("content", JSONArray()
                .put(JSONObject().put("type", "text").put("text", "ignored"))
                .put(JSONObject().put("type", "audio")
                    .put("data", Base64.getEncoder().encodeToString(wav))))
        ))
        assertArrayEquals(wav, GeminiSpeechProtocol.parseAudio(response.toString()))
    }

    @Test
    fun missingOrInvalidAudioIsRejectedBeforePlayback() {
        assertThrows(IllegalArgumentException::class.java) {
            GeminiSpeechProtocol.parseAudio("""{"steps":[]}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            GeminiSpeechProtocol.parseAudio("""{"output_audio":{"data":"bm90IGF1ZGlv"}}""")
        }
    }

    // Opt-in only: normal unit test runs never spend API quota.
    @Test
    fun liveGeminiProducesPlayableBosnianAndGermanAudio() = runBlocking {
        assumeTrue(System.getenv("RUN_GEMINI_TTS_LIVE") == "1")
        val samples = listOf(
            Triple(StoryLanguage.BOSNIAN, "Aoede", "Dobrodošli u Svijet snova. Mala vila je pronašla čarobnu zvijezdu."),
            Triple(StoryLanguage.GERMAN, "Kore", "Willkommen in der Traumwelt. Die kleine Fee hat einen magischen Stern gefunden.")
        )
        for ((language, voice, text) in samples) {
            val result = GeminiService().generateSpeech(text, language, voice)
            assertTrue("Live TTS ${language.code}/$voice: ${result.exceptionOrNull()?.message}", result.isSuccess)
            val wav = result.getOrThrow()
            assertTrue(wav.size > 1000)
            val folder = File("build/tts-verification").apply { mkdirs() }
            File(folder, "${language.code}-$voice.wav").writeBytes(wav)
            println("Live TTS verified: ${language.code}/$voice, ${wav.size} WAV bytes")
        }
    }
}
