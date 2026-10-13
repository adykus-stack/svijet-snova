package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class StoryLanguage(val code: String, val displayName: String, val flag: String) {
    BOSNIAN("bs", "Bosanski jezik", "🇧🇦"),
    GERMAN("de", "Deutsch", "🇩🇪");

    companion object {
        fun fromCode(code: String): StoryLanguage =
            if (code.equals("de", ignoreCase = true)) GERMAN else BOSNIAN
    }
}

enum class AgeGroupId(val id: String, val label: String, val ageRange: String, val descriptionBs: String, val descriptionDe: String) {
    AGE_3_5(
        id = "3-5",
        label = "3–5 godina",
        ageRange = "3–5",
        descriptionBs = "Jednostavne rečenice, puno ponavljanja, nježna radnja i kratka topla priča.",
        descriptionDe = "Sehr einfache Sätze, Wiederholungen, sanfte Handlung und kurze Gute-Nacht-Geschichte."
    ),
    AGE_6_8(
        id = "6-8",
        label = "6–8 godina",
        ageRange = "6–8",
        descriptionBs = "Više avanture, razigrani humor, dinamični dijalozi i maštoviti prijatelji.",
        descriptionDe = "Mehr Abenteuer, lustiger Humor, lebhafte Dialoge und phantasievolle Freunde."
    ),
    AGE_9_12(
        id = "9-12",
        label = "9–12 godina",
        ageRange = "9–12",
        descriptionBs = "Složeniji zaplet, dublji likovi, misterija, zagonetke i više uzbudljivih poglavlja.",
        descriptionDe = "Spannendere Handlung, tiefgründige Charaktere, Rätsel und abwechslungsreiche Kapitel."
    )
}

enum class HeroType(val id: String, val labelBs: String, val labelDe: String, val emoji: String) {
    CHILD("child", "Dijete", "Kind", "🧒"),
    ANIMAL("animal", "Životinja", "Tier", "🦊"),
    MAGICAL("magical", "Čarobno biće", "Zauberwesen", "🦄"),
    SURPRISE("surprise", "Iznenađenje", "Überraschung", "✨")
}

enum class StoryLength(val id: String, val labelBs: String, val labelDe: String, val targetMinutes: String) {
    SHORT("short", "Kratka priča", "Kurze Geschichte", "3 min"),
    MEDIUM("medium", "Srednja priča", "Mittlere Geschichte", "6 min"),
    LONG("long", "Duga priča", "Lange Geschichte", "10 min")
}

data class StoryCategory(
    val id: String,
    val titleBs: String,
    val titleDe: String,
    val emoji: String,
    val descriptionBs: String,
    val descriptionDe: String,
    val accentColor: Color
)

