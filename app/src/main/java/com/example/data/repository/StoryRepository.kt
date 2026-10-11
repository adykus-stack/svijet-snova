package com.example.data.repository

import com.example.data.local.StoryDao
import com.example.data.local.StoryEntity
import com.example.data.model.*
import com.example.data.remote.GeminiService
import kotlinx.coroutines.flow.Flow
import com.example.data.importing.*
import com.example.data.security.AiProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class StoryRepository(
    private val storyDao: StoryDao,
    private val geminiService: GeminiService = GeminiService()
) {
    val allSavedStories: Flow<List<StoryEntity>> = storyDao.getAllStories()

    suspend fun backup(): String = StoryBackup.encode(storyDao.snapshotWithArchives())
    suspend fun restore(text: String): Int = storyDao.restoreStories(StoryBackup.decode(text))

    suspend fun importConversation(conversation: ChatConversation, title: String, config: StoryCreationConfig,
        provider: AiProvider, checkpoint: ImportCheckpoint,
        onProgress: (Int, Int, String) -> Unit): ActiveStory {
        val chunks = ChatGptImport.chunks(conversation.transcript)
        val saved = checkpoint.read()?.takeIf { it.fingerprint == conversation.fingerprint && it.provider == provider && it.completed in 0..chunks.size }
        var chronicle = saved?.chronicle.orEmpty()
        val start = saved?.completed ?: 0
        for (i in start until chunks.size) {
            currentCoroutineContext().ensureActive()
            onProgress(i, chunks.size + 1, "Kronika / Chronik")
            chronicle = geminiService.updateChronicle(chronicle, chunks[i])
            currentCoroutineContext().ensureActive()
            checkpoint.save(ChronicleCheckpoint(conversation.fingerprint, provider, i + 1, chronicle))
            // Conservative sequential pacing, including between summarisation and recap.
            delay(12_000)
        }
        onProgress(chunks.size, chunks.size + 1, "Početno poglavlje / Erstes Kapitel")
        val initial = ActiveStory(title = title, config = config, chapters = emptyList(), chronicle = chronicle,
            sourceArchive = conversation.transcript, isSerial = true)
        val response = geminiService.generateSerialChapter(initial, recap = true)
        currentCoroutineContext().ensureActive()
        val story = initial.copy(chapters = listOf(response.chapter), chronicle = response.updatedChronicle!!)
        val id = saveStory(story)
        checkpoint.clear()
        return story.copy(id = id, isSaved = true)
    }

    suspend fun continueSerial(story: ActiveStory): ActiveStory {
        require(story.isSerial && story.chronicle.isNotBlank())
        val response = geminiService.generateSerialChapter(story)
        currentCoroutineContext().ensureActive()
        val updated = story.copy(chapters = story.chapters + response.chapter,
            currentChapterIndex = story.chapters.size, chronicle = response.updatedChronicle!!)
        val id = saveStory(updated)
        return updated.copy(id = id, isSaved = true)
    }

    suspend fun saveStory(story: ActiveStory): Long {
        val entity = StoryEntity.fromActiveStory(story)
        val savedId = storyDao.saveWithArchive(entity)
        return savedId
    }

    suspend fun deleteStoryById(id: Long) {
        storyDao.deleteStoryById(id)
    }

    suspend fun getSavedStory(id: Long): ActiveStory? {
        val entity = storyDao.getStoryById(id) ?: return null
        return entity.copy(sourceArchive = storyDao.archiveParts(id).joinToString("") { it.text }.ifBlank { entity.sourceArchive }).toActiveStory()
    }

    suspend fun generateFirstChapter(config: StoryCreationConfig): Result<ActiveStory> {
        val response = geminiService.generateStoryChapter(
            config = config,
            existingChapters = emptyList(),
            continuationChoice = null,
            isFinalChapter = false
        )

        return response.map { resp ->
            ActiveStory(
                id = 0L,
                title = resp.title,
                config = config,
                chapters = listOf(resp.chapter),
                currentChapterIndex = 0,
                isSaved = false
            )
        }
    }

    suspend fun continueStory(
        story: ActiveStory,
        continuationChoice: String,
        isFinalChapter: Boolean
    ): Result<ActiveStory> {
        val response = geminiService.generateStoryChapter(
            config = story.config,
            existingChapters = story.chapters,
            continuationChoice = continuationChoice,
            isFinalChapter = isFinalChapter
        )

        return response.map { resp ->
            val updatedChapters = story.chapters + resp.chapter
            story.copy(
                chapters = updatedChapters,
                currentChapterIndex = updatedChapters.size - 1
            )
        }
    }

    suspend fun translateStory(
        story: ActiveStory,
        targetLanguage: StoryLanguage
    ): Result<ActiveStory> {
        return geminiService.translateStory(story, targetLanguage)
    }
}
