package com.example.data.remote

import com.example.data.model.*

object FallbackStoryGenerator {

    fun generateFallbackChapter(
        config: StoryCreationConfig,
        chapterNum: Int,
        continuationChoice: String?,
        isFinalChapter: Boolean
    ): ChapterResponse {
        val isGerman = config.language == StoryLanguage.GERMAN
        val hero = if (config.heroName.isNotBlank()) config.heroName else {
            if (isGerman) "Mia" else "Zejd"
        }

        val item = if (config.themeOrItem.isNotBlank()) config.themeOrItem else {
            if (isGerman) "ein leuchtender Traumstern" else "svjetlucavi zvjezdani fenjer"
        }

        if (isGerman) {
            return generateGermanChapter(config, chapterNum, hero, item, continuationChoice, isFinalChapter)
        } else {
            return generateBosnianChapter(config, chapterNum, hero, item, continuationChoice, isFinalChapter)
        }
    }

    private fun generateBosnianChapter(
        config: StoryCreationConfig,
        chapterNum: Int,
        hero: String,
        item: String,
        continuationChoice: String?,
        isFinalChapter: Boolean
    ): ChapterResponse {
        val catId = config.category.id

        val storyTitle: String
        val chapterTitle: String
        val paragraphs: List<String>
        val moralText: String
        val options: List<StoryOption>

        if (chapterNum == 1) {
            when (catId) {
                "bedtime" -> {
                    storyTitle = "$hero i Mjesečeva uspavanka"
                    chapterTitle = "Zvjezdani šapat pred spavanje"
                    paragraphs = listOf(
                        "Večeras je nebo iznad malog gradića bilo obučeno u najnježniji plavi somot. $hero je kroz prozor posmatrao kako se male zvijezde pale jedna po jedna, poput sitnih svjetiljki na nebeskom svodu.",
                        "Odjednom, na prozorsku dasku sletio je mali srebrni leptirić. Nosio je sa sobom $item koji je nježno mirisao na vaniliju i toplo mlijeko. Leptirić je šapnuo: „Vrijeme je za putovanje u zemlju snova, gdje se sve brige pretvaraju u mekane jastuke.“",
                        "$hero se udobno smjestio pod topli pokrivač. Svjetlost fenjera bila je toliko mirna da su se i sve igračke na polici polako počele nasmiješeno spremati za san."
                    )
                    moralText = "Mir u srcu i topla misao donose najslađe snove."
                    options = listOf(
                        StoryOption(
                            primaryText = "$hero prati srebrnog leptirića do Mjesečevog dvorca od oblaka",
                            translatedText = "$hero folgt dem silbernen Falter zum Wolkenschloss des Mondes."
                        ),
                        StoryOption(
                            primaryText = "$hero zatvara oči i sluša pjesmu uspavane šume",
                            translatedText = "$hero schließt die Augen und lauscht dem Lied des schlafenden Waldes."
                        ),
                        StoryOption(
                            primaryText = "$hero ostavlja $item ispod jastuka i otkriva magična vrata",
                            translatedText = "$hero legt $item unter das Kissen und entdeckt eine magische Tür."
                        )
                    )
                }
                "animals" -> {
                    storyTitle = "$hero i Vesela šumska družina"
                    chapterTitle = "Tajna Zelenog luga"
                    paragraphs = listOf(
                        "U Zelenom lugu sunce je radosno provirivalo kroz krošnje starih hrastova. $hero je šetao stazom kada se iz obližnjeg žbuna začuo tihi smijeh.",
                        "Bio je to zečić sa plavom mašnom koji je nosio $item! „Hej, $hero!“, veselo je uzviknuo zečić. „Danas biramo najljepšu pjesmu šume, ali nam nedostaje neko s velikim i radoznalim srcem.“",
                        "Uskoro su im se pridružili mudra sova i medvjedić Brundo. Svi su bili oduševljeni što su upoznali novog prijatelja."
                    )
                    moralText = "Iskreni osmijeh i lijepa riječ otvaraju svako prijateljstvo."
                    options = listOf(
                        StoryOption(
                            primaryText = "$hero i zečić kreću na vrh brda gdje mudra sova čuva staru pjesmaricu",
                            translatedText = "$hero und das Häschen gehen zum Hügel, wo die Eule das alte Liederbuch hütet."
                        ),
                        StoryOption(
                            primaryText = "Družina priprema piknik s borovnicama i čarobnim kolačićima",
                            translatedText = "Die Freunde bereiten ein Picknick mit Blaubeeren und Zauberkeksen vor."
                        ),
                        StoryOption(
                            primaryText = "$hero pronalazi skrivenu stazu koja vodi do muzičkog potočića",
                            translatedText = "$hero findet einen verborgenen Pfad, der zum klingenden Bächlein führt."
                        )
                    )
                }
                "dragons" -> {
                    storyTitle = "$hero i Dobroćudni zmaj Iskrica"
                    chapterTitle = "Prijatelj sa plavim krilima"
                    paragraphs = listOf(
                        "Iza visokih Šarenih planina živio je mali zmaj po imenu Iskrica. Za razliku od strašnih zmajeva iz starih knjiga, Iskrica je umjesto vatre puhao tople, šarene mjehuriće od sapunice!",
                        "$hero je jednog sunčanog jutra pronašao Iskricu kako tužno sjedi pored bistrog jezera, držeći $item.",
                        "„Izgubio sam svoj najsjajniji mjehurić“, uzdahnuo je Iskrica. $hero mu je prišao bez imalo straha, pružio mu ruku i rekao: „Ne brini, zajedno ćemo ga pronaći!“"
                    )
                    moralText = "Prava hrabrost znači pružiti ruku nekome ko je usamljen."
                    options = listOf(
                        StoryOption(
                            primaryText = "$hero i Iskrica polete iznad oblaka u potragu za sjajnim mjehurićem",
                            translatedText = "$hero und Funke fliegen über die Wolken auf der Suche nach der glänzenden Seifenblase."
                        ),
                        StoryOption(
                            primaryText = "Zajedno pitaju šumsku vilu Zlatokosu za pomoć",
                            translatedText = "Gemeinsam bitten sie die Waldfee Goldlöckchen um Hilfe."
                        ),
                        StoryOption(
                            primaryText = "Iskrica pokazuje skrivenu pećinu punu kristala koji svijetle",
                            translatedText = "Funke zeigt eine geheime Höhle voller leuchtender Kristalle."
                        )
                    )
                }
                else -> {
                    storyTitle = "$hero i Čarobna avantura Svijeta snova"
                    chapterTitle = "Početak neobičnog putovanja"
                    paragraphs = listOf(
                        "Bilo je to jedno sasvim posebno popodne kada je $hero u starom tavanu otkrio nešto nevjerovatno — $item koji je tiho zujuckao i isijavao toplu zlatnu svjetlost.",
                        "Čim je $hero dotaknuo ovaj čarobni predmet, zid sobe se pretvorio u prozor sa pogledom na beskrajna smaragdna polja i nebo puno letećih balona.",
                        "„Tvoja mašta je ključ ovog svijeta“, začuo se prijatan glas. $hero je osjetio kako mu srce kuca od radosti i uzbuđenja za avanturu koja tek počinje."
                    )
                    moralText = "Mašta nas može odvesti na najljepša mjesta na svijetu."
                    options = listOf(
                        StoryOption(
                            primaryText = "$hero hrabro zakorači kroz čarobni prozor na smaragdno polje",
                            translatedText = "$hero tritt mutig durch das Zauberfenster auf die smaragdgrüne Wiese."
                        ),
                        StoryOption(
                            primaryText = "$hero pita neobičnog vodiča kamo vodi zlatna staza",
                            translatedText = "$hero fragt den Begleiter, wohin der goldene Pfad führt."
                        ),
                        StoryOption(
                            primaryText = "$hero otvara malu drvenu škrinju koja se pojavila pred njim",
                            translatedText = "$hero öffnet eine kleine Holztruhe, die vor ihm erschienen ist."
                        )
                    )
                }
            }
        } else {
            // Continuation chapter
            storyTitle = "$hero u Svijetu snova"
            chapterTitle = if (isFinalChapter) "Sretan povratak i topli dom" else "Poglavlje $chapterNum: Novi korak avanture"

            val direction = continuationChoice ?: "Junak nastavlja svoj put pun nade"
            if (isFinalChapter) {
                paragraphs = listOf(
                    "$hero je uspješno riješio sve zagonetke i pomogao svim dragim bićima koje je sreo na putu. $item je sada sjao najljepšim, mirnim sjajem zahvalnosti.",
                    "„Hvala ti, $hero!“, rekli su svi prijatelji u glas, mašući s osmijehom. „Pokazao si da su dobrota, ljubav i strpljenje najveće čarolije koje postoje.“",
                    "Sa toplim osmijehom na licu, $hero se vratio u svoju sigurnu sobu. Ležeći u krevetu, znao je da će ovaj čudesni svijet uvijek živjeti u njegovom srcu i snovima."
                )
                moralText = "Najljepša čarolija na svijetu je ona koju stvaramo svojom dobrotom."
                options = emptyList()
            } else {
                paragraphs = listOf(
                    "Slijedeći smjer: „$direction“, $hero je napravio hrabar korak naprijed. Svaki korak ostavljao je trag sitnih svjetlucavih iskrica na stazi.",
                    "Uskoro se pred njim ukazao predivan prizor. Prijatelji su ga dočekali sa radošću, a $item je počeo vibrirati toplom melodijom koja je svima unijela mir.",
                    "Ipak, pred njima se nalazila još jedna uzbudljiva tajna koju je trebalo otkriti."
                )
                moralText = "Kada koračamo s povjerenjem, svaka staza nas vodi do cilja."
                options = listOf(
                    StoryOption(
                        primaryText = "$hero otvara vrata koja vode do riznice snova",
                        translatedText = "$hero öffnet die Tür, die zur Schatzkammer der Träume führt."
                    ),
                    StoryOption(
                        primaryText = "$hero pomaže malom prijatelju da pronađe svoj izgubljeni osmijeh",
                        translatedText = "$hero hilft dem kleinen Freund, sein verlorenes Lächeln wiederzufinden."
                    ),
                    StoryOption(
                        primaryText = "$hero uzima magični dvogled i posmatra Zvjezdani toranj",
                        translatedText = "$hero nimmt das Zauberfernglas und betrachtet den Sternenturm."
                    )
                )
            }
        }

        return ChapterResponse(
            title = storyTitle,
            chapter = Chapter(
                chapterNumber = chapterNum,
                chapterTitle = chapterTitle,
                storyText = paragraphs.joinToString("\n\n"),
                moral = moralText,
                continuationOptions = if (isFinalChapter) emptyList() else options.map { it.primaryText },
                bilingualOptions = if (isFinalChapter) emptyList() else options,
                isFinalChapter = isFinalChapter,
                musicMood = MusicMood.forCategory(config.category.id)
            )
        )
    }

