package com.example

import android.graphics.Bitmap
import android.view.WindowManager
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.model.StoryCategories
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppLanguagePreferences
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SettingsAndCloudsUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var originalLanguage: AppLanguage

    @Before fun prepare() {
        originalLanguage = AppLanguagePreferences(compose.activity).get()
        compose.activity.runOnUiThread {
            compose.activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        compose.waitUntil(15000) {
            compose.onAllNodesWithTag("theme_background_DARK").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
    }
    @After fun restore() { AppLanguagePreferences(compose.activity).set(originalLanguage) }

    private fun screenshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun settingsContainLanguageParentsAndAllThreeThemes() {
        compose.onNodeWithTag("theme_background_DARK").assertExists()
        compose.onNodeWithTag("app_language_selector").assertDoesNotExist()
        compose.onNodeWithTag("open_parent_settings").assertDoesNotExist()
        compose.onNodeWithTag("open_settings").performClick()
        compose.onNodeWithTag("app_language_selector").assertIsDisplayed()
        compose.onNodeWithTag("open_parent_settings").assertIsDisplayed()
        compose.onNodeWithTag("app_theme_LIGHT").performScrollTo().performClick()
        compose.onNodeWithTag("theme_background_LIGHT").assertExists()
        compose.onNodeWithTag("app_theme_MOONLIGHT").performScrollTo().performClick()
        compose.onNodeWithTag("theme_background_MOONLIGHT").assertExists()
        screenshot("settings-moonlight.png")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("theme_background_MOONLIGHT").assertExists()
        compose.onNodeWithTag("settings_back_button").performClick()
        compose.onNodeWithTag("skip_intro_button").performClick()
        screenshot("home-moonlight.png")
        compose.onNodeWithTag("open_settings").performClick()
        compose.onNodeWithTag("app_theme_LIGHT").performScrollTo().performClick()
        compose.onNodeWithTag("settings_back_button").performClick()
        screenshot("home-light.png")
        compose.onNodeWithTag("open_settings").performClick()
        compose.onNodeWithTag("app_theme_DARK").performScrollTo().performClick()
        compose.onNodeWithTag("settings_back_button").performClick()
        screenshot("home-dark.png")
    }

    @Test fun cloudCategoriesAreTwoPerRowAndAllTenCanProceedToSetup() {
        compose.onNodeWithTag("skip_intro_button").performClick()
        val categories = StoryCategories.allCategories
        val firstNode = compose.onNodeWithTag("category_card_${categories[0].id}").fetchSemanticsNode()
        val secondNode = compose.onNodeWithTag("category_card_${categories[1].id}").fetchSemanticsNode()
        // Read animated/clipped layout coordinates on the UI thread, just like drawing.
        compose.runOnIdle {
            val first = firstNode.boundsInRoot
            val second = secondNode.boundsInRoot
            assertEquals(first.top, second.top, 1f)
            assertTrue(second.left > first.right)
        }
        for ((index, category) in categories.withIndex()) {
            // Align the cloud fully above the fixed next-step button before tapping.
            compose.onNodeWithTag("category_grid").performScrollToIndex(index + 1)
            compose.onNodeWithTag("category_card_${category.id}").performClick().assertIsSelected()
            compose.onNodeWithTag("proceed_to_setup_button").assertIsDisplayed()
        }
        compose.onNodeWithTag("proceed_to_setup_button").performClick()
        compose.onNodeWithTag("setup_back_button").assertIsDisplayed()
        compose.onNodeWithTag("open_settings").performClick()
        compose.onNodeWithTag("settings_back_button").performClick()
        compose.onNodeWithTag("setup_back_button").performClick()
        val lastTag = "category_card_${categories.last().id}"
        compose.onNodeWithTag("category_grid").performScrollToIndex(categories.size)
        compose.onNodeWithTag(lastTag).assertIsSelected()
    }
}
