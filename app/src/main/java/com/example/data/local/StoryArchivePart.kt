package com.example.data.local

import androidx.room.*

/** Large source conversations are split so Android cursor windows never hold one huge archive row. */
@Entity(tableName = "story_archive_parts", primaryKeys = ["storyId", "partIndex"],
    foreignKeys = [ForeignKey(entity = StoryEntity::class, parentColumns = ["id"], childColumns = ["storyId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("storyId")])
data class StoryArchivePart(val storyId: Long, val partIndex: Int, val text: String)