    private fun generateGermanChapter(
        config: StoryCreationConfig,
        chapterNum: Int,
        hero: String,
        item: String,
        continuationChoice: String?,
        isFinalChapter: Boolean
    ): ChapterResponse {
        val catId = config.category.id

        val storyTitle: String
        val chapterTitle: String
        val paragraphs: List<String>
        val moralText: String
        val options: List<StoryOption>

        if (chapterNum == 1) {
            when (catId) {
                "bedtime" -> {
                    storyTitle = "$hero und das Wiegenlied der Sterne"
                    chapterTitle = "Das sanfte Flüstern der Nacht"
                    paragraphs = listOf(
                        "Heute Abend hüllte sich der Himmel über der kleinen Stadt in samtiges Nachtblau. $hero schaute aus dem Fenster, wie die funkelnden Sterne einer nach dem anderen erwachten.",
                        "Plötzlich landete ein kleiner silberner Falter auf der Fensterbank. Er brachte $item mit, der ganz sanft nach warmer Milch und Honig duftete. Der Falter flüsterte: „Komm mit ins Traumland, wo alle Gedanken weich wie Kissen werden.“",
                        "$hero kuschelte sich tief unter die warme Decke. Das goldene Licht brachte Ruhe in das ganze Zimmer und ließ alle Spielsachen friedlich schlafen."
                    )
                    moralText = "Mit Ruhe im Herzen und einem lieben Gedanken schläft es sich am besten."
                    options = listOf(
                        StoryOption(
                            primaryText = "$hero folgt dem silbernen Falter zum Wolkenschloss des Mondes",
                            translatedText = "$hero prati srebrnog leptirića do Mjesečevog dvorca od oblaka."
                        ),
                        StoryOption(
                            primaryText = "$hero lauscht dem leisen Gutenachtlied der schlafenden Bäume",
                            translatedText = "$hero sluša tihu uspavanku uspavanog drveća."
                        ),
                        StoryOption(
                            primaryText = "$hero legt $item unter das Kopfkissen und entdeckt eine geheime Traumtür",
                            translatedText = "$hero stavlja $item pod jastuk i otkriva tajna vrata snova."
                        )
                    )
                }
                else -> {
                    storyTitle = "$hero und das Zauberreich der Träume"
                    chapterTitle = "Der Beginn einer wunderbaren Reise"
                    paragraphs = listOf(
                        "Es war ein ganz besonderer Tag, als $hero etwas Magisches fand: $item, der geheimnisvoll summte und ein goldenes Licht ausstrahlte.",
                        "Als $hero den Gegenstand berührte, verwandelte sich die Wand in ein Tor zu einem zauberhaften Land mit bunten Himmelsbrücken und sprechenden Tieren.",
                        "„Deine Fantasie ist der Schlüssel zu allem Guten“, ertönte eine freundliche Stimme. Mit klopfendem Herzen und voller Vorfreude begann das Abenteuer."
                    )
                    moralText = "Fantasie und ein gutes Herz öffnen jede geheime Tür."
                    options = listOf(
                        StoryOption(
                            primaryText = "$hero betritt mutig das zauberhafte Tor",
                            translatedText = "$hero hrabro stupa kroz čarobnu kapiju."
                        ),
                        StoryOption(
                            primaryText = "$hero fragt einen kleinen Waldelfen nach dem Weg",
                            translatedText = "$hero pita malog šumskog vilenjaka za put."
                        ),
                        StoryOption(
                            primaryText = "$hero öffnet eine glitzernde Schatulle am Wegesrand",
                            translatedText = "$hero otvara svjetlucavu kutijicu pored puta."
                        )
                    )
                }
            }
        } else {
            storyTitle = "$hero im Traumland"
            chapterTitle = if (isFinalChapter) "Das glückliche Ende der Reise" else "Kapitel $chapterNum: Das nächste Abenteuer"
            val direction = continuationChoice ?: "Die Reise geht weiter"

            if (isFinalChapter) {
                paragraphs = listOf(
                    "$hero hatte alle Rätsel gelöst und jedem geholfen, der Hilfe brauchte. $item leuchtete nun in voller, wärmender Pracht.",
                    "„Danke, $hero!“, riefen alle Freunde im Chor und winkten fröhlich. „Du hast gezeigt, dass Freundschaft und Mut die größte Magie sind.“",
                    "Zufrieden und glücklich schloss $hero die Augen, wissend, dass dieses Wunder immer im Herzen weiterlebt."
                )
                moralText = "Die schönste Zauberei ist die Liebe und Güte, die wir miteinander teilen."
                options = emptyList()
            } else {
                paragraphs = listOf(
                    "Mit dem Entschluss: „$direction“ machte $hero den nächsten Schritt. Goldener Sternenstaub leuchtete unter den Füßen auf.",
                    "Die neuen Gefährten lächelten ermutigend. Mit $item in der Hand schien jede Hürde spielend leicht zu werden.",
                    "Doch vor ihnen lag noch ein weiteres kleines Geheimnis, das gelüftet werden wollte."
                )
                moralText = "Schritt für Schritt führt jeder mutige Weg zum Ziel."
                options = listOf(
                    StoryOption(
                        primaryText = "$hero öffnet die Pforte zum Turm der Sternensucher",
                        translatedText = "$hero otvara vrata tornja tražitelja zvijezda."
                    ),
                    StoryOption(
                        primaryText = "$hero hilft einem kleinen Häschen bei der Suche nach seiner Laterne",
                        translatedText = "$hero pomaže malom zečiću da pronađe svoj fenjer."
                    ),
                    StoryOption(
                        primaryText = "$hero teilt die Zauberbeeren mit allen Gefährten",
                        translatedText = "$hero dijeli čarobne bobice sa svim prijateljima."
                    )
                )
            }
        }

        return ChapterResponse(
            title = storyTitle,
            chapter = Chapter(
                chapterNumber = chapterNum,
                chapterTitle = chapterTitle,
                storyText = paragraphs.joinToString("\n\n"),
                moral = moralText,
                continuationOptions = if (isFinalChapter) emptyList() else options.map { it.primaryText },
                bilingualOptions = if (isFinalChapter) emptyList() else options,
                isFinalChapter = isFinalChapter
            )
        )
    }

