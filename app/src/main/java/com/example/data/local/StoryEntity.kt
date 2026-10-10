package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "saved_stories")
data class StoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val languageCode: String,
    val categoryId: String,
    val ageGroupId: String,
    val heroName: String,
    val heroTypeId: String,
    val storyLengthId: String,
    val themeOrItem: String,
    val targetMoral: String,
    val chaptersJson: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toActiveStory(): ActiveStory {
        val parsedChapters = mutableListOf<Chapter>()
        try {
            val jsonArray = JSONArray(chaptersJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val optionsArray = obj.optJSONArray("continuationOptions")
                val options = mutableListOf<String>()
                if (optionsArray != null) {
                    for (j in 0 until optionsArray.length()) {
                        options.add(optionsArray.getString(j))
                    }
                }

                val biOptionsArray = obj.optJSONArray("bilingualOptions")
                val biOptions = mutableListOf<StoryOption>()
                if (biOptionsArray != null) {
                    for (j in 0 until biOptionsArray.length()) {
                        val biObj = biOptionsArray.getJSONObject(j)
                        val primary = biObj.optString("primaryText", "")
                        val translated = biObj.optString("translatedText", "")
                        if (primary.isNotBlank()) {
                            biOptions.add(StoryOption(primaryText = primary, translatedText = translated))
                        }
                    }
                }

                val finalBiOptions = if (biOptions.isNotEmpty()) {
                    biOptions
                } else {
                    options.map { StoryOption(primaryText = it, translatedText = "") }
                }

                parsedChapters.add(
                    Chapter(
                        chapterNumber = obj.optInt("chapterNumber", i + 1),
                        chapterTitle = obj.optString("chapterTitle", "Poglavlje ${i + 1}"),
                        storyText = obj.optString("storyText", ""),
                        moral = obj.optString("moral", ""),
                        continuationOptions = if (options.isNotEmpty()) options else finalBiOptions.map { it.primaryText },
                        bilingualOptions = finalBiOptions,
                        isFinalChapter = obj.optBoolean("isFinalChapter", false),
                        musicMood = MusicMood.fromCode(obj.optString("musicMood"))
                    )
                )
            }
        } catch (_: Exception) {}

        val category = StoryCategories.getById(categoryId)
        val age = AgeGroupId.entries.find { it.id == ageGroupId } ?: AgeGroupId.AGE_6_8
        val lang = StoryLanguage.fromCode(languageCode)
        val hero = HeroType.entries.find { it.id == heroTypeId } ?: HeroType.CHILD
        val length = StoryLength.entries.find { it.id == storyLengthId } ?: StoryLength.MEDIUM

        val config = StoryCreationConfig(
            category = category,
            ageGroup = age,
            language = lang,
            heroName = heroName,
            heroType = hero,
            storyLength = length,
            themeOrItem = themeOrItem,
            targetMoral = targetMoral
        )

        return ActiveStory(
            id = id,
            title = title,
            config = config,
            chapters = parsedChapters,
            currentChapterIndex = if (parsedChapters.isNotEmpty()) parsedChapters.size - 1 else 0,
            isSaved = true,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromActiveStory(story: ActiveStory): StoryEntity {
            val jsonArray = JSONArray()
            for (ch in story.chapters) {
                val obj = JSONObject()
                obj.put("chapterNumber", ch.chapterNumber)
                obj.put("chapterTitle", ch.chapterTitle)
                obj.put("storyText", ch.storyText)
                obj.put("moral", ch.moral)
                obj.put("isFinalChapter", ch.isFinalChapter)
                obj.put("musicMood", ch.musicMood?.code)

                val effectiveOptions = ch.getEffectiveOptions()
                val optArr = JSONArray()
                val biOptArr = JSONArray()

                for (opt in effectiveOptions) {
                    optArr.put(opt.primaryText)
                    val bObj = JSONObject().apply {
                        put("primaryText", opt.primaryText)
                        put("translatedText", opt.translatedText)
                    }
                    biOptArr.put(bObj)
                }
                obj.put("continuationOptions", optArr)
                obj.put("bilingualOptions", biOptArr)
                jsonArray.put(obj)
            }

            return StoryEntity(
                id = story.id,
                title = story.title,
                languageCode = story.config.language.code,
                categoryId = story.config.category.id,
                ageGroupId = story.config.ageGroup.id,
                heroName = story.config.heroName,
                heroTypeId = story.config.heroType.id,
                storyLengthId = story.config.storyLength.id,
                themeOrItem = story.config.themeOrItem,
                targetMoral = story.config.targetMoral,
                chaptersJson = jsonArray.toString(),
                createdAt = story.createdAt
            )
        }
    }
}
