package com.example.data.cloud

import android.app.Activity
import android.content.Context
import android.util.Base64
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

data class GoogleAccountInfo(val uid: String, val displayName: String, val email: String)
data class CloudSnapshot(val backup: String, val settings: Map<String, Any?>)

/** Optional Google account sync. Credentials never leave Firebase Auth and are never stored here. */
class GoogleCloudSync(private val context: Context) {
    private val credentialManager by lazy { CredentialManager.create(context) }

    fun isConfigured(): Boolean {
        val clientId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        return clientId != 0 && context.getString(clientId).isNotBlank() && firebaseApp() != null
    }

    fun currentAccount(): GoogleAccountInfo? {
        if (!isConfigured()) return null
        return runCatching { FirebaseAuth.getInstance().currentUser }.getOrNull()?.let {
            GoogleAccountInfo(it.uid, it.displayName.orEmpty(), it.email.orEmpty())
        }
    }

    suspend fun signIn(activity: Activity): GoogleAccountInfo {
        check(isConfigured()) {
            "Google sinkronizacija još nije povezana s Firebase projektom."
        }
        val clientIdResource = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val serverClientId = context.getString(clientIdResource)
        suspend fun credential(authorizedOnly: Boolean) = credentialManager.getCredential(
            context = activity,
            request = GetCredentialRequest.Builder().addCredentialOption(
                GetGoogleIdOption.Builder().setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(authorizedOnly).setAutoSelectEnabled(authorizedOnly).build()
            ).build()
        )
        val response = try { credential(true) } catch (_: GetCredentialException) { credential(false) }
        val raw = response.credential
        check(raw.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { "Google račun nije izabran." }
        val google = GoogleIdTokenCredential.createFrom(raw.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(google.idToken, null)
        val result = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).awaitResult()
        val user = checkNotNull(result.user) { "Google prijava nije uspjela." }
        return GoogleAccountInfo(user.uid, user.displayName.orEmpty(), user.email.orEmpty())
    }

    suspend fun signOut() {
        if (firebaseApp() != null) FirebaseAuth.getInstance().signOut()
        runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }

    suspend fun download(): CloudSnapshot? = withContext(Dispatchers.IO) {
        val user = currentAccount() ?: return@withContext null
        val db = FirebaseFirestore.getInstance()
        val manifest = db.collection("users").document(user.uid).collection("sync").document("current")
            .get().awaitResult()
        if (!manifest.exists()) return@withContext null
        val generation = manifest.getString("generation") ?: return@withContext null
        val count = (manifest.getLong("chunkCount") ?: 0L).toInt()
        require(count in 1..200) { "Neispravna Google sigurnosna kopija." }
        val payload = buildString {
            repeat(count) { index ->
                val part = db.collection("users").document(user.uid).collection("payload")
                    .document("$generation-$index").get().awaitResult().getString("data")
                    ?: error("Google kopija nije potpuna.")
                append(part)
            }
        }
        val expected = manifest.getString("sha256").orEmpty()
        require(expected == CloudPayloadCodec.sha256(payload)) { "Google kopija je oštećena." }
        @Suppress("UNCHECKED_CAST")
        CloudSnapshot(CloudPayloadCodec.decode(payload), manifest.get("settings") as? Map<String, Any?> ?: emptyMap())
    }

    suspend fun upload(backup: String, settings: Map<String, Any?>) = withContext(Dispatchers.IO) {
        val user = currentAccount() ?: return@withContext
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(user.uid)
        val manifestRef = userDoc.collection("sync").document("current")
        val previous = runCatching { manifestRef.get().awaitResult() }.getOrNull()
        val oldGeneration = previous?.getString("generation")
        val oldCount = (previous?.getLong("chunkCount") ?: 0L).toInt()
        val generation = UUID.randomUUID().toString()
        val encoded = CloudPayloadCodec.encode(backup)
        val chunks = encoded.chunked(CloudPayloadCodec.CHUNK_CHARS)
        require(chunks.size in 1..200) { "Kopija je prevelika za Google sinkronizaciju." }
        chunks.forEachIndexed { index, part ->
            userDoc.collection("payload").document("$generation-$index")
                .set(mapOf("generation" to generation, "index" to index, "data" to part)).awaitResult()
        }
        manifestRef.set(mapOf("generation" to generation, "chunkCount" to chunks.size,
            "sha256" to CloudPayloadCodec.sha256(encoded), "settings" to settings,
            "updatedAt" to FieldValue.serverTimestamp(), "formatVersion" to 1)).awaitResult()
        if (!oldGeneration.isNullOrBlank() && oldGeneration != generation && oldCount in 1..200) {
            repeat(oldCount) { index ->
                runCatching { userDoc.collection("payload").document("$oldGeneration-$index").delete().awaitResult() }
            }
        }
    }

    private fun firebaseApp(): FirebaseApp? = FirebaseApp.getApps(context).firstOrNull()
        ?: FirebaseApp.initializeApp(context)
}

object CloudPayloadCodec {
    const val CHUNK_CHARS = 700_000

    fun encode(text: String): String {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).bufferedWriter(Charsets.UTF_8).use { it.write(text) }
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    fun decode(encoded: String): String = GZIPInputStream(
        ByteArrayInputStream(Base64.decode(encoded, Base64.NO_WRAP))
    ).bufferedReader(Charsets.UTF_8).use { it.readText() }

    fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        if (task.isSuccessful) continuation.resume(task.result)
        else continuation.resumeWithException(task.exception ?: IllegalStateException("Google zahtjev nije uspio."))
    }
}