    fun fallbackTranslateOption(text: String, isPrimaryBosnian: Boolean): String {
        val clean = text.trim()
        if (clean.isBlank()) return ""

        return if (isPrimaryBosnian) {
            when {
                clean.contains("otvara", ignoreCase = true) && clean.contains("vrata", ignoreCase = true) ->
                    "Öffnet die magische Tür"
                clean.contains("prati", ignoreCase = true) && clean.contains("leptir", ignoreCase = true) ->
                    "Folgt dem leuchtenden Schmetterling"
                clean.contains("zmaj", ignoreCase = true) ->
                    "Fliegt mit dem freundlichen Drachen"
                clean.contains("sova", ignoreCase = true) || clean.contains("knjig", ignoreCase = true) ->
                    "Sucht Rat bei der weisen Eule"
                clean.contains("prijatelj", ignoreCase = true) ->
                    "Hilft den Freunden auf dem Weg"
                clean.contains("staz", ignoreCase = true) || clean.contains("put", ignoreCase = true) ->
                    "Folgt dem goldenen Pfad"
                clean.contains("škrinj", ignoreCase = true) || clean.contains("kutij", ignoreCase = true) ->
                    "Öffnet die geheimnisvolle Schatulle"
                clean.contains("zvijezd", ignoreCase = true) || clean.contains("mjesec", ignoreCase = true) ->
                    "Reist zu den funkelnden Sternen"
                clean.contains("šum", ignoreCase = true) ->
                    "Erkundet den zauberhaften Wald"
                else -> "Weiteres Abenteuer entdecken"
            }
        } else {
            when {
                clean.contains("Tür", ignoreCase = true) || clean.contains("Tor", ignoreCase = true) || clean.contains("Pforte", ignoreCase = true) ->
                    "Otvara čarobna vrata"
                clean.contains("Schmetterling", ignoreCase = true) || clean.contains("Falter", ignoreCase = true) ->
                    "Prati svjetlucavog leptira"
                clean.contains("Drache", ignoreCase = true) ->
                    "Leti s dobroćudnim zmajem"
                clean.contains("Eule", ignoreCase = true) || clean.contains("Buch", ignoreCase = true) ->
                    "Pita mudru sovu za savjet"
                clean.contains("Freund", ignoreCase = true) || clean.contains("Häschen", ignoreCase = true) ->
                    "Pomaže novim prijateljima"
                clean.contains("Pfad", ignoreCase = true) || clean.contains("Weg", ignoreCase = true) ->
                    "Prati zlatnu stazu"
                clean.contains("Schatulle", ignoreCase = true) || clean.contains("Truhe", ignoreCase = true) ->
                    "Otvara zagonetnu škrinju"
                clean.contains("Stern", ignoreCase = true) || clean.contains("Mond", ignoreCase = true) ->
                    "Putuje prema sjajnim zvijezdama"
                clean.contains("Wald", ignoreCase = true) ->
                    "Istražuje čarobnu šumu"
                else -> "Otkrij sljedeću pustolovinu"
            }
        }
    }

