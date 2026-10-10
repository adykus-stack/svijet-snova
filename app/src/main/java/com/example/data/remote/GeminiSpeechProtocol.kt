package com.example.data.remote

import com.example.data.model.StoryLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

/** Single-speaker Interactions API requests. Directions must never be read as story text. */
internal object GeminiSpeechProtocol {
    const val MODEL = "gemini-3.8-flash-lite-tts"
    const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/interactions"

    fun request(text: String, language: StoryLanguage, voice: String): JSONObject {
        require(text.isNotBlank()) { "Nema teksta za čitanje." }
        val style = if (language == StoryLanguage.GERMAN) {
            "Lies auf Deutsch als warmer, natürlicher und sanfter Geschichtenerzähler für Kinder. " +
                "Sprich klar, mäßig langsam und ausdrucksstark, mit natürlichen Pausen."
        } else {
            "Čitaj na bosanskom kao topao, prirodan i nježan pripovjedač dječjih priča. " +
                "Govori jasno, umjereno sporo i izražajno, s prirodnim pauzama."
        }
        return JSONObject().apply {
            put("model", MODEL)
            put("input", JSONArray().put(JSONObject().apply {
                put("type", "user_input")
                put("content", JSONArray().put(JSONObject().apply {
                    put("type", "text")
                    put("text", text)
                    put("annotations", JSONArray().put(JSONObject().apply {
                        put("type", "speech_metadata")
                        put("style", style)
                    }))
                }))
            }))
            put("response_format", JSONObject().apply {
                put("type", "audio")
                put("mime_type", "audio/wav")
            })
            put("generation_config", JSONObject().apply {
                put("speech_config", JSONArray().put(JSONObject().put("voice", voice)))
            })
        }
    }

    fun parseAudio(response: String): ByteArray {
        val root = JSONObject(response)
        var audio: JSONObject? = null
        val steps = root.optJSONArray("steps") ?: JSONArray()
        for (i in 0 until steps.length()) {
            val step = steps.optJSONObject(i) ?: continue
            if (step.optString("type") != "model_output") continue
            val content = step.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val part = content.optJSONObject(j) ?: continue
                if (part.optString("type") == "audio" && part.optString("data").isNotBlank()) {
                    audio = part
                }
            }
        }
        val part = audio ?: root.optJSONObject("output_audio")
            ?: throw IllegalArgumentException("Gemini TTS nije vratio audio podatke.")
        val bytes = Base64.getDecoder().decode(part.getString("data"))
        require(bytes.size > 44 &&
            String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(bytes, 8, 4, Charsets.US_ASCII) == "WAVE") {
            "Gemini TTS nije vratio ispravan WAV zvuk."
        }
        // WAV already includes its header; adding another one corrupts playback.
        return bytes
    }
}
