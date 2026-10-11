package com.example.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.example.data.model.*
import com.example.ui.components.MagicButton
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.theme.*

private object CloudShape : Shape {
    fun path(size: Size): Path = Path().apply {
        val w = size.width
        val h = size.height
        moveTo(w * .16f, h * .82f)
        cubicTo(w * .01f, h * .83f, w * .01f, h * .45f, w * .15f, h * .41f)
        cubicTo(w * .12f, h * .18f, w * .31f, h * .10f, w * .43f, h * .25f)
        cubicTo(w * .51f, h * .05f, w * .74f, h * .06f, w * .79f, h * .30f)
        cubicTo(w * .99f, h * .26f, w * 1.02f, h * .61f, w * .88f, h * .69f)
        cubicTo(w * .89f, h * .88f, w * .73f, h * .97f, w * .63f, h * .87f)
        cubicTo(w * .46f, h * 1.01f, w * .29f, h * .99f, w * .23f, h * .86f)
        cubicTo(w * .20f, h * .88f, w * .18f, h * .84f, w * .16f, h * .82f)
        close()
    }

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(path(size))
}

@Composable
fun HomeScreen(
    selectedCategory: StoryCategory,
    onSelectCategory: (StoryCategory) -> Unit,
    onProceedToSetup: () -> Unit,
    activeStory: ActiveStory?,
    onResumeActiveStory: () -> Unit,
    isNightMode: Boolean,
    modifier: Modifier = Modifier,
    categoryHasStories: Boolean = false,
    onOpenCategoryStories: () -> Unit = {}
) {
    val ui = LocalAppLanguage.current
    val shimmer = rememberInfiniteTransition(label = "cloud_glow").animateFloat(
        initialValue = .35f, targetValue = .95f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "gentle_shimmer"
    )
    Box(modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().testTag("category_grid"),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = if (categoryHasStories) 168.dp else 104.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ui.text("Kuda nas vodi mašta?", "Wohin führt uns die Fantasie?"),
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(ui.text("Dodirni oblačić i započni novu priču.", "Berühre eine Wolke und beginne eine neue Geschichte."),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (activeStory != null && activeStory.chapters.isNotEmpty()) {
                        OutlinedButton(onClick = onResumeActiveStory,
                            modifier = Modifier.fillMaxWidth().testTag("resume_story_banner")) {
                            Text(ui.text("Nastavi: ", "Weiterlesen: ") + activeStory.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(ui.text("Izaberi kategoriju priče", "Wähle eine Geschichtenkategorie"),
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(ui.text("${StoryCategories.allCategories.size} tema", "${StoryCategories.allCategories.size} Themen"), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(StoryCategories.allCategories, key = { it.id }) { category ->
                CategoryCard(category, category.id == selectedCategory.id, isNightMode, shimmer,
                    onClick = { onSelectCategory(category) })
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = .97f))))
            .padding(horizontal = 20.dp, vertical = 16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (categoryHasStories) OutlinedButton(onClick = onOpenCategoryStories,
                modifier = Modifier.fillMaxWidth().testTag("open_category_stories")) {
                Text(ui.text("Otvori priče iz ove kategorije", "Geschichten dieser Kategorie öffnen"))
            }
            MagicButton(text = ui.text("Izaberi uzrast djeteta ➔", "Alter des Kindes wählen ➔"),
                onClick = onProceedToSetup, icon = Icons.AutoMirrored.Filled.ArrowForward,
                gradientColors = listOf(MagicPurple, MagicSky), modifier = Modifier.fillMaxWidth(),
                testTag = "proceed_to_setup_button")
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: StoryCategory, isSelected: Boolean, isNightMode: Boolean, shimmer: State<Float>,
    onClick: () -> Unit
) {
    val ui = LocalAppLanguage.current
    val accent = if (isNightMode) lerp(category.accentColor, Color.White, .38f) else category.accentColor
    val colors = if (isNightMode) listOf(
        lerp(DreamNightCard, accent, if (isSelected) .30f else .12f),
        lerp(DreamNightSurface, accent, if (isSelected) .20f else .06f)
    ) else listOf(lerp(Color.White, accent, if (isSelected) .22f else .09f), Color.White)
    Box(Modifier.fillMaxWidth().height(190.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val path = CloudShape.path(size)
            val glow = shimmer.value
            drawPath(path, accent.copy(alpha = .04f + glow * .05f), style = Stroke(12.dp.toPx()))
            drawPath(path, accent.copy(alpha = .09f + glow * .08f), style = Stroke(7.dp.toPx()))
            drawPath(path, accent.copy(alpha = .12f + glow * .12f), style = Stroke(3.dp.toPx()))
        }
        Box(Modifier.fillMaxSize().clip(CloudShape).background(Brush.verticalGradient(colors))
            .selectable(selected = isSelected, role = Role.Button, onClick = onClick)
            .testTag("category_card_${category.id}"), contentAlignment = Alignment.Center) {
            Column(Modifier.padding(start = 21.dp, end = 21.dp, top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(category.emoji, fontSize = 32.sp)
                Text(ui.text(category.titleBs, category.titleDe), fontSize = 13.sp, lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onBackground)
            }
            if (isSelected) Icon(Icons.Default.CheckCircle, ui.text("Izabrano", "Ausgewählt"), tint = accent,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 49.dp, end = 24.dp).size(18.dp))
            Text("✦", color = accent.copy(alpha = .7f), fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 14.dp))
        }
        Canvas(Modifier.fillMaxSize()) {
            drawPath(CloudShape.path(size), accent.copy(alpha = if (isSelected) .95f else .4f),
                style = Stroke((if (isSelected) 2.dp else 1.dp).toPx()))
        }
    }
}
