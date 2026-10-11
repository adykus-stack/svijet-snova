package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.StoryEntity
import com.example.data.model.ActiveStory
import com.example.data.model.Chapter
import com.example.data.model.StoryCategories
import com.example.data.model.StoryCreationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Svijet snova", appName)
  }

  @Test
  fun `verify story categories exist`() {
    val categories = StoryCategories.allCategories
    assertEquals(11, categories.size)
    assertNotNull(StoryCategories.getById("magic"))
    assertNotNull(StoryCategories.getById("bedtime"))
  }

  @Test
  fun `verify story entity conversion`() {
    val activeStory = ActiveStory(
      id = 1L,
      title = "Čarobni zmaj",
      config = StoryCreationConfig(),
      chapters = listOf(
        Chapter(
          chapterNumber = 1,
          chapterTitle = "Susret",
          storyText = "Bio jednom zmaj...",
          moral = "Budi dobar.",
          continuationOptions = listOf("Idi lijevo", "Idi desno", "Ostani")
        )
      )
    )

    val entity = StoryEntity.fromActiveStory(activeStory)
    val restored = entity.toActiveStory()

    assertEquals("Čarobni zmaj", restored.title)
    assertEquals(1, restored.chapters.size)
    assertEquals("Susret", restored.chapters[0].chapterTitle)
  }
}
