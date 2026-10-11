package com.example.ui.reader

import com.example.ui.localization.LocalAppLanguage

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveStory
import com.example.data.model.Chapter
import com.example.data.model.GeminiVoice
import com.example.data.model.GeminiVoices
import com.example.data.model.StoryLanguage
import com.example.data.model.MusicMood
import com.example.tts.MusicSettings
import com.example.tts.NarrationProgress
import com.example.tts.AudioEngineType
import com.example.tts.TtsState
import com.example.ui.components.MagicButton
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import com.example.data.security.AiProvider

enum class StoryFontSize(val label: String, val fontSize: TextUnit, val lineHeight: TextUnit) {
    MEDIUM("Srednje", 18.sp, 28.sp),
    LARGE("Veliko", 22.sp, 34.sp),
    EXTRA_LARGE("Vrlo veliko", 26.sp, 40.sp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryReaderScreen(
    story: ActiveStory,
    ttsState: TtsState,
    audioEngine: AudioEngineType,
    currentVoice: GeminiVoice,
    ttsErrorMessage: String?,
    isPreviewingVoice: Boolean,
    hasUserSelectedVoiceEver: Boolean,
    onPlayTts: (String, String, MusicMood) -> Unit,
    musicSettings: MusicSettings,
    narrationProgress: NarrationProgress,
    onMusicEnabledChange: (Boolean) -> Unit,
    onMusicVolumeChange: (Float) -> Unit,
    onMusicSelectionChange: (MusicMood?) -> Unit,
    onPauseTts: () -> Unit,
    onResumeTts: () -> Unit,
    onStopTts: () -> Unit,
    onSelectVoice: (GeminiVoice) -> Unit,
    onPreviewVoice: (GeminiVoice, String) -> Unit,
    onStopPreviewVoice: () -> Unit,
    onSaveStory: () -> Unit,
    onNewStoryClick: () -> Unit,
    onContinueStoryWithOption: (String) -> Unit,
    onFinishStoryClick: () -> Unit,
    onTranslateStoryClick: () -> Unit,
    isNightMode: Boolean,
    onOpenSettings: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onContinueSerial: () -> Unit = {},
    onSaveChronicle: (String) -> Unit = {},
    storyProvider: AiProvider = AiProvider.GEMINI,
    onStoryProviderChange: (AiProvider) -> Unit = {}
) {
    val ui = LocalAppLanguage.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var showVoicePickerSheet by remember { mutableStateOf(false) }
    var showChronicle by remember { mutableStateOf(false) }
    var selectedFontSize by remember { mutableStateOf(StoryFontSize.MEDIUM) }
    var showCustomContinuationField by remember { mutableStateOf(false) }
    var customContinuationText by remember { mutableStateOf("") }
    var currentChapterIndex by remember(story.chapters.size) {
        mutableIntStateOf(story.chapters.size - 1)
    }

    val currentChapter: Chapter? = story.chapters.getOrNull(currentChapterIndex)
        ?: story.chapters.lastOrNull()
    val chapterMusicMood = currentChapter?.musicMood ?: MusicMood.forCategory(story.config.category.id)

    // Full text of the current chapter for TTS or clipboard
    val chapterFullText = buildString {
        if (currentChapter != null) {
            append("${currentChapter.chapterTitle}.\n\n")
            append("${currentChapter.storyText}\n\n")
            append(if (story.config.language == StoryLanguage.GERMAN) "Botschaft: ${currentChapter.moral}" else "Pouka: ${currentChapter.moral}")
        }
    }

    // The title stays with the first paragraph, instead of creating its own music break.
    val chapterSpeechText = if (currentChapter == null) "" else
        "${currentChapter.chapterTitle}.\n${currentChapter.storyText}\n\n" +
            if (story.config.language == StoryLanguage.GERMAN) "Botschaft: ${currentChapter.moral}"
            else "Pouka: ${currentChapter.moral}"

    fun copyToClipboard() {
        clipboardManager.setText(AnnotatedString("${story.title}\n\n$chapterFullText"))
        Toast.makeText(context, ui.text("Priča je kopirana u međuspremnik!", "Die Geschichte wurde in die Zwischenablage kopiert!"), Toast.LENGTH_SHORT).show()
    }

    fun shareStory() {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, story.title)
            putExtra(Intent.EXTRA_TEXT, "✨ ${story.title} ✨\n\n$chapterFullText\n\n— ${ui.text("Stvoreno u aplikaciji Svijet snova", "Erstellt mit Traumwelt")}")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, ui.text("Podijeli priču", "Geschichte teilen"))
        context.startActivity(shareIntent)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = story.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = ui.text("Nazad", "Zurück"))
                    }
                },
                actions = {
                    // Translate button
                    IconButton(
                        onClick = onTranslateStoryClick,
                        modifier = Modifier.testTag("translate_story_button")
                    ) {
                        Text(
                            text = if (story.config.language == StoryLanguage.BOSNIAN) "🇩🇪" else "🇧🇦",
                            fontSize = 20.sp
                        )
                    }

                    // Settings: theme, interface language and parent controls.
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = ui.text("Postavke", "Einstellungen"),
                            tint = if (isNightMode) MagicGold else MagicPurple
                        )
                    }

                    // Save story
                    IconButton(
                        onClick = onSaveStory,
                        modifier = Modifier.testTag("save_story_button")
                    ) {
                        Icon(
                            imageVector = if (story.isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                            contentDescription = ui.text("Sačuvaj priču", "Geschichte speichern"),
                            tint = if (story.isSaved) MagicMint else MagicPurple
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isNightMode) DreamNightSurface else DreamDaySurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp)
        ) {
            // Chapter Selection Bar (if multiple chapters exist)
            if (story.chapters.size > 1) {
                item {
                    Text(
                        text = ui.text("Poglavlja priče:", "Kapitel der Geschichte:"),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isNightMode) MagicGoldLight else MagicPurple
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(story.chapters) { idx, ch ->
                            val isCurrent = idx == currentChapterIndex
                            FilterChip(
                                selected = isCurrent,
                                onClick = {
                                    onStopTts()
                                    currentChapterIndex = idx
                                    onStopTts()
                                },
                                label = { Text(ui.text("Poglavlje ${ch.chapterNumber}", "Kapitel ${ch.chapterNumber}")) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MagicPurple,
                                    selectedLabelColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("chapter_tab_${ch.chapterNumber}")
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // Book Card Container
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNightMode) DreamNightSurface else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    border = BorderStroke(
                        1.5.dp,
                        if (isNightMode) MagicPurple.copy(alpha = 0.4f) else MagicGoldLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Decorative Chapter Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = story.config.category.accentColor.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(text = story.config.category.emoji, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = ui.text("Poglavlje ${currentChapter?.chapterNumber ?: 1} od ${story.chapters.size}", "Kapitel ${currentChapter?.chapterNumber ?: 1} von ${story.chapters.size}"),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = story.config.category.accentColor
                                        )
                                    )
                                }
                            }

                            // Language and Age badge
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MagicPurple.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = story.config.language.flag,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 14.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MagicSky.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = ui.text(story.config.ageGroup.label, "${story.config.ageGroup.ageRange} Jahre"),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MagicSky
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Chapter Title
                        Text(
                            text = currentChapter?.chapterTitle ?: ui.text("Poglavlje", "Kapitel"),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isNightMode) MagicGoldLight else MagicPurple
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Voice Indicator Banner & Selector Chip
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (audioEngine != AudioEngineType.OFFLINE_DEVICE) {
                                MagicPurple.copy(alpha = if (isNightMode) 0.25f else 0.12f)
                            } else {
                                MagicMint.copy(alpha = if (isNightMode) 0.25f else 0.15f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (audioEngine != AudioEngineType.OFFLINE_DEVICE) MagicPurple.copy(alpha = 0.35f) else MagicMint.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showVoicePickerSheet = true }
                                .testTag("voice_selector_trigger")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (audioEngine != AudioEngineType.OFFLINE_DEVICE) currentVoice.emoji else "📱",
                                        fontSize = 18.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (audioEngine != AudioEngineType.OFFLINE_DEVICE) {
                                                ui.text("${currentVoice.provider.label} AI glas: ${currentVoice.displayName}", "${currentVoice.provider.label}-KI-Stimme: ${currentVoice.displayName}")
                                            } else {
                                                ui.text("Offline glas uređaja", "Offline-Gerätestimme")
                                            },
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (audioEngine != AudioEngineType.OFFLINE_DEVICE) {
                                                    if (isNightMode) MagicGoldLight else MagicPurple
                                                } else {
                                                    if (isNightMode) MagicMintLight else Color(0xFF065F46)
                                                }
                                            )
                                        )
                                        Text(
                                            text = if (audioEngine != AudioEngineType.OFFLINE_DEVICE) {
                                                ui.text(currentVoice.toneCategoryBs, currentVoice.toneCategoryDe)
                                            } else {
                                                ui.text("Standardni TTS uređaja (bez interneta)", "Gerätestimme (ohne Internet)")
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = if (isNightMode) DreamNightTextSec else DreamDayTextSec
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MagicPurple.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = ui.text("Promijeni glas", "Stimme ändern"),
                                            tint = MagicPurple,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = ui.text("Promijeni", "Ändern"),
                                            maxLines = 1,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MagicPurple
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        MusicControls(
                            settings = musicSettings,
                            automaticMood = chapterMusicMood,
                            progress = narrationProgress,
                            onEnabledChange = onMusicEnabledChange,
                            onVolumeChange = onMusicVolumeChange,
                            onSelectionChange = onMusicSelectionChange
                        )

                        if (!ttsErrorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📢", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = ttsErrorMessage,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF92400E)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Reader Toolbar: TTS Read Aloud + Font Sizer
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isNightMode) DreamNightCard else DreamDayCard.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Text to Speech Multi-State Controls
                                when (ttsState) {
                                    TtsState.IDLE -> {
                                        Button(
                                            onClick = {
                                                onPlayTts(chapterSpeechText, story.config.language.code, chapterMusicMood)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MagicPurple),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.testTag("tts_play_stop_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = ui.text("Čitanje naglas", "Vorlesen"),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = ui.text("Pročitaj naglas", "Vorlesen"),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }

                                    TtsState.PREPARING -> {
                                        FilledTonalButton(
                                            onClick = onStopTts,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.testTag("tts_play_stop_button")
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MagicPurple
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = ui.text("Priprema glasa...", "Stimme wird vorbereitet..."),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }

                                    TtsState.PLAYING -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = onPauseTts,
                                                colors = ButtonDefaults.buttonColors(containerColor = MagicPurple),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.testTag("tts_pause_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Pause,
                                                    contentDescription = ui.text("Pauziraj", "Pausieren"),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(ui.text("Pauza", "Pause"), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            }

                                            Button(
                                                onClick = onStopTts,
                                                colors = ButtonDefaults.buttonColors(containerColor = MagicPink),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.testTag("tts_play_stop_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Stop,
                                                    contentDescription = ui.text("Zaustavi", "Stoppen"),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(ui.text("Zaustavi", "Stoppen"), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }

                                    TtsState.PAUSED -> {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = onResumeTts,
                                                colors = ButtonDefaults.buttonColors(containerColor = MagicMint),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.testTag("tts_resume_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = ui.text("Nastavi", "Fortsetzen"),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(ui.text("Nastavi", "Fortsetzen"), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            }

                                            Button(
                                                onClick = onStopTts,
                                                colors = ButtonDefaults.buttonColors(containerColor = MagicPink),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier.testTag("tts_play_stop_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Stop,
                                                    contentDescription = ui.text("Zaustavi", "Stoppen"),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(ui.text("Zaustavi", "Stoppen"), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }

                                    TtsState.ERROR -> {
                                        Button(
                                            onClick = { onPlayTts(chapterSpeechText, story.config.language.code, chapterMusicMood) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.testTag("tts_play_stop_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = ui.text("Pokušaj ponovo", "Erneut versuchen"),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(ui.text("Pokušaj ponovo", "Erneut versuchen"), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }

                                // Font size selector
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "A",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { selectedFontSize = StoryFontSize.MEDIUM }
                                            .padding(6.dp),
                                        color = if (selectedFontSize == StoryFontSize.MEDIUM) MagicPurple else Color.Gray
                                    )
                                    Text(
                                        text = "A",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { selectedFontSize = StoryFontSize.LARGE }
                                            .padding(6.dp),
                                        color = if (selectedFontSize == StoryFontSize.LARGE) MagicPurple else Color.Gray
                                    )
                                    Text(
                                        text = "A",
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .clickable { selectedFontSize = StoryFontSize.EXTRA_LARGE }
                                            .padding(6.dp),
                                        color = if (selectedFontSize == StoryFontSize.EXTRA_LARGE) MagicPurple else Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Story Paragraphs
                        val paragraphs = currentChapter?.storyText?.split("\n\n") ?: emptyList()
                        paragraphs.forEachIndexed { pIdx, paragraph ->
                            if (paragraph.isNotBlank()) {
                                Text(
                                    text = paragraph.trim(),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = selectedFontSize.fontSize,
                                        lineHeight = selectedFontSize.lineHeight,
                                        color = if (isNightMode) DreamNightText else DreamDayText
                                    )
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                            }
                        }

                        // Moral of the story card
                        if (!currentChapter?.moral.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MagicGoldLight.copy(alpha = if (isNightMode) 0.2f else 0.45f)
                                ),
                                border = BorderStroke(1.5.dp, MagicGold),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🌟", fontSize = 28.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = ui.text("Pouka priče:", "Botschaft der Geschichte:"),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MagicGold
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = currentChapter?.moral ?: "",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isNightMode) DreamNightText else Color(0xFF78350F)
                                            )
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Utility Action Buttons: Copy, Share, Scroll to Top
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { copyToClipboard() },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("copy_story_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = ui.text("Kopiraj", "Kopieren"),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(ui.text("Kopiraj", "Kopieren"), fontSize = 13.sp)
                                }

                                OutlinedButton(
                                    onClick = { shareStory() },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("share_story_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = ui.text("Podijeli", "Teilen"),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(ui.text("Podijeli", "Teilen"), fontSize = 13.sp)
                                }
                            }

                            // Scroll to top button
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(0)
                                    }
                                },
                                modifier = Modifier.testTag("scroll_to_top_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerticalAlignTop,
                                    contentDescription = ui.text("Na vrh", "Nach oben"),
                                    tint = MagicPurple
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Continuation Section (or Story Finished Celebration)
            item {
                Spacer(modifier = Modifier.height(24.dp))

                val isLatestChapter = currentChapterIndex == story.chapters.size - 1
                val isStoryConcluded = currentChapter?.isFinalChapter == true

                if (story.isSerial && isLatestChapter) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(ui.text("Sljedeće večernje poglavlje", "Das nächste Abendkapitel"), style = MaterialTheme.typography.titleLarge)
                        Text(ui.text("AI nastavlja radnju uz sačuvanu kroniku i poznate likove. Oko 7 minuta čitanja; nastavak se automatski sprema.",
                            "Die KI setzt die Handlung mit Chronik und vertrauten Figuren fort. Etwa 7 Minuten Vorlesezeit; das Kapitel wird automatisch gespeichert."))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AiProvider.entries.forEach { provider -> FilterChip(storyProvider == provider,
                                onClick = { onStoryProviderChange(provider) }, label = { Text(provider.label) },
                                modifier = Modifier.testTag("serial_provider_${provider.name}")) }
                        }
                        Button(onClick = onContinueSerial, modifier = Modifier.fillMaxWidth().testTag("continue_serial_story")) {
                            Text(ui.text("Nastavi radnju ✨", "Handlung fortsetzen ✨"))
                        }
                        OutlinedButton(onClick = { showChronicle = true }, modifier = Modifier.fillMaxWidth().testTag("open_chronicle")) {
                            Text(ui.text("Pregledaj i ispravi kroniku", "Chronik prüfen und korrigieren"))
                        }
                    }
                } else if (isStoryConcluded) {
                    // Celebration Completed Card
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MagicMintLight.copy(alpha = if (isNightMode) 0.2f else 0.4f)
                        ),
                        border = BorderStroke(1.5.dp, MagicMint),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🎉 ✨ 🌙", fontSize = 34.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = ui.text("Čestitamo! Priča je završena.", "Geschafft! Die Geschichte ist zu Ende."),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MagicMint
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = ui.text("Ova čarobna avantura ima sretan i miran završetak. Možeš je sačuvati u svojoj biblioteci ili stvoriti potpuno novu priču!", "Dieses magische Abenteuer hat ein glückliches und friedliches Ende. Speichere es in deiner Bibliothek oder erstelle eine neue Geschichte!"),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onSaveStory,
                                    colors = ButtonDefaults.buttonColors(containerColor = MagicMint),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (story.isSaved) ui.text("Sačuvano ✓", "Gespeichert ✓") else ui.text("Sačuvaj priču", "Geschichte speichern"))
                                }

                                OutlinedButton(
                                    onClick = onNewStoryClick,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(ui.text("Nova priča ✨", "Neue Geschichte ✨"))
                                }
                            }
                        }
                    }
                } else if (isLatestChapter) {
                    // "Šta bi se moglo dogoditi dalje?" Interactive Options Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNightMode) DreamNightCard else DreamDayCard
                        ),
                        border = BorderStroke(1.5.dp, MagicPurple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🔮", fontSize = 26.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = ui.text("Šta bi se moglo dogoditi dalje?", "Wie könnte es weitergehen?"),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNightMode) MagicGoldLight else MagicPurple
                                        )
                                    )
                                    Text(
                                        text = ui.text("Izaberi smjer nastavka ili unesi svoju ideju:", "Wähle eine Fortsetzung oder gib deine eigene Idee ein:"),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isNightMode) DreamNightTextSec else DreamDayTextSec
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 3 AI Options (Dvojezični prikaz)
                            currentChapter?.getEffectiveOptions()?.forEachIndexed { optIdx, option ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isNightMode) DreamNightSurface else Color.White,
                                    border = BorderStroke(1.dp, MagicPurple.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onContinueStoryWithOption(option.primaryText) }
                                        .testTag("continuation_option_$optIdx")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(MagicPurple.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${optIdx + 1}",
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MagicPurple
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            // Glavni tekst na primarnom jeziku priče
                                            Text(
                                                text = option.primaryText,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isNightMode) DreamNightText else DreamDayText
                                                )
                                            )

                                            // Prijevod na drugom jeziku (manjim crvenim kurzivnim slovima)
                                            if (option.translatedText.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = option.translatedText,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 12.5.sp,
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                        fontWeight = FontWeight.Normal,
                                                        color = if (isNightMode) Color(0xFFFF8585) else Color(0xFFC5221F)
                                                    )
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            tint = MagicPurple,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 4th Option: "Imam svoju ideju"
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isNightMode) DreamNightSurface else Color.White,
                                border = BorderStroke(1.dp, MagicGold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCustomContinuationField = !showCustomContinuationField }
                                    .testTag("custom_idea_toggle")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "💡", fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = ui.text("Imam svoju ideju...", "Ich habe eine eigene Idee..."),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNightMode) MagicGoldLight else Color(0xFF78350F)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (showCustomContinuationField) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MagicGold
                                    )
                                }
                            }

                            // Custom Continuation Text Field
                            AnimatedVisibility(visible = showCustomContinuationField) {
                                Column(modifier = Modifier.padding(top = 10.dp)) {
                                    OutlinedTextField(
                                        value = customContinuationText,
                                        onValueChange = { customContinuationText = it },
                                        placeholder = { Text(ui.text("Napiši šta želiš da se desi sljedeće...", "Schreibe, was als Nächstes passieren soll...")) },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("custom_idea_input"),
                                        maxLines = 3
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            if (customContinuationText.isNotBlank()) {
                                                onContinueStoryWithOption(customContinuationText.trim())
                                                customContinuationText = ""
                                                showCustomContinuationField = false
                                            }
                                        },
                                        enabled = customContinuationText.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = MagicGold),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("submit_custom_idea_button")
                                    ) {
                                        Text(ui.text("Nastavi s mojom idejom ✨", "Mit meiner Idee fortsetzen ✨"), color = Color.White)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // "Završi priču" button
                            FilledTonalButton(
                                onClick = onFinishStoryClick,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MagicMintLight,
                                    contentColor = Color(0xFF065F46)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("finish_story_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = ui.text("Završi priču sretnim krajem", "Geschichte mit einem glücklichen Ende abschließen"),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                } else {
                    // Viewing an earlier chapter: Offer button to jump to latest chapter
                    Button(
                        onClick = {
                            onStopTts()
                            currentChapterIndex = story.chapters.size - 1
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("jump_to_latest_chapter_button")
                    ) {
                        Text(ui.text("Skoči na najnovije poglavlje (${story.chapters.size}) ➔", "Zum neuesten Kapitel (${story.chapters.size}) ➔"))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Quick Action Buttons: "Nova priča" & "Pročitaj ponovo"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onStopTts()
                            currentChapterIndex = 0
                            coroutineScope.launch { listState.animateScrollToItem(0) }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(ui.text("Pročitaj ponovo", "Noch einmal lesen"), fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onNewStoryClick,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(ui.text("Nova priča", "Neue Geschichte"), fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showChronicle) {
        var draft by remember { mutableStateOf(story.chronicle) }
        AlertDialog(onDismissRequest = { showChronicle = false },
            title = { Text(ui.text("Kronika priče", "Geschichtenchronik")) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(ui.text("Ovo je memorija serije koju AI koristi za nastavke. Ispravi eventualno pogrešne činjenice.",
                    "Dies ist das Gedächtnis der Serie. Korrigiere mögliche falsche Fakten."))
                OutlinedTextField(draft, { draft = it.take(22_000) }, modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp).testTag("chronicle_editor"))
            } },
            confirmButton = { TextButton(enabled = draft.isNotBlank(), onClick = { onSaveChronicle(draft); showChronicle = false }) {
                Text(ui.text("Sačuvaj", "Speichern")) } },
            dismissButton = { TextButton(onClick = { showChronicle = false }) { Text(ui.text("Odustani", "Abbrechen")) } })
    }
    if (showVoicePickerSheet) {
        VoiceSelectionBottomSheet(
            voices = GeminiVoices.ALL_VOICES,
            selectedVoice = currentVoice,
            language = story.config.language,
            isNightMode = isNightMode,
            isPreviewing = isPreviewingVoice,
            onSelectVoice = onSelectVoice,
            onPreviewVoice = { voice ->
                onPreviewVoice(voice, story.config.language.code)
            },
            onStopPreviewVoice = onStopPreviewVoice,
            onDismiss = {
                onStopPreviewVoice()
                showVoicePickerSheet = false
            },
            onConfirmAndPlay = { voice ->
                onSelectVoice(voice)
                showVoicePickerSheet = false
                onPlayTts(chapterSpeechText, story.config.language.code, chapterMusicMood)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSelectionBottomSheet(
    voices: List<GeminiVoice>,
    selectedVoice: GeminiVoice,
    language: StoryLanguage,
    isNightMode: Boolean,
    isPreviewing: Boolean,
    onSelectVoice: (GeminiVoice) -> Unit,
    onPreviewVoice: (GeminiVoice) -> Unit,
    onStopPreviewVoice: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmAndPlay: (GeminiVoice) -> Unit
) {
    val ui = LocalAppLanguage.current
    var previewingVoiceName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isPreviewing) {
        if (!isPreviewing) {
            previewingVoiceName = null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isNightMode) DreamNightSurface else DreamDayCard,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎙️", fontSize = 26.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (ui.isGerman) "Erzählerstimme wählen" else "Izaberi glas pripovjedača",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isNightMode) MagicGoldLight else MagicPurple
                            )
                        )
                        Text(
                            text = ui.text("Gemini i OpenAI prirodni AI glasovi za dječje priče", "Natürliche Gemini- und OpenAI-KI-Stimmen für Kindergeschichten"),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (isNightMode) DreamNightTextSec else DreamDayTextSec
                            )
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = ui.text("Zatvori", "Schließen"))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Voice cards list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(voices.size) { index ->
                    val voice = voices[index]
                    val isSelected = voice.name == selectedVoice.name
                    val isCurrentPreview = isPreviewing && previewingVoiceName == voice.name

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = when {
                            isSelected -> MagicPurple.copy(alpha = if (isNightMode) 0.35f else 0.12f)
                            else -> if (isNightMode) DreamNightCard else Color.White
                        },
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = when {
                                isSelected -> MagicPurple
                                else -> if (isNightMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectVoice(voice) }
                            .testTag("voice_option_${voice.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Emoji circle
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MagicPurple.copy(alpha = 0.25f)
                                        else if (isNightMode) Color(0xFF334155) else Color(0xFFF1F5F9)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = voice.emoji, fontSize = 22.sp)
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Name, Tone, Description
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (voice.isOfflineDevice) ui.text("Offline glas uređaja", "Offline-Gerätestimme") else voice.displayName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isNightMode) DreamNightText else DreamDayText
                                        )
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (voice.isOffline) MagicMint.copy(alpha = 0.2f) else MagicGold.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = ui.text(voice.toneCategoryBs, voice.toneCategoryDe),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (voice.isOffline) Color(0xFF047857) else Color(0xFFB45309)
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = ui.text(voice.descriptionBs, voice.descriptionDe),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isNightMode) DreamNightTextSec else DreamDayTextSec,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Preview button
                            IconButton(
                                onClick = {
                                    if (isCurrentPreview) {
                                        onStopPreviewVoice()
                                        previewingVoiceName = null
                                    } else {
                                        previewingVoiceName = voice.name
                                        onPreviewVoice(voice)
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrentPreview) Color(0xFFEF4444).copy(alpha = 0.15f)
                                        else MagicPurple.copy(alpha = 0.15f)
                                    )
                                    .testTag("voice_preview_${voice.name}")
                            ) {
                                if (isCurrentPreview) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = ui.text("Zaustavi preslušavanje", "Hörprobe stoppen"),
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = ui.text("Preslušaj glas", "Stimme anhören"),
                                        tint = MagicPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm & Play Button
            Button(
                onClick = { onConfirmAndPlay(selectedVoice) },
                colors = ButtonDefaults.buttonColors(containerColor = MagicPurple),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_voice_and_play_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (ui.isGerman) "Stimme wählen & vorlesen" else "Izaberi glas i pokreni čitanje",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}
