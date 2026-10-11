package com.example.data.remote

import com.example.data.security.AiProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import java.io.IOException

class AiRequestException(val status: Int) : IOException("AI zahtjev nije uspio ($status).")

internal suspend fun OkHttpClient.aiRequest(request: Request): ByteArray = suspendCancellableCoroutine { continuation ->
    // Never forward a private credential to a redirected host.
    val call = newBuilder().followRedirects(false).followSslRedirects(false).build().newCall(request)
    continuation.invokeOnCancellation { call.cancel() }
    call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWith(Result.failure(IOException("Veza s AI servisom nije uspjela.")))
        }
        override fun onResponse(call: Call, response: Response) {
            response.use {
                val result = runCatching {
                    if (!it.isSuccessful) throw AiRequestException(it.code)
                    it.body?.bytes() ?: throw IOException("Prazan odgovor AI servisa.")
                }
                if (continuation.isActive) continuation.resumeWith(result)
            }
        }
    })
}

class ApiKeyValidator(private val client: OkHttpClient = OkHttpClient.Builder()
    .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS).build()) {
    suspend fun validate(provider: AiProvider, key: String) {
        require(key.length in 20..512 && key.none { it.isWhitespace() }) { "format" }
        val request = when (provider) {
            AiProvider.GEMINI -> Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models")
                .header("x-goog-api-key", key)
            AiProvider.OPENAI -> Request.Builder().url("https://api.openai.com/v1/models")
                .header("Authorization", "Bearer $key")
        }.get().build()
        client.aiRequest(request)
    }
}
