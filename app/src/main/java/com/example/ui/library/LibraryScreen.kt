package com.example.ui.library

import com.example.ui.localization.LocalAppLanguage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StoryEntity
import com.example.data.model.ActiveStory
import com.example.data.model.StoryCategories
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.MagicButton
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    savedStories: List<StoryEntity>,
    onSelectStory: (ActiveStory) -> Unit,
    onDeleteStory: (Long) -> Unit,
    onBackClick: () -> Unit,
    onCreateNewStory: () -> Unit,
    isNightMode: Boolean,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    initialCategory: String? = null
) {
    val ui = LocalAppLanguage.current
    var storyToDelete by remember { mutableStateOf<StoryEntity?>(null) }
    var categoryFilter by remember(initialCategory) { mutableStateOf(initialCategory) }
    val filteredStories = savedStories.filter { categoryFilter == null || it.categoryId == categoryFilter }

    storyToDelete?.let { story ->
        DeleteConfirmationDialog(
            storyTitle = story.title,
            onConfirm = {
                onDeleteStory(story.id)
                storyToDelete = null
            },
            onDismiss = {
                storyToDelete = null
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = ui.text("Biblioteka priča", "Geschichtenbibliothek"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("library_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = ui.text("Nazad", "Zurück"))
                    }
                },
                actions = { IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("open_settings")) {
                    Icon(Icons.Default.Settings, ui.text("Postavke", "Einstellungen"))
                } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isNightMode) DreamNightSurface else DreamDaySurface
                )
            )
        }
    ) { innerPadding ->
        if (savedStories.isEmpty()) {
            // Empty State
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "📚 ✨", fontSize = 54.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = ui.text("Tvoja biblioteka je prazna", "Deine Bibliothek ist noch leer"),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isNightMode) DreamNightText else DreamDayText
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ui.text("Kada stvoriš i sačuvaš priču, ona će se pojaviti ovdje kako biste je mogli čitati bilo kada!", "Erstelle und speichere eine Geschichte. Sie erscheint hier, damit du sie jederzeit lesen kannst!"),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isNightMode) DreamNightTextSec else DreamDayTextSec
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    MagicButton(
                        text = ui.text("Stvori prvu priču ✨", "Erste Geschichte erstellen ✨"),
                        onClick = onCreateNewStory,
                        gradientColors = listOf(MagicPurple, MagicGold),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }
            }
        } else {
            // List of Saved Stories
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { FilterChip(categoryFilter == null, onClick = { categoryFilter = null }, label = { Text(ui.text("Sve", "Alle")) }) }
                        items(StoryCategories.allCategories.filter { c -> savedStories.any { it.categoryId == c.id } }) { c ->
                            FilterChip(categoryFilter == c.id, onClick = { categoryFilter = c.id }, label = { Text(ui.text(c.titleBs, c.titleDe)) })
                        }
                    }
                }
                if (filteredStories.isEmpty()) item { Text(ui.text("Nema sačuvanih priča u ovoj kategoriji.", "Keine gespeicherten Geschichten in dieser Kategorie.")) }
                items(filteredStories, key = { it.id }) { storyEntity ->
                    val category = StoryCategories.getById(storyEntity.categoryId)
                    val dateFormatted = remember(storyEntity.createdAt) {
                        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        sdf.format(Date(storyEntity.createdAt))
                    }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNightMode) DreamNightSurface else Color.White
                        ),
                        border = BorderStroke(1.dp, category.accentColor.copy(alpha = 0.35f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectStory(storyEntity.toActiveStory()) }
                            .testTag("saved_story_card_${storyEntity.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(category.accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = category.emoji, fontSize = 28.sp)
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (storyEntity.languageCode == "de") "🇩🇪" else "🇧🇦",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = ui.text(category.titleBs, category.titleDe),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = category.accentColor
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "• $dateFormatted",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isNightMode) DreamNightTextSec else DreamDayTextSec
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = storyEntity.title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNightMode) DreamNightText else DreamDayText
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = if (storyEntity.isSerial) ui.text("Serija · ${storyEntity.toActiveStory().chapters.size} poglavlja · kronika sačuvana", "Serie · ${storyEntity.toActiveStory().chapters.size} Kapitel · Chronik gespeichert")
                                        else ui.text("Junak: ", "Hauptfigur: ") + storyEntity.heroName.ifBlank { ui.text("Mali avanturist", "Kleiner Abenteurer") },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isNightMode) DreamNightTextSec else DreamDayTextSec
                                    )
                                )
                            }

                            // Delete button
                            IconButton(
                                onClick = { storyToDelete = storyEntity },
                                modifier = Modifier.testTag("delete_saved_story_${storyEntity.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = ui.text("Obriši", "Löschen"),
                                    tint = MagicPink.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
