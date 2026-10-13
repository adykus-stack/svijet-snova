package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.importing.*
import com.example.data.cloud.CloudPayloadCodec
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.*
import com.example.data.repository.StoryRepository
import com.example.data.security.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChronicleImportAndBackupTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private fun node(parent: String?, role: String, body: String, time: Int) = JSONObject()
        .put("parent", parent ?: JSONObject.NULL).put("message", JSONObject().put("create_time", time)
            .put("author", JSONObject().put("role", role)).put("content", JSONObject().put("content_type", "text")
                .put("parts", JSONArray().put(body))))
    private fun export() = JSONArray().put(JSONObject().put("id", "story").put("title", "Zejdova avantura").put("current_node", "end")
        .put("mapping", JSONObject().put("root", node(null, "system", "Secret instructions", 0))
            .put("first", node("root", "user", "Zejd i njegova sestra Hana.", 1))
            .put("old", node("first", "assistant", "Odbačena radnja sa zmajem.", 9))
            .put("end", node("first", "assistant", "Hana je pronašla plavi ključ.", 2))))

    @Test fun googlePayloadCodecPreservesLargeUnicodeBackups() {
        val original = buildString {
            repeat(20_000) { append("Leon, Hana i zmaj čuvaju mjesečevu kroniku — $it. 🐉🌙\n") }
        }
        val encoded = CloudPayloadCodec.encode(original)
        assertEquals(original, CloudPayloadCodec.decode(encoded))
        assertEquals(64, CloudPayloadCodec.sha256(encoded).length)
    }

    @Test fun activeBranchIsChronologicalAndDiscardedRepliesAreNotImported() {
        val conversation = ChatGptImport.parse(export().toString()).single()
        assertEquals(2, conversation.messageCount)
        assertTrue(conversation.transcript.indexOf("Zejd") < conversation.transcript.indexOf("plavi ključ"))
        assertFalse(conversation.transcript.contains("zmajem"))
        assertFalse(conversation.transcript.contains("Secret instructions"))
        assertEquals(conversation.fingerprint, ChatGptImport.parse(export().toString()).single().fingerprint)
    }

    @Test fun zipAndNumberedJsonImportUnicodeWithoutExtractingAttachments() {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("../unused.txt")); zip.write("unused".toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("folder/conversations_1.json")); zip.write(export().toString().toByteArray()); zip.closeEntry()
            zip.putNextEntry(ZipEntry("conversations-2.json")); zip.write(export().toString().toByteArray()); zip.closeEntry()
        }
        val conversation = ChatGptImport.read(bytes.toByteArray().inputStream()).single()
        assertTrue(conversation.transcript.contains("ključ"))
        assertEquals("Zejdova avantura", conversation.title)
        assertThrows(Exception::class.java) { ChatGptImport.read("Not JSON".byteInputStream()) }
        assertThrows(Exception::class.java) { ChatGptImport.readLimited(ByteArray(101).inputStream(), 100) }
    }

    @Test fun longConversationChunksDoNotDropAnyText() {
        val text = (1..1000).joinToString("\n\n") { "Dan $it: Hana je zapamtila put do čarobnog izvora." }
        val chunks = ChatGptImport.chunks(text, 800)
        assertTrue(chunks.size > 10)
        assertTrue(chunks.all { it.length <= 800 })
        assertEquals(text, chunks.joinToString(""))
    }

    private fun serial() = ActiveStory(title = "Zejdova avantura", config = StoryCreationConfig(category = StoryCategories.family),
        chapters = listOf(Chapter(1, "Ključ", "Hana je pronašla ključ.", "Dobrota.")), chronicle = "Hana je Zejdova sestra. Imaju plavi ključ.",
        sourceArchive = "Privatni originalni razgovor koji se ne šalje pri svakom nastavku.", isSerial = true)

    @Test fun backupsPreserveChronicleAndOriginalArchiveWithoutCredentialsAndPin() {
        val entity = StoryEntity.fromActiveStory(serial().copy(nextChapterGuidance = "Leon treba pronaći mjesečevu pećinu."))
        val text = StoryBackup.encode(listOf(entity))
        val restored = StoryBackup.decode(text).single().toActiveStory()
        assertTrue(restored.isSerial)
        assertEquals(serial().chronicle, restored.chronicle)
        assertEquals(serial().sourceArchive, restored.sourceArchive)
        assertEquals("family", restored.config.category.id)
        assertEquals(serial().chapters, restored.chapters)
        assertEquals("Leon treba pronaći mjesečevu pećinu.", restored.nextChapterGuidance)
        assertFalse(text.contains("verifier"))
        assertFalse(text.contains("api_key"))
        assertThrows(Exception::class.java) { StoryBackup.decode("{\"stories\":[]}") }
        val invalid = JSONObject(text).put("version", 99)
        assertThrows(Exception::class.java) { StoryBackup.decode(invalid.toString()) }
    }

    @Test fun databaseMigrationPreservesExistingStoriesAndRestoreIsAdditiveAndIdempotent() = runBlocking {
        val name = "test_chronicle_migration"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("CREATE TABLE saved_stories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, languageCode TEXT NOT NULL, categoryId TEXT NOT NULL, ageGroupId TEXT NOT NULL, heroName TEXT NOT NULL, heroTypeId TEXT NOT NULL, storyLengthId TEXT NOT NULL, themeOrItem TEXT NOT NULL, targetMoral TEXT NOT NULL, chaptersJson TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            val old = StoryEntity.fromActiveStory(serial())
            db.execSQL("INSERT INTO saved_stories VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", arrayOf(old.title, old.languageCode, "magic", old.ageGroupId,
                old.heroName, old.heroTypeId, old.storyLengthId, old.themeOrItem, old.targetMoral, old.chaptersJson, old.createdAt))
            db.version = 1
        }
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
        try {
            val dao = db.storyDao()
            val preserved = dao.getStoryById(1)!!
            assertEquals("Zejdova avantura", preserved.title)
            assertFalse(preserved.isSerial)
            assertEquals("", preserved.chronicle)
            assertEquals("", preserved.nextChapterGuidance)
            val imported = StoryEntity.fromActiveStory(serial()).copy(createdAt = 1234L)
            assertEquals(1, dao.restoreStories(listOf(imported)))
            assertEquals(0, dao.restoreStories(listOf(imported)))
            assertEquals(2, dao.snapshot().size)
            assertTrue(dao.snapshot().any { it.isSerial && it.chronicle == serial().chronicle })
            assertNotNull(dao.getStoryById(1))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun pinPreservesLeadingZeroIsHashedAndResetDoesNotTouchCredentials() {
        val name = "test_pin_store"
        context.deleteSharedPreferences(name)
        val store = ParentPinStore(context, name)
        assertFalse(store.exists())
        assertThrows(IllegalArgumentException::class.java) { store.set("123") }
        store.set("0246")
        assertTrue(store.verify("0246"))
        assertFalse(context.getSharedPreferences(name, 0).all.values.any { it == "0246" })
        assertFalse(store.verify("0000"))
        store.set("1357")
        assertFalse(store.verify("0246"))
        assertTrue(store.verify("1357"))
        repeat(5) { assertFalse(store.verify("0000")) }
        assertTrue(store.waitSeconds() > 0)
        context.deleteSharedPreferences(name)
    }

    private fun service(handler: (String) -> JSONObject): GeminiService {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val buffer = okio.Buffer(); chain.request().body!!.writeTo(buffer)
            val body = JSONObject(buffer.readUtf8())
            val prompt = body.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").getJSONObject(0).getString("text")
            val result = JSONObject().put("candidates", JSONArray().put(JSONObject().put("content", JSONObject()
                .put("parts", JSONArray().put(JSONObject().put("text", handler(prompt).toString()))))))
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("Test")
                .body(result.toString().toResponseBody()).build()
        }.build()
        return GeminiService(client, apiKeyProvider = { "test-key-not-real" })
    }

    @Test fun continuationUsesChronicleAndLastTwoChaptersAndReturnsUpdatedMemoryTogether() = runBlocking {
        val story = serial().copy(
            chapters = (1..5).map { Chapter(it, "Dan $it", "Događaj broj $it.", "Dobrota.") },
            nextChapterGuidance = "Leon treba pronaći mjesečevu pećinu, bez strašnih scena."
        )
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val service = service { prompt ->
            assertTrue(prompt.contains(story.chronicle))
            assertTrue(prompt.contains("Događaj broj 5"))
            assertFalse(prompt.contains("Događaj broj 1"))
            assertFalse(prompt.contains(story.sourceArchive))
            assertTrue(prompt.contains("SEDAM"))
            assertTrue(prompt.contains("ŽELJE RODITELJA ZA SLJEDEĆE POGLAVLJE"))
            assertTrue(prompt.contains(story.nextChapterGuidance))
            JSONObject().put("storyText", "Zejd je zajedno s Hanom otključao kapiju.").put("chapterTitle", "Kapija")
                .put("updatedChronicle", "Hana je Zejdova sestra. Plavi ključ je otvorio kapiju.")
        }
        try {
            val updated = StoryRepository(db.storyDao(), service).continueSerial(story)
            assertEquals(6, updated.chapters.size)
            assertEquals(6, updated.chapters.last().chapterNumber)
            assertTrue(updated.isSaved)
            assertEquals(updated.chronicle, db.storyDao().getStoryById(updated.id)!!.chronicle)
            assertEquals("", updated.nextChapterGuidance)
            assertEquals("", db.storyDao().getStoryById(updated.id)!!.nextChapterGuidance)
            assertEquals("Leon treba pronaći mjesečevu pećinu, bez strašnih scena.", story.nextChapterGuidance)
            assertEquals(5, story.chapters.size)
            assertFalse(updated.chapters.last().isFinalChapter)
        } finally { db.close() }
    }

    @Test fun failedContinuationKeepsSavedParentGuidance() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val draft = serial().copy(nextChapterGuidance = "Neka Hana upozna prijateljsku sovu.")
            val repo = StoryRepository(db.storyDao(), service {
                JSONObject().put("storyText", "Nedovršeno poglavlje bez kronike")
            })
            val id = repo.saveStory(draft)
            try { repo.continueSerial(draft.copy(id = id)); fail("Continuation must fail") }
            catch (_: JSONException) { }
            assertEquals(draft.nextChapterGuidance, db.storyDao().getStoryById(id)!!.nextChapterGuidance)
        } finally { db.close() }
    }

    @Test fun missingChronicleDoesNotSaveAnIncompleteContinuation() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = StoryRepository(db.storyDao(), service { JSONObject().put("storyText", "New story") })
            try { repo.continueSerial(serial()); fail("Missing memory must fail") } catch (_: JSONException) { }
            assertTrue(db.storyDao().snapshot().isEmpty())
        } finally { db.close() }
    }

    @Test fun checkpointSurvivesRestartAndSummaryKeepsOldAndNewFactsInThePrompt() = runBlocking {
        val checkpoint = ImportCheckpoint(context)
        try {
            val value = ChronicleCheckpoint("test-source", AiProvider.GEMINI, 2, "Hana je sestra.")
            checkpoint.save(value)
            assertEquals(value, ImportCheckpoint(context).read())
            val updated = service { prompt ->
                assertTrue(prompt.contains(value.chronicle)); assertTrue(prompt.contains("plavi ključ"))
                JSONObject().put("chronicle", "Hana je sestra i našla je plavi ključ.")
            }.updateChronicle(value.chronicle, "Hana je našla plavi ključ.")
            assertTrue(updated.contains("ključ"))
        } finally { checkpoint.clear() }
    }

    @Test fun completedImportStepsAreResumedWithoutSendingTheWholeConversationAgain() = runBlocking {
        val checkpoint = ImportCheckpoint(context)
        val conversation = ChatGptImport.parse(export().toString()).single()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        var requests = 0
        val service = service { prompt ->
            requests++
            assertTrue(prompt.contains("Hana je pronašla plavi ključ"))
            JSONObject().put("chapterTitle", "Dosadašnja avantura").put("storyText", "Hana i Zejd su pronašli plavi ključ.")
                .put("updatedChronicle", "Hana je Zejdova sestra. Imaju plavi ključ.")
        }
        try {
            checkpoint.save(ChronicleCheckpoint(conversation.fingerprint, AiProvider.GEMINI,
                ChatGptImport.chunks(conversation.transcript).size, "Hana je pronašla plavi ključ"))
            val imported = StoryRepository(db.storyDao(), service).importConversation(conversation, conversation.title,
                StoryCreationConfig(category = StoryCategories.family), AiProvider.GEMINI, checkpoint) { _, _, _ -> }
            assertEquals(1, requests)
            assertEquals(1, imported.chapters.size)
            assertEquals(conversation.transcript, imported.sourceArchive)
            assertTrue(imported.isSaved && imported.isSerial)
            assertEquals("family", imported.config.category.id)
            assertNull(checkpoint.read())
        } finally { checkpoint.clear(); db.close() }
    }

    @Test fun largeArchiveIsSplitAndPreservedWhenLibraryContinuationHasNoArchiveInMemory() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val repo = StoryRepository(db.storyDao())
            val original = serial().copy(sourceArchive = "Hana je našla ključ. ".repeat(100_000))
            val id = repo.saveStory(original)
            assertEquals("", db.storyDao().getStoryById(id)!!.sourceArchive)
            assertTrue(db.storyDao().archiveParts(id).all { it.text.length <= 100_000 })
            val fromLibrary = db.storyDao().getStoryById(id)!!.toActiveStory()
            repo.saveStory(fromLibrary.copy(chronicle = "Nova kronika."))
            assertEquals(original.sourceArchive, repo.getSavedStory(id)!!.sourceArchive)
            assertEquals(original.sourceArchive, StoryBackup.decode(repo.backup()).single().sourceArchive)
            repo.deleteStoryById(id)
            assertTrue(db.storyDao().archiveParts(id).isEmpty())
        } finally { db.close() }
    }
}
