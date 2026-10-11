package com.example.data.importing

import com.example.data.local.StoryEntity
import org.json.JSONArray
import org.json.JSONObject

/** Deliberate allow-list: no API credentials, PINs or account tokens enter a backup. */
object StoryBackup {
    fun encode(stories: List<StoryEntity>): String = JSONObject().put("format", "svijet-snova-backup")
        .put("version", 1).put("stories", JSONArray().apply {
            stories.forEach { s -> put(JSONObject().put("title", s.title).put("languageCode", s.languageCode)
                .put("categoryId", s.categoryId).put("ageGroupId", s.ageGroupId).put("heroName", s.heroName)
                .put("heroTypeId", s.heroTypeId).put("storyLengthId", s.storyLengthId).put("themeOrItem", s.themeOrItem)
                .put("targetMoral", s.targetMoral).put("chapters", JSONArray(s.chaptersJson)).put("createdAt", s.createdAt)
                .put("chronicle", s.chronicle).put("sourceArchive", s.sourceArchive).put("isSerial", s.isSerial)) }
        }).toString()

    fun decode(text: String): List<StoryEntity> {
        val root = JSONObject(text)
        require(root.optString("format") == "svijet-snova-backup" && root.optInt("version") == 1) {
            "Ovo nije sigurnosna kopija Svijeta snova / Keine Svijet-snova-Sicherung."
        }
        val entries = root.getJSONArray("stories")
        require(entries.length() <= 2000)
        return (0 until entries.length()).map { i ->
            val s = entries.getJSONObject(i)
            val chapters = s.getJSONArray("chapters")
            require(chapters.length() in 1..2000)
            for (j in 0 until chapters.length()) {
                require(chapters.getJSONObject(j).getString("storyText").isNotBlank())
            }
            val title = s.getString("title")
            require(title.isNotBlank() && title.length <= 500)
            StoryEntity(title = title, languageCode = s.getString("languageCode"), categoryId = s.getString("categoryId"),
                ageGroupId = s.getString("ageGroupId"), heroName = s.getString("heroName"), heroTypeId = s.getString("heroTypeId"),
                storyLengthId = s.getString("storyLengthId"), themeOrItem = s.getString("themeOrItem"), targetMoral = s.getString("targetMoral"),
                chaptersJson = chapters.toString(), createdAt = s.getLong("createdAt"), chronicle = s.optString("chronicle"),
                sourceArchive = s.optString("sourceArchive"), isSerial = s.optBoolean("isSerial"))
        }
    }
}
