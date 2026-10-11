package com.example.data.remote

import com.example.data.model.StoryLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAiService(private val keyProvider: () -> String,
    private val client: OkHttpClient = OkHttpClient.Builder().callTimeout(180, TimeUnit.SECONDS).build()) {

    suspend fun generateJson(prompt: String): String = withContext(Dispatchers.IO) {
        val body = JSONObject().put("model", "gpt-6-luna").put("input", prompt)
            .put("store", false).put("reasoning", JSONObject().put("effort", "low"))
            .put("text", JSONObject().put("format", JSONObject().put("type", "json_object")))
        val root = JSONObject(client.aiRequest(request("responses", body)).toString(Charsets.UTF_8))
        check(root.optString("status") == "completed") { "OpenAI odgovor nije dovršen." }
        val output = root.getJSONArray("output")
        val text = buildString {
            for (i in 0 until output.length()) {
                val item = output.getJSONObject(i)
                if (item.optString("type") != "message") continue
                val content = item.getJSONArray("content")
                for (j in 0 until content.length()) {
                    val part = content.getJSONObject(j)
                    if (part.optString("type") == "output_text") append(part.getString("text"))
                }
            }
        }
        check(text.isNotBlank()) { "OpenAI nije vratio tekst priče." }
        // Keep one shared story JSON parser for both providers.
        JSONObject().put("candidates", JSONArray().put(JSONObject().put("content",
            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", text)))))).toString()
    }

    suspend fun speech(text: String, language: StoryLanguage, voice: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("model", "gpt-4o-mini-tts").put("voice", voice)
                .put("input", text).put("response_format", "wav")
                .put("instructions", "Read the exact supplied text in ${if (language == StoryLanguage.GERMAN) "German" else "Bosnian"}. Warm, gentle storyteller; clear pronunciation and natural pace. Do not add or omit words.")
            val wav = client.aiRequest(request("audio/speech", body))
            require(wav.size > 44 && String(wav, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                String(wav, 8, 4, Charsets.US_ASCII) == "WAVE") { "OpenAI nije vratio ispravan WAV zvuk." }
            Result.success(wav)
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }
    }

    private fun request(path: String, body: JSONObject): Request {
        val key = keyProvider()
        check(key.isNotBlank()) { "Unesi OpenAI API ključ u roditeljskim postavkama / OpenAI API-Schlüssel in den Elterneinstellungen eingeben." }
        return Request.Builder().url("https://api.openai.com/v1/$path").header("Authorization", "Bearer $key")
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()
    }
}