object StoryCategories {
    val family = StoryCategory("family", "Naša porodična priča", "Unsere Familiengeschichte", "📚",
        "Nastavi svoj svijet priče uz sačuvanu kroniku i poznate likove.",
        "Setze deine Geschichte mit Chronik und vertrauten Figuren fort.", CatMagic)
    val allCategories = listOf(
        StoryCategory(
            id = "magic",
            titleBs = "Čarobne avanture",
            titleDe = "Magische Abenteuer",
            emoji = "🪄",
            descriptionBs = "Čudesna putovanja kroz tajne svjetove, zvjezdane kapije i čarobne šume.",
            descriptionDe = "Wundervolle Reisen durch verborgene Welten und Zauberwälder.",
            accentColor = CatMagic
        ),
        StoryCategory(
            id = "princess",
            titleBs = "Princeze i kraljevstva",
            titleDe = "Prinzessinnen & Königreiche",
            emoji = "👑",
            descriptionBs = "Hrabre princeze, sjajni dvorci, plemenite misije i vjerni prijatelji.",
            descriptionDe = "Mutige Prinzessinnen, glänzende Schlösser und treue Gefährten.",
            accentColor = CatPrincess
        ),
        StoryCategory(
            id = "dragons",
            titleBs = "Zmajevi i čarobna bića",
            titleDe = "Drachen & Fabelwesen",
            emoji = "🐉",
            descriptionBs = "Dobroćudni zmajevi, vile, jednorozi i krilata stvorenja punog srca.",
            descriptionDe = "Freundliche Drachen, Feen, Einhörner und zauberhafte Fabelwesen.",
            accentColor = CatDragon
        ),
        StoryCategory(
            id = "animals",
            titleBs = "Životinje koje govore",
            titleDe = "Sprechende Tiere",
            emoji = "🦊",
            descriptionBs = "Mudra sova, radoznali zečić, veseli medo i njihove šumske zgode.",
            descriptionDe = "Kluge Eulen, neugierige Häschen und lustige Waldtiere.",
            accentColor = CatAnimal
        ),
        StoryCategory(
            id = "space",
            titleBs = "Svemirske pustolovine",
            titleDe = "Weltraumabenteuer",
            emoji = "🚀",
            descriptionBs = "Zvjezdane rakete, daleke šarene planete i prijateljski vanzemaljci.",
            descriptionDe = "Sternenraketen, bunte Planeten und freundliche Sternenfreunde.",
            accentColor = CatSpace
        ),
        StoryCategory(
            id = "mystery",
            titleBs = "Misterije i skrivena blaga",
            titleDe = "Geheimnisse & Schätze",
            emoji = "🗺️",
            descriptionBs = "Stare mape, zagonetne škrinje i uzbudljiva otkrića punih tajni.",
            descriptionDe = "Alte Schatzkarten, knifflige Rätsel und spannende Entdeckungen.",
            accentColor = CatMystery
        ),
        StoryCategory(
            id = "bedtime",
            titleBs = "Priče za laku noć",
            titleDe = "Gute-Nacht-Geschichten",
            emoji = "🌙",
            descriptionBs = "Nježne, tople i umirujuće priče uz mjesečinu za najslađe snove.",
            descriptionDe = "Sanfte, beruhigende Geschichten im Mondschein für süße Träume.",
            accentColor = CatBedtime
        ),
        StoryCategory(
            id = "funny",
            titleBs = "Smiješne priče",
            titleDe = "Lustige Geschichten",
            emoji = "🎭",
            descriptionBs = "Nestašne zgode, veseli preokreti i puno smijeha za dobro raspoloženje.",
            descriptionDe = "Lustige Missgeschicke, fröhliche Streiche und ganz viel Lachen.",
            accentColor = CatFunny
        ),
        StoryCategory(
            id = "moral",
            titleBs = "Poučne priče",
            titleDe = "Lehrreiche Geschichten",
            emoji = "🌱",
            descriptionBs = "Priče o prijateljstvu, dijeljenju, strpljenju, hrabrosti i ljubaznosti.",
            descriptionDe = "Geschichten über Freundschaft, Teilen, Mut und Hilfsbereitschaft.",
            accentColor = CatMoral
        ),
        StoryCategory(
            id = "surprise",
            titleBs = "Nasumično iznenađenje",
            titleDe = "Zufällige Überraschung",
            emoji = "🎁",
            descriptionBs = "Čarobna škrinja bira potpuno neočekivanu i jedinstvenu avanturu!",
            descriptionDe = "Die Zauberkiste wählt eine völlig unerwartete, tolle Überraschung!",
            accentColor = CatSurprise
        ),
        family
    )

    fun getById(id: String): StoryCategory =
        allCategories.find { it.id == id } ?: allCategories.first()
}

data class StoryOption(
    val primaryText: String,
    val translatedText: String = ""
)

data class GeminiVoice(
    val id: String,
    val displayName: String,
    val toneCategoryBs: String,
    val toneCategoryDe: String,
    val descriptionBs: String,
    val descriptionDe: String,
    val emoji: String,
    val provider: com.example.data.security.AiProvider = com.example.data.security.AiProvider.GEMINI
) {
    val name: String get() = id
}

object GeminiVoices {
    val VOICE_AOEDE = GeminiVoice(
        id = "Aoede",
        displayName = "Aoede",
        toneCategoryBs = "Topao i nježan",
        toneCategoryDe = "Warm und sanft",
        descriptionBs = "Nježan, topao i melodiozan glas, savršen za priče pred spavanje i mirne snove.",
        descriptionDe = "Sanfte, warme und melodische Stimme, perfekt für Gutenachtgeschichten.",
        emoji = "🌸"
    )

    val VOICE_PUCK = GeminiVoice(
        id = "Puck",
        displayName = "Puck",
        toneCategoryBs = "Veseo i razigran",
        toneCategoryDe = "Fröhlich und verspielt",
        descriptionBs = "Lagan, razigran i pun energije, idealan za smiješne i vesele pustolovine.",
        descriptionDe = "Leicht, verspielt und voller Energie, ideal für lustige Abenteuer.",
        emoji = "🎭"
    )

