package com.example.data.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Only a salted verifier is stored; resetting it never touches stories or API credentials. */
class ParentPinStore(context: Context, name: String = "parent_pin") {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    fun exists() = prefs.contains("verifier")
    fun set(pin: String) {
        require(pin.length == 4 && pin.all { it in '0'..'9' })
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        check(prefs.edit().putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("verifier", Base64.encodeToString(derive(pin, salt), Base64.NO_WRAP))
            .remove("failures").remove("locked_until").commit())
    }
    fun waitSeconds(): Int = ((prefs.getLong("locked_until", 0) - System.currentTimeMillis() + 999) / 1000).toInt().coerceAtLeast(0)
    fun verify(pin: String): Boolean {
        if (!exists() || waitSeconds() > 0 || pin.length != 4 || !pin.all { it in '0'..'9' }) return false
        val salt = Base64.decode(prefs.getString("salt", ""), Base64.NO_WRAP)
        val expected = Base64.decode(prefs.getString("verifier", ""), Base64.NO_WRAP)
        val ok = MessageDigest.isEqual(expected, derive(pin, salt))
        val failures = if (ok) 0 else prefs.getInt("failures", 0) + 1
        prefs.edit().putInt("failures", failures)
            .putLong("locked_until", if (failures >= 5) System.currentTimeMillis() + 30_000 else 0).apply()
        return ok
    }
    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 60_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
