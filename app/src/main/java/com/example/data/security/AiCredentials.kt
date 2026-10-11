package com.example.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class AiProvider(val label: String, val keyUrl: String) {
    GEMINI("Gemini", "https://aistudio.google.com/api-keys"),
    OPENAI("OpenAI", "https://platform.openai.com/api-keys")
}

/** The encryption key never leaves Android Keystore. Preferences contain only ciphertext. */
class AiKeyStore(context: Context, prefsName: String = "private_ai_credentials",
    private val encryptionKey: () -> SecretKey = { deviceKey() }) {
    private val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    fun read(provider: AiProvider): String {
        val encoded = prefs.getString(provider.name, null) ?: return ""
        val parts = encoded.split(":")
        check(parts.size == 2)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
        cipher.updateAAD(provider.name.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    fun save(provider: AiProvider, key: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        cipher.updateAAD(provider.name.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(key.trim().toByteArray(Charsets.UTF_8))
        val value = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(prefs.edit().putString(provider.name, value).commit())
    }

    fun remove(provider: AiProvider) { check(prefs.edit().remove(provider.name).commit()) }

    companion object {
        @Synchronized private fun deviceKey(): SecretKey {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (store.getKey("svijet_snova_ai_v1", null) as? SecretKey)?.let { return it }
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder("svijet_snova_ai_v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
            }.generateKey()
        }
    }
}

class AiPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("ai_preferences", Context.MODE_PRIVATE)
    fun storyProvider(): AiProvider = runCatching { AiProvider.valueOf(prefs.getString("story_provider", "GEMINI")!!) }.getOrDefault(AiProvider.GEMINI)
    fun setStoryProvider(provider: AiProvider) { prefs.edit().putString("story_provider", provider.name).apply() }
}
