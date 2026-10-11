package com.example.data.importing

import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import java.security.MessageDigest

data class ChatConversation(val id: String, val title: String, val transcript: String, val messageCount: Int) {
    val fingerprint: String get() = MessageDigest.getInstance("SHA-256").digest(transcript.toByteArray())
        .joinToString("") { "%02x".format(it) }
}

object ChatGptImport {
    private const val MAX_JSON = 48 * 1024 * 1024
    private const val MAX_TRANSCRIPT = 2_000_000
    fun read(input: InputStream): List<ChatConversation> {
        val buffered = input.buffered()
        buffered.mark(4)
        val prefix = ByteArray(2)
        buffered.read(prefix)
        buffered.reset()
        val result = mutableListOf<ChatConversation>()
        if (prefix[0] == 0x50.toByte() && prefix[1] == 0x4b.toByte()) {
            ZipInputStream(buffered).use { zip ->
                var count = 0
                var jsonBytes = 0
                var skippedBytes = 0L
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(++count <= 20_000) { "ZIP ima previše datoteka / Zu viele Dateien im ZIP." }
                    val name = entry.name.substringAfterLast('/').substringAfterLast('\\')
                    if (name.matches(Regex("conversations(?:[-_]?\\d+)?\\.json", RegexOption.IGNORE_CASE))) {
                        val bytes = readLimited(zip, MAX_JSON - jsonBytes)
                        jsonBytes += bytes.size
                        result += parse(bytes.toString(Charsets.UTF_8))
                    } else {
                        // Never extract files; also bound decompression of unrelated attachments.
                        val buffer = ByteArray(8192)
                        while (true) {
                            val n = zip.read(buffer)
                            if (n < 0) break
                            skippedBytes += n
                            require(skippedBytes <= 256L * 1024 * 1024) {
                                "Izvoz ima previše priloga. Izaberi conversations.json iz ZIP-a. / Bitte conversations.json aus dem ZIP auswählen."
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
        } else result += parse(readLimited(buffered, MAX_JSON).toString(Charsets.UTF_8))
        val unique = result.distinctBy { it.id }.sortedByDescending { it.transcript.length }
        require(unique.isNotEmpty()) { "Nema tekstualnih ChatGPT razgovora / Keine ChatGPT-Textgespräche gefunden." }
        return unique
    }

    fun readLimited(input: InputStream, maximum: Int = MAX_JSON): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            require(output.size().toLong() + n <= maximum) { "Datoteka je prevelika / Datei zu groß." }
            output.write(buffer, 0, n)
        }
        return output.toByteArray()
    }

    fun parse(text: String): List<ChatConversation> {
        val clean = text.trim().removePrefix("\uFEFF")
        val array = if (clean.startsWith("[")) JSONArray(clean) else JSONArray().put(JSONObject(clean))
        val result = mutableListOf<ChatConversation>()
        for (i in 0 until array.length()) {
            val root = array.getJSONObject(i)
            val mapping = root.optJSONObject("mapping") ?: continue
            val nodes = mapping.keys().asSequence().toList()
            val leaf = root.optString("current_node").takeIf { mapping.has(it) }
                ?: nodes.maxByOrNull { mapping.optJSONObject(it)?.optJSONObject("message")?.optDouble("create_time", 0.0) ?: 0.0 }
                ?: continue
            val path = mutableListOf<JSONObject>()
            val seen = mutableSetOf<String>()
            var cursor: String? = leaf
            while (cursor != null && mapping.has(cursor) && seen.add(cursor)) {
                val node = mapping.getJSONObject(cursor)
                node.optJSONObject("message")?.let { path += it }
                cursor = node.optString("parent").takeUnless { it.isBlank() || it == "null" }
            }
            val messages = path.asReversed().mapNotNull { message ->
                val role = message.optJSONObject("author")?.optString("role") ?: return@mapNotNull null
                if (role !in listOf("user", "assistant") || message.optString("channel") == "analysis") return@mapNotNull null
                val content = message.optJSONObject("content") ?: return@mapNotNull null
                if (content.optString("content_type") !in listOf("text", "multimodal_text")) return@mapNotNull null
                val parts = content.optJSONArray("parts") ?: return@mapNotNull null
                val body = (0 until parts.length()).mapNotNull { parts.opt(it) as? String }.joinToString("\n").trim()
                if (body.isBlank()) null else "${if (role == "assistant") "PRIPOVJEDAČ" else "KORISNIK"}:\n$body"
            }
            val transcript = messages.joinToString("\n\n")
            require(transcript.length <= MAX_TRANSCRIPT) { "Razgovor je prevelik za ovaj uvoz / Gespräch zu groß für diesen Import." }
            if (transcript.isNotBlank()) result += ChatConversation(root.optString("id").ifBlank { "conversation-$i" },
                root.optString("title", "Naša porodična priča"), transcript, messages.size)
        }
        return result
    }

    fun chunks(text: String, maximum: Int = 18_000): List<String> {
        require(maximum > 0)
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = (start + maximum).coerceAtMost(text.length)
            if (end < text.length) {
                val boundary = text.lastIndexOf('\n', end - 1)
                if (boundary > start + maximum / 2) end = boundary + 1
            }
            chunks += text.substring(start, end)
            start = end
        }
        return chunks
    }
}
