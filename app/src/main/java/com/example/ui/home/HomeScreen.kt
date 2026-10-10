package com.example.ui.home

import com.example.ui.localization.LocalAppLanguage

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveStory
import com.example.data.model.StoryCategories
import com.example.data.model.StoryCategory
import com.example.ui.components.MagicButton
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    selectedCategory: StoryCategory,
    onSelectCategory: (StoryCategory) -> Unit,
    onProceedToSetup: () -> Unit,
    activeStory: ActiveStory?,
    onResumeActiveStory: () -> Unit,
    isNightMode: Boolean,
    modifier: Modifier = Modifier
) {
    val ui = LocalAppLanguage.current
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
        // Welcoming Hero Header
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isNightMode) DreamNightCard else DreamDayCard
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(MagicGold, MagicCoral))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🌙", fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ui.text("Svijet snova ✨", "Traumwelt ✨"),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isNightMode) MagicGoldLight else MagicPurple
                        )
                    )
                    Text(
                        text = ui.text("Kakvu ćemo priču večeras zajedno stvoriti?", "Welche Geschichte erschaffen wir heute Abend zusammen?"),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isNightMode) DreamNightText else DreamDayText,
                            lineHeight = 24.sp
                        )
                    )
                }
            }
        }

        // Ongoing Story Quick Resume Banner
        if (activeStory != null && activeStory.chapters.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNightMode) MagicPurple.copy(alpha = 0.25f) else MagicPurpleLight.copy(alpha = 0.45f)
                ),
                border = BorderStroke(1.5.dp, MagicPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onResumeActiveStory() }
                    .testTag("resume_story_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MagicPurple,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ui.text("Nastavi čitanje priče:", "Weiterlesen:"),
                            style = MaterialTheme.typography.labelSmall.copy(color = MagicPurple)
                        )
                        Text(
                            text = activeStory.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = ui.text("Nastavi ➔", "Weiter ➔"),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MagicPurple
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Categories Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = ui.text("Izaberi kategoriju priče", "Wähle eine Geschichtenkategorie"),
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isNightMode) DreamNightText else DreamDayText
                )
            )
            Text(
                text = ui.text("${StoryCategories.allCategories.size} tema", "${StoryCategories.allCategories.size} Themen"),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (isNightMode) DreamNightTextSec else DreamDayTextSec
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Grid of 10 illustrated category cards
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(StoryCategories.allCategories, key = { it.id }) { category ->
                    val isSelected = category.id == selectedCategory.id

                    CategoryCard(
                        category = category,
                        isSelected = isSelected,
                        isNightMode = isNightMode,
                        onClick = { onSelectCategory(category) }
                    )
                }
            }
        }

        // Keep the next-step action visible above the scrolling category list.
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            MagicButton(
                text = ui.text("Izaberi uzrast djeteta ➔", "Alter des Kindes wählen ➔"),
                onClick = onProceedToSetup,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                gradientColors = listOf(MagicPurple, MagicSky),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("proceed_to_setup_button")
            )
        }
    }
}

@Composable
fun CategoryCard(
    category: StoryCategory,
    isSelected: Boolean,
    isNightMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui = LocalAppLanguage.current
    val cardBg = if (isSelected) {
        category.accentColor.copy(alpha = if (isNightMode) 0.35f else 0.22f)
    } else {
        if (isNightMode) DreamNightCard else DreamDaySurface
    }

    val borderColor = if (isSelected) category.accentColor else Color.Transparent
    val borderWidth = if (isSelected) 2.5.dp else 0.dp

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = if (isSelected) BorderStroke(borderWidth, borderColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("category_card_${category.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(category.accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = category.emoji, fontSize = 26.sp)
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = ui.text("Izabrano", "Ausgewählt"),
                            tint = category.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = ui.text(category.titleBs, category.titleDe),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) category.accentColor
                        else if (isNightMode) DreamNightText else DreamDayText
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = ui.text(category.descriptionBs, category.descriptionDe),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (isNightMode) DreamNightTextSec else DreamDayTextSec,
                        lineHeight = 16.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
