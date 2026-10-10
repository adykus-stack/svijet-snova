package com.example.tts

/** Keeps paragraph boundaries while bounding long passages for Gemini and device TTS. */
internal object NarrationPlan {
    const val GAP_MS = 2000L
    const val MAX_SEGMENT_CHARS = 2400

    fun segments(text: String): List<String> =
        text.trim().split(Regex("\\r?\\n\\s*\\r?\\n"))
            .filter { it.isNotBlank() }
            .flatMap { paragraph ->
                val result = mutableListOf<String>()
                var remaining = paragraph.trim()
                while (remaining.length > MAX_SEGMENT_CHARS) {
                    val prefix = remaining.take(MAX_SEGMENT_CHARS)
                    val sentenceEnd = Regex("[.!?][\\s\"»”]").findAll(prefix)
                        .lastOrNull { it.range.last > MAX_SEGMENT_CHARS / 2 }?.range?.let { it.last + 1 }
                    val boundary = sentenceEnd ?: prefix.lastIndexOf(' ').takeIf { it > 0 }
                        ?: MAX_SEGMENT_CHARS
                    result += remaining.take(boundary).trim()
                    remaining = remaining.drop(boundary).trim()
                }
                if (remaining.isNotBlank()) result += remaining
                result
            }
}

data class NarrationProgress(val paragraph: Int = 0, val total: Int = 0, val inGap: Boolean = false)

data class MusicSettings(
    val enabled: Boolean = true,
    /** Maximum volume between paragraphs; speech automatically reduces it to one third. */
    val volume: Float = 0.30f,
    val selectedMood: com.example.data.model.MusicMood? = null
)
