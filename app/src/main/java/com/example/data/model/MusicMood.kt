package com.example.data.model

enum class MusicMood(val code: String, val labelBs: String, val labelDe: String, val emoji: String) {
    CALM("calm", "Mirna", "Ruhig", "🍃"),
    MAGIC("magic", "Čarobna", "Magisch", "✨"),
    ADVENTURE("adventure", "Pustolovna", "Abenteuerlich", "🧭"),
    MYSTERY("mystery", "Tajanstvena", "Geheimnisvoll", "🔍"),
    HAPPY("happy", "Vesela", "Fröhlich", "☀️"),
    LULLABY("lullaby", "Uspavanka", "Schlaflied", "🌙");

    companion object {
        fun fromCode(code: String?): MusicMood? = entries.find { it.code == code }
        fun forCategory(categoryId: String): MusicMood = when (categoryId) {
            "bedtime" -> LULLABY
            "mystery" -> MYSTERY
            "funny", "animals" -> HAPPY
            "space", "adventure", "dragons" -> ADVENTURE
            "magic", "fantasy", "princess", "surprise" -> MAGIC
            else -> CALM
        }
    }
}
