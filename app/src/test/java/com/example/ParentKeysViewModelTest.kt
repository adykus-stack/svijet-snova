package com.example

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.ApiKeyValidator
import com.example.data.security.*
import com.example.ui.MainViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.KeyGenerator

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ParentKeysViewModelTest {
    private suspend fun awaitReady(vm: MainViewModel) = withContext(Dispatchers.Default) {
        withTimeout(5000) { vm.parentKeys.first { !it.busy } }
    }
    @Test fun failedReplacementPreservesOldKeyAndSuccessfulSaveKeepsTheOtherKey() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val application = ApplicationProvider.getApplicationContext<Application>()
        application.getSharedPreferences("test_parent_keys", 0).edit().clear().commit()
        val encryptionKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val keys = AiKeyStore(application, "test_parent_keys") { encryptionKey }
        val oldKey = "existing-gemini-key-for-test-only"
        val otherKey = "existing-openai-key-for-test-only"
        keys.save(AiProvider.GEMINI, oldKey)
        keys.save(AiProvider.OPENAI, otherKey)
        var accepted = false
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(if (accepted) 200 else 401).message("Test")
                .body("Never display this server body".toResponseBody()).build()
        }.build()
        val vm = MainViewModel(application, keys, ApiKeyValidator(client))
        val store = ViewModelStore().apply { put("main", vm) }
        try {
            vm.openParentSettings()
            awaitReady(vm)
            assertEquals(setOf(AiProvider.GEMINI, AiProvider.OPENAI), vm.parentKeys.value.saved)
            vm.checkAndSaveKey(AiProvider.GEMINI, "replacement-key-for-test-only")
            awaitReady(vm)
            assertEquals(oldKey, keys.read(AiProvider.GEMINI))
            assertEquals(otherKey, keys.read(AiProvider.OPENAI))
            assertEquals(0, vm.parentKeys.value.savedRevision)
            assertFalse(vm.parentKeys.value.messageBs.contains("server body"))
            accepted = true
            vm.checkAndSaveKey(AiProvider.GEMINI, " replacement-key-for-test-only ")
            awaitReady(vm)
            assertEquals("replacement-key-for-test-only", keys.read(AiProvider.GEMINI))
            assertEquals(otherKey, keys.read(AiProvider.OPENAI))
            assertEquals(1, vm.parentKeys.value.savedRevision)
            vm.removeKey(AiProvider.GEMINI)
            awaitReady(vm)
            assertEquals("", keys.read(AiProvider.GEMINI))
            assertEquals(otherKey, keys.read(AiProvider.OPENAI))
            assertEquals(setOf(AiProvider.OPENAI), vm.parentKeys.value.saved)
        } finally {
            store.clear()
            runCurrent()
            Dispatchers.resetMain()
            client.dispatcher.executorService.shutdownNow()
            client.connectionPool.evictAll()
        }
    }
}
