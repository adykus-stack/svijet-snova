package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.*
import com.example.data.remote.*
import com.example.data.security.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrivateAiKeysTest {
    private val fakeKey = "test-private-key-not-a-real-key"
    private fun client(handler: (Request) -> Pair<Int, ByteArray>) = OkHttpClient.Builder().addInterceptor { chain ->
        val (status, body) = handler(chain.request())
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(status).message("Test")
            .body(body.toResponseBody("application/octet-stream".toMediaType())).build()
    }.build()

    @Test fun keysAreEncryptedWithFreshIvAndBoundToTheirProvider() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_private_keys", 0)
        prefs.edit().clear().commit()
        val encryptionKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val store = AiKeyStore(context, "test_private_keys") { encryptionKey }
        store.save(AiProvider.GEMINI, fakeKey)
        val first = prefs.getString("GEMINI", "")!!
        assertFalse(first.contains(fakeKey))
        assertEquals(fakeKey, AiKeyStore(context, "test_private_keys") { encryptionKey }.read(AiProvider.GEMINI))
        store.save(AiProvider.GEMINI, fakeKey)
        assertNotEquals(first, prefs.getString("GEMINI", ""))
        prefs.edit().putString("OPENAI", first).commit()
        assertThrows(Exception::class.java) { store.read(AiProvider.OPENAI) }
        store.save(AiProvider.OPENAI, "another-test-key-not-real-either")
        store.remove(AiProvider.GEMINI)
        assertEquals("", store.read(AiProvider.GEMINI))
        assertEquals("another-test-key-not-real-either", store.read(AiProvider.OPENAI))
    }

    @Test fun keyValidationUsesOnlyTheSelectedProviderAndDoesNotGenerateContent() = runBlocking {
        val requests = mutableListOf<Request>()
        val client = client { request -> requests.add(request); 200 to "{}".toByteArray() }
        val validator = ApiKeyValidator(client)
        validator.validate(AiProvider.GEMINI, fakeKey)
        validator.validate(AiProvider.OPENAI, fakeKey)
        assertEquals(2, requests.size)
        assertEquals("generativelanguage.googleapis.com", requests[0].url.host)
        assertEquals("api.openai.com", requests[1].url.host)
        assertEquals(fakeKey, requests[0].header("x-goog-api-key"))
        assertNull(requests[0].header("Authorization"))
        assertEquals("Bearer $fakeKey", requests[1].header("Authorization"))
        assertNull(requests[1].header("x-goog-api-key"))
        requests.forEach { assertEquals("GET", it.method); assertNull(it.body); assertNull(it.url.query) }
    }

    @Test fun malformedAndRejectedKeysDoNotExposeSecretsInErrors() = runBlocking {
        var calls = 0
        val validator = ApiKeyValidator(client { calls++; 401 to "Server echoed $fakeKey".toByteArray() })
        try { validator.validate(AiProvider.OPENAI, "too short"); fail() } catch (_: IllegalArgumentException) { }
        assertEquals(0, calls)
        try { validator.validate(AiProvider.OPENAI, fakeKey); fail() } catch (e: AiRequestException) {
            assertEquals(401, e.status)
            assertFalse(e.message!!.contains(fakeKey))
        }
        assertEquals(1, calls)
    }

    @Test fun redirectsAreRejectedWithoutForwardingTheCredential() = runBlocking {
        val calls = java.util.concurrent.atomic.AtomicInteger()
        val server = java.net.ServerSocket(0)
        val serverThread = Thread {
            try {
                while (!server.isClosed) server.accept().use { socket ->
                    val reader = socket.getInputStream().bufferedReader()
                    while (reader.readLine()?.isNotEmpty() == true) { }
                    val firstRequest = calls.incrementAndGet() == 1
                    val response = if (firstRequest)
                        "HTTP/1.1 302 Found\r\nLocation: http://127.0.0.1:${server.localPort}/redirected\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                    else "HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\n{}"
                    socket.getOutputStream().write(response.toByteArray(Charsets.US_ASCII))
                }
            } catch (_: java.net.SocketException) { }
        }.apply { isDaemon = true; start() }
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            // A real local HTTP response exercises OkHttp's redirect handling.
            chain.proceed(chain.request().newBuilder().url("http://127.0.0.1:${server.localPort}/check").build())
        }.build()
        try {
            try { ApiKeyValidator(client).validate(AiProvider.GEMINI, fakeKey); fail() } catch (e: AiRequestException) {
                assertEquals(302, e.status)
            }
            assertEquals(1, calls.get())
        } finally {
            server.close(); serverThread.join(2000)
            client.dispatcher.executorService.shutdownNow(); client.connectionPool.evictAll()
        }
    }

    private fun output(json: JSONObject) = JSONObject().put("status", "completed").put("output", JSONArray()
        .put(JSONObject().put("type", "reasoning"))
        .put(JSONObject().put("type", "message").put("content", JSONArray()
            .put(JSONObject().put("type", "output_text").put("text", json.toString()))))).toString().toByteArray()

    @Test fun openAiCreatesAndTranslatesStoriesThroughTheSharedParser() = runBlocking {
        val requests = mutableListOf<Request>()
        val client = client { request ->
            requests.add(request)
            val chapter = JSONObject().put("title", "Zvijezda").put("chapterNumber", 1).put("chapterTitle", "Početak")
                .put("storyText", "Mala vila je pronašla zvijezdu.").put("moral", "Budi dobar.")
                .put("musicMood", "calm").put("continuationOptions", JSONArray().put(JSONObject()
                    .put("primaryText", "Prati zvijezdu").put("translatedText", "Folge dem Stern")))
            200 to output(if (requests.size == 1) chapter else JSONObject().put("title", "Stern")
                .put("chapters", JSONArray().put(chapter.put("storyText", "Die kleine Fee fand einen Stern."))))
        }
        val service = GeminiService(textProvider = { AiProvider.OPENAI },
            openAiService = OpenAiService({ fakeKey }, client), apiKeyProvider = { error("Must not access Gemini key") })
        val generated = service.generateStoryChapter(StoryCreationConfig()).getOrThrow()
        assertEquals("Mala vila je pronašla zvijezdu.", generated.chapter.storyText)
        assertEquals("Folge dem Stern", generated.chapter.bilingualOptions.single().translatedText)
        val translated = service.translateStory(ActiveStory(title = generated.title, config = StoryCreationConfig(),
            chapters = listOf(generated.chapter)), StoryLanguage.GERMAN).getOrThrow()
        assertEquals(StoryLanguage.GERMAN, translated.config.language)
        assertEquals("Die kleine Fee fand einen Stern.", translated.chapters.single().storyText)
        requests.forEach { request ->
            assertEquals("/v1/responses", request.url.encodedPath)
            assertEquals("Bearer $fakeKey", request.header("Authorization"))
            val buffer = okio.Buffer(); request.body!!.writeTo(buffer)
            val body = JSONObject(buffer.readUtf8())
            assertEquals("gpt-6-luna", body.getString("model"))
            assertFalse(body.getBoolean("store"))
            assertEquals("json_object", body.getJSONObject("text").getJSONObject("format").getString("type"))
        }
    }

    @Test fun openAiSpeechUsesTheSelectedVoiceAndReturnsWavWithoutChangingIt() = runBlocking {
        val wav = ByteArray(48).apply { "RIFF".toByteArray().copyInto(this); "WAVE".toByteArray().copyInto(this, 8) }
        val client = client { request ->
            assertEquals("/v1/audio/speech", request.url.encodedPath)
            val buffer = okio.Buffer(); request.body!!.writeTo(buffer)
            val body = JSONObject(buffer.readUtf8())
            assertEquals("marin", body.getString("voice"))
            assertEquals("wav", body.getString("response_format"))
            assertEquals("Mala vila.", body.getString("input"))
            assertTrue(body.getString("instructions").contains("Bosnian"))
            200 to wav
        }
        assertArrayEquals(wav, OpenAiService({ fakeKey }, client).speech("Mala vila.", StoryLanguage.BOSNIAN, "marin").getOrThrow())
        assertTrue(OpenAiService({ fakeKey }, client { 200 to "invalid audio".toByteArray() })
            .speech("Test", StoryLanguage.GERMAN, "cedar").isFailure)
    }

    @Test fun cancellingKeyValidationCancelsTheHttpCall() = runBlocking {
        val started = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            started.countDown()
            val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (!chain.call().isCanceled() && System.nanoTime() < until) Thread.sleep(5)
            if (chain.call().isCanceled()) cancelled.countDown()
            throw java.io.IOException("Stopped test request")
        }.build()
        try {
            val job = launch(Dispatchers.IO) { ApiKeyValidator(client).validate(AiProvider.OPENAI, fakeKey) }
            assertTrue(started.await(3, TimeUnit.SECONDS))
            withTimeout(3000) { job.cancelAndJoin() }
            assertTrue(cancelled.await(3, TimeUnit.SECONDS))
        } finally { client.dispatcher.executorService.shutdownNow(); client.connectionPool.evictAll() }
    }
}
