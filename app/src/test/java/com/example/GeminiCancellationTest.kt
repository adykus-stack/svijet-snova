package com.example

import com.example.data.model.*
import com.example.data.remote.GeminiService
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiCancellationTest {
    // Intercepts requests before the network: no real key or API quota is used.
    private fun assertRequestCanBeCancelled(operation: suspend (GeminiService) -> Unit) = runBlocking {
        val started = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        val returnedResult = AtomicBoolean(false)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            assertNull(chain.request().url.queryParameter("key"))
            assertEquals("test-key-only", chain.request().header("x-goog-api-key"))
            started.countDown()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (!chain.call().isCanceled() && System.nanoTime() < deadline) Thread.sleep(5)
            if (chain.call().isCanceled()) cancelled.countDown()
            throw IOException("Test request stopped")
        }.build()
        try {
            val service = GeminiService(client) { "test-key-only" }
            val job = launch(Dispatchers.IO) {
                operation(service)
                returnedResult.set(true)
            }
            assertTrue("The request did not start", started.await(3, TimeUnit.SECONDS))
            withTimeout(3000) { job.cancelAndJoin() }
            assertTrue("The HTTP call was not cancelled", cancelled.await(3, TimeUnit.SECONDS))
            assertFalse("Cancellation must not return an offline story or voice", returnedResult.get())
            assertTrue(job.isCancelled)
        } finally {
            client.dispatcher.executorService.shutdownNow()
            client.connectionPool.evictAll()
        }
    }

    @Test
    fun cancellingStoryStopsItsHttpRequest() = assertRequestCanBeCancelled {
        it.generateStoryChapter(StoryCreationConfig())
        Unit
    }

    @Test
    fun cancellingTranslationStopsItsHttpRequest() = assertRequestCanBeCancelled {
        it.translateStory(
            ActiveStory(
                title = "Priča", config = StoryCreationConfig(),
                chapters = listOf(Chapter(1, "Početak", "Bila jednom vila.", "Budi dobar."))
            ),
            StoryLanguage.GERMAN
        )
        Unit
    }

    @Test
    fun cancellingVoiceStillStopsItsHttpRequest() = assertRequestCanBeCancelled {
        it.generateSpeech("Bila jednom vila.", StoryLanguage.BOSNIAN, "Aoede")
        Unit
    }
}
