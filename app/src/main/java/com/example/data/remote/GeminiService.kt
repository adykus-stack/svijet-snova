package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build(),
    private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY }
) {
    private class GeminiApiException(message: String) : java.io.IOException(message)

    // Text generation and translation model
    private val modelName = "gemini-3.8-flash"

    // Generating a complete chapter can take longer than a text request.
    private val speechClient = client.newBuilder()
        .readTimeout(180, TimeUnit.SECONDS)
        .callTimeout(240, TimeUnit.SECONDS)
        .build()

    suspend fun generateStoryChapter(
        config: StoryCreationConfig,
        existingChapters: List<Chapter> = emptyList(),
        continuationChoice: String? = null,
        isFinalChapter: Boolean = false
    ): Result<ChapterResponse> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Provide a high-quality fallback story generator for prototyping / offline usage
            return@withContext Result.success(
                FallbackStoryGenerator.generateFallbackChapter(
                    config = config,
                    chapterNum = existingChapters.size + 1,
                    continuationChoice = continuationChoice,
                    isFinalChapter = isFinalChapter
                )
            )
        }

        try {
            val prompt = buildStoryPrompt(config, existingChapters, continuationChoice, isFinalChapter)
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject().apply {
                    put("role", "user")
                    val partsArray = JSONArray()
                    partsArray.put(JSONObject().apply { put("text", prompt) })
                    put("parts", partsArray)
                }
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                val genConfig = JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.75)
                }
                put("generationConfig", genConfig)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

            val httpRequest = Request.Builder()
                .url(url)
                .header("x-goog-api-key", apiKey)
                .post(requestBody)
                .build()

            val responseBody = executeGeminiRequest(httpRequest, apiKey, client, "Gemini API")
            val parsedResponse = parseGeminiResponse(responseBody, existingChapters.size + 1, isFinalChapter, config.language == StoryLanguage.BOSNIAN)
            Result.success(parsedResponse)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: GeminiApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            // In case of network drops or timeout, fall back to safe local generator
            val fallback = FallbackStoryGenerator.generateFallbackChapter(
                config = config,
                chapterNum = existingChapters.size + 1,
                continuationChoice = continuationChoice,
                isFinalChapter = isFinalChapter
            )
            Result.success(fallback)
        }
    }

    suspend fun generateSpeech(
        text: String,
        language: StoryLanguage,
        voiceName: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API ključ nije postavljen u .env"))
        }

        try {
            val requestBody = GeminiSpeechProtocol.request(text, language, voiceName)
                .toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(GeminiSpeechProtocol.ENDPOINT)
                .header("x-goog-api-key", apiKey)
                .post(requestBody)
                .build()
            val responseBody = executeGeminiRequest(request, apiKey)
            Result.success(GeminiSpeechProtocol.parseAudio(responseBody))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun executeGeminiRequest(
        request: Request,
        apiKey: String,
        requestClient: OkHttpClient = speechClient,
        errorLabel: String = "Gemini TTS"
    ): String =
        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            val call = requestClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    if (continuation.isActive) continuation.resumeWith(Result.failure(e))
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use {
                        try {
                            val body = response.body?.string().orEmpty()
                            if (!response.isSuccessful) {
                                val message = runCatching {
                                    JSONObject(body).optJSONObject("error")?.optString("message")
                                }.getOrNull().orEmpty().replace(apiKey, "[skriven ključ]").take(300)
                                throw GeminiApiException(
                                    "$errorLabel (${response.code}): ${message.ifBlank { "Zahtjev nije uspio." }}"
                                )
                            }
                            if (continuation.isActive) continuation.resumeWith(Result.success(body))
                        } catch (e: Exception) {
                            if (continuation.isActive) continuation.resumeWith(Result.failure(e))
                        }
                    }
                }
            })
        }

    suspend fun translateStory(
        story: ActiveStory,
        targetLanguage: StoryLanguage
    ): Result<ActiveStory> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Local fallback translation
            return@withContext Result.success(
                FallbackStoryGenerator.translateLocally(story, targetLanguage)
            )
        }

        try {
            val prompt = buildTranslationPrompt(story, targetLanguage)
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject().apply {
                    put("role", "user")
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    }
                    put("parts", partsArray)
                }
                contentsArray.put(contentObj)
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.4)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent"

            val httpRequest = Request.Builder()
                .url(url)
                .header("x-goog-api-key", apiKey)
                .post(requestBody)
                .build()

            val body = executeGeminiRequest(httpRequest, apiKey, client, "Gemini API")
            val translatedStory = parseTranslationResponse(body, story, targetLanguage)
            Result.success(translatedStory)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.success(FallbackStoryGenerator.translateLocally(story, targetLanguage))
        }
    }

    private fun buildStoryPrompt(
        config: StoryCreationConfig,
        existingChapters: List<Chapter>,
        continuationChoice: String?,
        isFinalChapter: Boolean
    ): String {
        val langCode = config.language.code
        val isBosnian = config.language == StoryLanguage.BOSNIAN
        val primaryLangName = if (isBosnian) "bosanski" else "njemački (Deutsch)"
        val otherLangName = if (isBosnian) "njemački (Deutsch)" else "bosanski"
        val nextChapterNum = existingChapters.size + 1

        val heroDescription = buildString {
            if (config.heroName.isNotBlank()) append("Ime junaka: ${config.heroName}. ")
            append("Vrsta junaka: ${config.heroType.labelBs}. ")
            if (config.themeOrItem.isNotBlank()) append("Uključi detalj/predmet: ${config.themeOrItem}. ")
            if (config.targetMoral.isNotBlank()) append("Željena poruka: ${config.targetMoral}. ")
        }

        val historyContext = if (existingChapters.isNotEmpty()) {
            buildString {
                append("Dosadašnja poglavlja priče:\n")
                existingChapters.forEach { ch ->
                    append("Poglavlje ${ch.chapterNumber}: ${ch.chapterTitle}\n${ch.storyText}\n\n")
                }
                if (!continuationChoice.isNullOrBlank()) {
                    append("Korisnik je izabrao smjer za novo poglavlje: \"$continuationChoice\"\n")
                }
            }
        } else {
            "Ovo je prvo poglavlje potpuno nove priče."
        }

        val endingDirective = if (isFinalChapter) {
            "OVO JE ZAVRŠNO POGLAVLJE PRIČE. Radnja se mora lijepo, toplo i sretno zaokružiti. 'continuationOptions' u JSON-u treba biti prazna lista []."
        } else {
            """
            Na kraju ponudi TAČNO TRI (3) kratka, uzbudljiva i međusobno različita prijedloga za nastavak priče u polju 'continuationOptions'.
            DVOJEZIČNI PRIKAZ ZA SVAKU OPCIJU:
            Svaka opcija u listi 'continuationOptions' MORA biti JSON objekat sa dva polja:
            - 'primaryText': puni tekst opcije na primarnom jeziku priče ($primaryLangName).
            - 'translatedText': tačan i prirodan prijevod te iste opcije na drugom jeziku ($otherLangName).
            """.trimIndent()
        }

        return """
            Ti si najbolji dječiji pripovjedač na svijetu za aplikaciju „Svijet snova“.
            Tvoj zadatak je stvoriti čarobno, toplo i potpuno sigurno poglavlje dječije priče.

            PARAMETRI:
            - Jezik priče: $primaryLangName (jezički kod: '$langCode'). Cjelokupan tekst, naslovi, dijalozi, pouka i primarne opcije MORAJU biti na ovom jeziku!
            - Kategorija: ${config.category.titleBs} (${config.category.descriptionBs})
            - Ciljani uzrast: ${config.ageGroup.label} (${config.ageGroup.descriptionBs})
            - Željena dužina: ${config.storyLength.labelBs}
            - Detalji o junaku i svijetu: $heroDescription
            - Broj poglavlja koje pišeš: $nextChapterNum
            $endingDirective

            $historyContext

            PRAVILA ZA UZRAST:
            - Ako je uzrast 3–5: vrlo jednostavne, ritmične, nježne rečenice, tople riječi, puno ponavljanja i osjećaj sigurnosti.
            - Ako je uzrast 6–8: avantura, razigrani humor, veseli dijalozi, maštoviti prijatelji.
            - Ako je uzrast 9–12: zanimljiva zagonetka, dublji osjećaji, uzbudljivi preokret i živopisni opisi.

            SIGURNOSNA PRAVILA:
            - Apsolutno bez nasilja, straha, noćnih mora, povređivanja ili opasnosti.
            - Ljubaznost, saradnja, toplina, poštovanje i vedrina.
            - Podijeli priču na 3 do 5 preglednih, čitljivih odlomaka.
            - Za pozadinsku instrumentalnu muziku izaberi 'musicMood' prema emociji ovog poglavlja:
              calm, magic, adventure, mystery, happy ili lullaby. Za uspavljivanje uvijek lullaby.

            VRATI ISKLJUČIVO VALIDAN JSON OBJEKAT sa sljedećim poljima (bez ikakvog dodatnog teksta izvan JSON-a):
            {
              "title": "${if (existingChapters.isNotEmpty()) "isti naslov cjelokupne priče" else "Kreativan naslov priče"}",
              "chapterNumber": $nextChapterNum,
              "chapterTitle": "Naslov ovog poglavlja",
              "storyText": "Tekst poglavlja podijeljen u odlomke razdvojene sa dva nova reda (\n\n)...",
              "moral": "Kratka topla mudrost ili poruka ovog poglavlja (1 rečenica)",
              "musicMood": "magic",
              "continuationOptions": [
                {
                  "primaryText": "${if (isBosnian) "Luka odlučuje otvoriti čarobna vrata" else "Mia beschließt, die magische Tür zu öffnen"}",
                  "translatedText": "${if (isBosnian) "Luka beschließt, die magische Tür zu öffnen." else "Mia odlučuje otvoriti čarobna vrata."}"
                },
                {
                  "primaryText": "${if (isBosnian) "Prati svjetlucavog leptira prema šumi" else "Sie folgt dem leuchtenden Schmetterling"}",
                  "translatedText": "${if (isBosnian) "Sie folgt dem leuchtenden Schmetterling zum Wald." else "Prati svjetlucavog leptira prema šumi."}"
                },
                {
                  "primaryText": "${if (isBosnian) "Pita mudru sovu za savjet" else "Fragt die weise Eule um Rat"}",
                  "translatedText": "${if (isBosnian) "Fragt die weise Eule um Rat." else "Pita mudru sovu za savjet."}"
                }
              ]
            }
        """.trimIndent()
    }

    private fun buildTranslationPrompt(story: ActiveStory, targetLanguage: StoryLanguage): String {
        val targetName = if (targetLanguage == StoryLanguage.GERMAN) "njemački (Deutsch)" else "bosanski"
        val otherName = if (targetLanguage == StoryLanguage.GERMAN) "bosanski" else "njemački"
        val chaptersJson = JSONArray()
        story.chapters.forEach { ch ->
            val obj = JSONObject().apply {
                put("chapterNumber", ch.chapterNumber)
                put("chapterTitle", ch.chapterTitle)
                put("storyText", ch.storyText)
                put("moral", ch.moral)
                val optArr = JSONArray()
                ch.getEffectiveOptions().forEach { opt ->
                    val oObj = JSONObject()
                    oObj.put("primaryText", opt.primaryText)
                    oObj.put("translatedText", opt.translatedText)
                    optArr.put(oObj)
                }
                put("continuationOptions", optArr)
            }
            chaptersJson.put(obj)
        }

        return """
            Prevedi sljedeću dječiju priču pod nazivom "${story.title}" na $targetName.
            Sačuvaj sva imena likova, emociju, nježan dječiji stil, toplinu i značenje.
            Uzrast: ${story.config.ageGroup.label}.

            Originalni podaci:
            Naslov: "${story.title}"
            Poglavlja: $chaptersJson

            VRATI ISKLJUČIVO JSON OBJEKAT u formatu:
            {
              "translatedTitle": "Prevedeni naslov priče",
              "chapters": [
                {
                  "chapterNumber": 1,
                  "chapterTitle": "Prevedeni naslov poglavlja",
                  "storyText": "Prevedeni tekst poglavlja...",
                  "moral": "Prevedena pouka",
                  "continuationOptions": [
                    {
                      "primaryText": "Opcija prevedena na $targetName",
                      "translatedText": "Opcija na $otherName"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
    }

    private fun parseGeminiResponse(
        rawBody: String,
        fallbackChapterNum: Int,
        isFinalChapter: Boolean,
        isBosnianPrimary: Boolean
    ): ChapterResponse {
        val jsonRoot = JSONObject(rawBody)
        val candidates = jsonRoot.getJSONArray("candidates")
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")
        val text = parts.getJSONObject(0).getString("text")

        // Clean any accidental markdown json fencing
        val cleanJson = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val storyJson = JSONObject(cleanJson)
        val title = storyJson.optString("title", "Svijet snova")
        val chapterNum = storyJson.optInt("chapterNumber", fallbackChapterNum)
        val chapterTitle = storyJson.optString("chapterTitle", "Čarobno poglavlje $chapterNum")
        val storyText = storyJson.optString("storyText", "")
        val moral = storyJson.optString("moral", "Prijateljstvo i dobrota obasjavaju svaki dan.")

        val biOptionsList = mutableListOf<StoryOption>()
        val legacyOptionsList = mutableListOf<String>()

        if (!isFinalChapter) {
            val optArray = storyJson.optJSONArray("continuationOptions")
            if (optArray != null) {
                for (i in 0 until optArray.length()) {
                    val item = optArray.opt(i)
                    if (item is JSONObject) {
                        val primary = item.optString("primaryText", item.optString("text", "")).trim()
                        var translated = item.optString("translatedText", item.optString("translation", "")).trim()
                        if (primary.isNotBlank()) {
                            if (translated.isBlank()) {
                                translated = FallbackStoryGenerator.fallbackTranslateOption(primary, isBosnianPrimary)
                            }
                            biOptionsList.add(StoryOption(primaryText = primary, translatedText = translated))
                            legacyOptionsList.add(primary)
                        }
                    } else if (item is String && item.isNotBlank()) {
                        val primary = item.trim()
                        val translated = FallbackStoryGenerator.fallbackTranslateOption(primary, isBosnianPrimary)
                        biOptionsList.add(StoryOption(primaryText = primary, translatedText = translated))
                        legacyOptionsList.add(primary)
                    }
                }
            }
        }

        return ChapterResponse(
            title = title,
            chapter = Chapter(
                chapterNumber = chapterNum,
                chapterTitle = chapterTitle,
                storyText = storyText,
                moral = moral,
                continuationOptions = legacyOptionsList.take(3),
                bilingualOptions = biOptionsList.take(3),
                isFinalChapter = isFinalChapter,
                musicMood = MusicMood.fromCode(storyJson.optString("musicMood"))
            )
        )
    }

    private fun parseTranslationResponse(rawBody: String, originalStory: ActiveStory, targetLang: StoryLanguage): ActiveStory {
        val jsonRoot = JSONObject(rawBody)
        val candidates = jsonRoot.getJSONArray("candidates")
        val firstCandidate = candidates.getJSONObject(0)
        val text = firstCandidate.getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        val cleanJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val parsed = JSONObject(cleanJson)

        val newTitle = parsed.optString("translatedTitle", originalStory.title)
        val chaptersArray = parsed.optJSONArray("chapters")
        val newChapters = mutableListOf<Chapter>()
        val isTargetBosnian = targetLang == StoryLanguage.BOSNIAN

        if (chaptersArray != null) {
            for (i in 0 until chaptersArray.length()) {
                val obj = chaptersArray.getJSONObject(i)
                val optArr = obj.optJSONArray("continuationOptions")
                val biOpts = mutableListOf<StoryOption>()
                val legacyOpts = mutableListOf<String>()

                if (optArr != null) {
                    for (j in 0 until optArr.length()) {
                        val item = optArr.opt(j)
                        if (item is JSONObject) {
                            val p = item.optString("primaryText", item.optString("text", "")).trim()
                            var tr = item.optString("translatedText", item.optString("translation", "")).trim()
                            if (p.isNotBlank()) {
                                if (tr.isBlank()) tr = FallbackStoryGenerator.fallbackTranslateOption(p, isTargetBosnian)
                                biOpts.add(StoryOption(p, tr))
                                legacyOpts.add(p)
                            }
                        } else if (item is String && item.isNotBlank()) {
                            val p = item.trim()
                            val tr = FallbackStoryGenerator.fallbackTranslateOption(p, isTargetBosnian)
                            biOpts.add(StoryOption(p, tr))
                            legacyOpts.add(p)
                        }
                    }
                }
                newChapters.add(
                    Chapter(
                        chapterNumber = obj.optInt("chapterNumber", i + 1),
                        chapterTitle = obj.optString("chapterTitle", "Poglavlje ${i + 1}"),
                        storyText = obj.optString("storyText", ""),
                        moral = obj.optString("moral", ""),
                        continuationOptions = legacyOpts.take(3),
                        bilingualOptions = biOpts.take(3),
                        isFinalChapter = i == chaptersArray.length() - 1 && originalStory.chapters.getOrNull(i)?.isFinalChapter == true,
                        musicMood = originalStory.chapters.getOrNull(i)?.musicMood
                    )
                )
            }
        }

        val updatedConfig = originalStory.config.copy(language = targetLang)
        return originalStory.copy(
            title = newTitle,
            config = updatedConfig,
            chapters = if (newChapters.isNotEmpty()) newChapters else originalStory.chapters
        )
    }
}

data class ChapterResponse(
    val title: String,
    val chapter: Chapter
)
