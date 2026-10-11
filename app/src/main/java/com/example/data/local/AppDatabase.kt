package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [StoryEntity::class, StoryArchivePart::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storyDao(): StoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE saved_stories ADD COLUMN chronicle TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_stories ADD COLUMN sourceArchive TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_stories ADD COLUMN isSerial INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS story_archive_parts (storyId INTEGER NOT NULL, partIndex INTEGER NOT NULL, text TEXT NOT NULL, PRIMARY KEY(storyId, partIndex), FOREIGN KEY(storyId) REFERENCES saved_stories(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_story_archive_parts_storyId ON story_archive_parts(storyId)")
            }
        }
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "svijet_snova_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
