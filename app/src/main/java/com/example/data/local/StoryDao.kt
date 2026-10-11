package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM saved_stories ORDER BY createdAt DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM saved_stories ORDER BY createdAt DESC")
    suspend fun snapshot(): List<StoryEntity>

    @Query("SELECT * FROM story_archive_parts WHERE storyId = :id ORDER BY partIndex")
    suspend fun archiveParts(id: Long): List<StoryArchivePart>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertArchive(parts: List<StoryArchivePart>)

    @Query("DELETE FROM story_archive_parts WHERE storyId = :id")
    suspend fun deleteArchive(id: Long)

    @Transaction
    suspend fun saveWithArchive(story: StoryEntity): Long {
        val record = story.copy(sourceArchive = "")
        val id = if (story.id > 0 && getStoryById(story.id) != null) {
            updateStory(record)
            story.id
        } else insertStory(record)
        if (story.sourceArchive.isNotBlank()) {
            deleteArchive(id)
            insertArchive(story.sourceArchive.chunked(100_000).mapIndexed { index, text -> StoryArchivePart(id, index, text) })
        }
        return id
    }

    @Transaction
    suspend fun snapshotWithArchives(): List<StoryEntity> = snapshot().map { story ->
        story.copy(sourceArchive = archiveParts(story.id).joinToString("") { it.text }.ifBlank { story.sourceArchive })
    }

    @Transaction
    suspend fun restoreStories(stories: List<StoryEntity>): Int {
        val existing = snapshot().map { Triple(it.title, it.createdAt, it.chaptersJson) }.toSet()
        val additions = stories.distinctBy { Triple(it.title, it.createdAt, it.chaptersJson) }
            .filter { Triple(it.title, it.createdAt, it.chaptersJson) !in existing }.map { it.copy(id = 0L) }
        additions.forEach { saveWithArchive(it) }
        return additions.size
    }

    @Query("SELECT * FROM saved_stories WHERE id = :id")
    suspend fun getStoryById(id: Long): StoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity): Long

    @Update
    suspend fun updateStory(story: StoryEntity)

    @Delete
    suspend fun deleteStory(story: StoryEntity)

    @Query("DELETE FROM saved_stories WHERE id = :id")
    suspend fun deleteStoryById(id: Long)
}
