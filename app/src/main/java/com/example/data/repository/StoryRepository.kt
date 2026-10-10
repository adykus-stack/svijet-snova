package com.example.data.repository

import com.example.data.local.StoryDao
import com.example.data.local.StoryEntity
import com.example.data.model.*
import com.example.data.remote.GeminiService
import kotlinx.coroutines.flow.Flow

class StoryRepository(
    private val storyDao: StoryDao,
    private val geminiService: GeminiService = GeminiService()
) {
    val allSavedStories: Flow<List<StoryEntity>> = storyDao.getAllStories()

    suspend fun saveStory(story: ActiveStory): Long {
        val entity = StoryEntity.fromActiveStory(story)
        val savedId = storyDao.insertStory(entity)
        return savedId
    }

    suspend fun deleteStoryById(id: Long) {
        storyDao.deleteStoryById(id)
    }

    suspend fun getSavedStory(id: Long): ActiveStory? {
        val entity = storyDao.getStoryById(id) ?: return null
        return entity.toActiveStory()
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