    fun translateLocally(story: ActiveStory, targetLang: StoryLanguage): ActiveStory {
        val isTargetGerman = targetLang == StoryLanguage.GERMAN
        val newTitle = if (isTargetGerman) {
            "Die Traumgeschichte von ${story.config.heroName.ifBlank { "Mia" }}"
        } else {
            "Svijet snova: Priča za ${story.config.heroName.ifBlank { "Zejda" }}"
        }

        val translatedChapters = story.chapters.map { ch ->
            val effective = ch.getEffectiveOptions()
            val translatedOptions = effective.map { opt ->
                if (isTargetGerman) {
                    val newPrimary = if (opt.translatedText.isNotBlank()) opt.translatedText else fallbackTranslateOption(opt.primaryText, true)
                    val newSecondary = opt.primaryText
                    StoryOption(primaryText = newPrimary, translatedText = newSecondary)
                } else {
                    val newPrimary = if (opt.translatedText.isNotBlank()) opt.translatedText else fallbackTranslateOption(opt.primaryText, false)
                    val newSecondary = opt.primaryText
                    StoryOption(primaryText = newPrimary, translatedText = newSecondary)
                }
            }

            if (isTargetGerman) {
                Chapter(
                    chapterNumber = ch.chapterNumber,
                    chapterTitle = "Kapitel ${ch.chapterNumber}: ${ch.chapterTitle}",
                    storyText = ch.storyText,
                    moral = "Güte und Freundschaft leuchten in jedem Herzen.",
                    continuationOptions = translatedOptions.map { it.primaryText },
                    bilingualOptions = translatedOptions,
                    isFinalChapter = ch.isFinalChapter,
                    musicMood = ch.musicMood
                )
            } else {
                Chapter(
                    chapterNumber = ch.chapterNumber,
                    chapterTitle = "Poglavlje ${ch.chapterNumber}: ${ch.chapterTitle}",
                    storyText = ch.storyText,
                    moral = "Dobrota i prijateljstvo donose najljepše snove.",
                    continuationOptions = translatedOptions.map { it.primaryText },
                    bilingualOptions = translatedOptions,
                    isFinalChapter = ch.isFinalChapter,
                    musicMood = ch.musicMood
                )
            }
        }

        return story.copy(
            title = newTitle,
            config = story.config.copy(language = targetLang),
            chapters = translatedChapters
        )
    }
}
