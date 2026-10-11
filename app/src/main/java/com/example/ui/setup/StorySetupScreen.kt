package com.example.ui.setup

import com.example.ui.localization.LocalAppLanguage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.MagicButton
import com.example.ui.theme.*
import kotlin.random.Random
import com.example.data.security.AiProvider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StorySetupScreen(
    config: StoryCreationConfig,
    onConfigChange: (StoryCreationConfig) -> Unit,
    onGenerateClick: () -> Unit,
    onBackClick: () -> Unit,
    isNightMode: Boolean,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    storyProvider: AiProvider = AiProvider.GEMINI,
    onStoryProviderChange: (AiProvider) -> Unit = {}
) {
    val ui = LocalAppLanguage.current
    val scrollState = rememberScrollState()

    fun randomizeImaginativeDetails() {
        val randomNamesBs = listOf("Zejd", "Hana", "Dino", "Lejla", "Tarik", "Ema", "Medo Brundo", "Mali Zvjezdan")
        val randomNamesDe = listOf("Mia", "Lukas", "Emma", "Noah", "Sophie", "Finn", "Bär Brummi", "Sternchen")
        val isDe = config.language == StoryLanguage.GERMAN

        val chosenName = if (isDe) randomNamesDe[Random.nextInt(randomNamesDe.size)]
        else randomNamesBs[Random.nextInt(randomNamesBs.size)]

        val randomHeroType = HeroType.entries[Random.nextInt(HeroType.entries.size)]

        val randomItemsBs = listOf("čarobni sat koji govori", "leteći plavi tepih", "zlatni ključ od oblaka", "svjetlucavi kompas", "drvo slatkiša")
        val randomItemsDe = listOf("eine sprechende Zauberuhr", "ein fliegender Teppich", "ein goldener Wolkenschlüssel", "ein Zauberkompass", "ein Wunschbaum")
        val chosenItem = if (isDe) randomItemsDe[Random.nextInt(randomItemsDe.size)]
        else randomItemsBs[Random.nextInt(randomItemsBs.size)]

        val randomMoralsBs = listOf("Prijateljstvo pobjeđuje svaku prepreku", "Hrabrost je u našem srcu", "Dijeljenje donosi radost", "Strpljenje i dobrota")
        val randomMoralsDe = listOf("Freundschaft überwindet jedes Hindernis", "Mut liegt im Herzen", "Teilen bringt Freude", "Geduld und Güte")
        val chosenMoral = if (isDe) randomMoralsDe[Random.nextInt(randomMoralsDe.size)]
        else randomMoralsBs[Random.nextInt(randomMoralsBs.size)]

        onConfigChange(
            config.copy(
                heroName = chosenName,
                heroType = randomHeroType,
                themeOrItem = chosenItem,
                targetMoral = chosenMoral
            )
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = ui.text("Prilagodi priču", "Geschichte anpassen"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("setup_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = ui.text("Nazad", "Zurück")
                        )
                    }
                },
                actions = { IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("open_settings")) {
                    Icon(Icons.Default.Settings, ui.text("Postavke", "Einstellungen"))
                } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isNightMode) DreamNightSurface else DreamDaySurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = if (isNightMode) DreamNightSurface else DreamDaySurface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    MagicButton(
                        text = ui.text("Stvori moju priču ✨", "Meine Geschichte erstellen ✨"),
                        onClick = onGenerateClick,
                        icon = Icons.Default.AutoAwesome,
                        gradientColors = listOf(MagicPurple, MagicPink),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_story_button")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Selected Category Badge
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = config.category.accentColor.copy(alpha = 0.15f)
                ),
                border = BorderStroke(1.dp, config.category.accentColor.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = config.category.emoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = ui.text("Izabrana kategorija:", "Ausgewählte Kategorie:"),
                            style = MaterialTheme.typography.labelSmall.copy(color = config.category.accentColor)
                        )
                        Text(
                            text = ui.text(config.category.titleBs, config.category.titleDe),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(ui.text("Ko stvara priču?", "Wer erstellt die Geschichte?"), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AiProvider.entries.forEach { provider ->
                    FilterChip(selected = storyProvider == provider, onClick = { onStoryProviderChange(provider) },
                        label = { Text(provider.label) }, modifier = Modifier.testTag("story_provider_${provider.name}"))
                }
            }
            Text(ui.text("Koristi spremljeni API ključ iz Postavki. Čitanje koristi tvoj početni glas, nezavisno od autora.",
                "Verwendet den gespeicherten API-Schlüssel aus den Einstellungen. Die Vorlesestimme ist unabhängig vom Autor."),
                style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(20.dp))

            // 1. AGE GROUP SELECTION
            Text(
                text = ui.text("Za koji uzrast stvaramo priču?", "Für welches Alter ist die Geschichte?"),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isNightMode) DreamNightText else DreamDayText
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            AgeGroupId.entries.forEach { age ->
                val isSelected = config.ageGroup == age
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MagicPurple.copy(alpha = if (isNightMode) 0.35f else 0.2f)
                        else if (isNightMode) DreamNightCard else DreamDaySurface
                    ),
                    border = if (isSelected) BorderStroke(2.dp, MagicPurple) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onConfigChange(config.copy(ageGroup = age)) }
                        .testTag("age_group_card_${age.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MagicPurple else MagicPurple.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = age.ageRange,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MagicPurple
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ui.text(age.label, "${age.ageRange} Jahre"),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MagicPurple else if (isNightMode) DreamNightText else DreamDayText
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ui.text(age.descriptionBs, age.descriptionDe),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isNightMode) DreamNightTextSec else DreamDayTextSec
                                )
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = ui.text("Izabrano", "Ausgewählt"),
                                tint = MagicPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. STORY LANGUAGE SELECTION
            Text(
                text = ui.text("Na kojem jeziku želiš svoju priču?", "In welcher Sprache möchtest du deine Geschichte?"),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isNightMode) DreamNightText else DreamDayText
                )
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StoryLanguage.entries.forEach { lang ->
                    val isSelected = config.language == lang
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MagicSky.copy(alpha = if (isNightMode) 0.35f else 0.2f)
                            else if (isNightMode) DreamNightCard else DreamDaySurface
                        ),
                        border = if (isSelected) BorderStroke(2.dp, MagicSky) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onConfigChange(config.copy(language = lang)) }
                            .testTag("lang_card_${lang.code}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = lang.flag, fontSize = 34.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (lang == StoryLanguage.GERMAN) "Deutsch" else ui.text("Bosanski jezik", "Bosnisch"),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MagicSky else if (isNightMode) DreamNightText else DreamDayText
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. OPTIONAL CUSTOMIZATION FIELDS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = ui.text("Dodatna podešavanja (opcionalno)", "Weitere Einstellungen (optional)"),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isNightMode) DreamNightText else DreamDayText
                    )
                )

                // "Prepusti sve mašti" magic button
                FilledTonalButton(
                    onClick = { randomizeImaginativeDetails() },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MagicGoldLight,
                        contentColor = Color(0xFF78350F)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("leave_to_imagination_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = ui.text("Prepusti mašti", "Überrasch mich"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hero Name Field
            OutlinedTextField(
                value = config.heroName,
                onValueChange = { onConfigChange(config.copy(heroName = it)) },
                label = { Text(ui.text("Ime glavnog junaka", "Name der Hauptfigur")) },
                placeholder = { Text(ui.text("npr. Luka, Hana, Zejd, Medo...", "z. B. Mia, Lukas, Emma, Bär...")) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hero_name_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Hero Type Selector
            Text(
                text = ui.text("Vrsta glavnog junaka:", "Art der Hauptfigur:"),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroType.entries.forEach { type ->
                    val isSelected = config.heroType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { onConfigChange(config.copy(heroType = type)) },
                        label = { Text("${type.emoji} ${ui.text(type.labelBs, type.labelDe)}") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("hero_type_${type.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Story Length Selector
            Text(
                text = ui.text("Željena dužina priče:", "Gewünschte Geschichtenlänge:"),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoryLength.entries.forEach { length ->
                    val isSelected = config.storyLength == length
                    FilterChip(
                        selected = isSelected,
                        onClick = { onConfigChange(config.copy(storyLength = length)) },
                        label = { Text(ui.text(length.labelBs, length.labelDe)) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("story_length_${length.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Special theme or item to include
            OutlinedTextField(
                value = config.themeOrItem,
                onValueChange = { onConfigChange(config.copy(themeOrItem = it)) },
                label = { Text(ui.text("Tema ili čarobni predmet koji treba uključiti", "Thema oder magischer Gegenstand")) },
                placeholder = { Text(ui.text("npr. leteći balon, čarobni fenjer, drvo želja...", "z. B. fliegender Ballon, Zauberlaterne, Wunschbaum...")) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_item_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Moral / Message
            OutlinedTextField(
                value = config.targetMoral,
                onValueChange = { onConfigChange(config.copy(targetMoral = it)) },
                label = { Text(ui.text("Pouka koju priča treba prenijeti", "Botschaft der Geschichte")) },
                placeholder = { Text(ui.text("npr. važnost dijeljenja, hrabrost, lijepo ponašanje...", "z. B. Teilen, Mut, Freundlichkeit...")) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("moral_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