    val VOICE_CHARON = GeminiVoice(
        id = "Charon",
        displayName = "Charon",
        toneCategoryBs = "Smiren pripovjedač",
        toneCategoryDe = "Ruhiger Erzähler",
        descriptionBs = "Uravnotežen, jasan i mudar glas klasičnog pripovjedača za poučne priče.",
        descriptionDe = "Ausgewogene, klare und weise Erzählerstimme für lehrreiche Geschichten.",
        emoji = "📖"
    )

    val VOICE_FENRIR = GeminiVoice(
        id = "Fenrir",
        displayName = "Fenrir",
        toneCategoryBs = "Dubok i bajkovit",
        toneCategoryDe = "Tief und märchenhaft",
        descriptionBs = "Dublji, tajanstven i bogat ton za zmajeve, misterije i čarobna bića.",
        descriptionDe = "Tiefere, geheimnisvolle Stimme für Drachen, Wunder und Fabelwesen.",
        emoji = "🐉"
    )

    val VOICE_LEDA = GeminiVoice(
        id = "Leda",
        displayName = "Leda",
        toneCategoryBs = "Mladalački glas",
        toneCategoryDe = "Jugendliche Stimme",
        descriptionBs = "Vedri, prijateljski i mladalački ton pun radosti i dječije topline.",
        descriptionDe = "Helle, freundliche und jugendliche Stimme voller Freude.",
        emoji = "✨"
    )

    val VOICE_KORE = GeminiVoice(
        id = "Kore",
        displayName = "Kore",
        toneCategoryBs = "Staložen i nježan",
        toneCategoryDe = "Ruhig und sanft",
        descriptionBs = "Mekan, staložen i čist glas koji donosi osjećaj sigurnosti i mira.",
        descriptionDe = "Sanfte, besonnene und klare Stimme für Geborgenheit.",
        emoji = "🌙"
    )

    val openAiVoices = listOf("marin", "cedar", "coral", "nova", "onyx", "fable", "alloy", "ash", "ballad", "echo", "sage", "shimmer", "verse").map { name ->
        GeminiVoice("openai:$name", "OpenAI · ${name.replaceFirstChar { it.uppercase() }}", "Prirodni AI glas", "Natürliche KI-Stimme",
            "OpenAI glas za čitanje priča. Probaj izgovor na jeziku svoje priče.",
            "OpenAI-Stimme zum Vorlesen. Teste die Aussprache in deiner Sprache.", "🎙️", provider = com.example.data.security.AiProvider.OPENAI)
    }

    val allVoices = listOf(
        VOICE_AOEDE,
        VOICE_PUCK,
        VOICE_CHARON,
        VOICE_FENRIR,
        VOICE_LEDA,
        VOICE_KORE
    ) + openAiVoices

    val ALL_VOICES: List<GeminiVoice> get() = allVoices

    fun getById(id: String): GeminiVoice =
        allVoices.find { it.id.equals(id, ignoreCase = true) } ?: VOICE_AOEDE
}

data class Chapter(
    val chapterNumber: Int,
    val chapterTitle: String,
    val storyText: String,
    val moral: String,
    val continuationOptions: List<String> = emptyList(),
    val bilingualOptions: List<StoryOption> = emptyList(),
    val isFinalChapter: Boolean = false,
    val musicMood: MusicMood? = null
) {
    fun getEffectiveOptions(): List<StoryOption> {
        if (bilingualOptions.isNotEmpty()) return bilingualOptions
        return continuationOptions.map { StoryOption(primaryText = it, translatedText = "") }
    }
}

data class StoryCreationConfig(
    val category: StoryCategory = StoryCategories.allCategories.first(),
    val ageGroup: AgeGroupId = AgeGroupId.AGE_6_8,
    val language: StoryLanguage = StoryLanguage.BOSNIAN,
    val heroName: String = "",
    val heroType: HeroType = HeroType.CHILD,
    val storyLength: StoryLength = StoryLength.MEDIUM,
    val themeOrItem: String = "",
    val targetMoral: String = ""
)

data class ActiveStory(
    val id: Long = 0L,
    val title: String,
    val config: StoryCreationConfig,
    val chapters: List<Chapter>,
    val currentChapterIndex: Int = 0,
    val isSaved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val chronicle: String = "",
    val sourceArchive: String = "",
    val isSerial: Boolean = false,
    val nextChapterGuidance: String = ""
)
