package com.example.data.importing

import android.content.Context
import android.util.AtomicFile
import com.example.data.security.AiProvider
import org.json.JSONObject
import java.io.File

data class ChronicleCheckpoint(val fingerprint: String, val provider: AiProvider, val completed: Int, val chronicle: String)

class ImportCheckpoint(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "chronicle-import.json"))
    fun read(): ChronicleCheckpoint? = runCatching {
        val json = JSONObject(file.openRead().use { it.readBytes().toString(Charsets.UTF_8) })
        ChronicleCheckpoint(json.getString("fingerprint"), AiProvider.valueOf(json.getString("provider")),
            json.getInt("completed"), json.getString("chronicle"))
    }.getOrNull()
    fun save(value: ChronicleCheckpoint) {
        val stream = file.startWrite()
        try {
            stream.write(JSONObject().put("fingerprint", value.fingerprint).put("provider", value.provider.name)
                .put("completed", value.completed).put("chronicle", value.chronicle).toString().toByteArray())
            file.finishWrite(stream)
        } catch (e: Exception) { file.failWrite(stream); throw e }
    }
    fun clear() = file.delete()
}
