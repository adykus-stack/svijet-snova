package com.example

import android.view.WindowManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.Lifecycle
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.After
import com.example.data.security.ParentPinStore
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParentEntryUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var originalPinPrefs: Map<String, *>
    @Before fun preparePin() {
        originalPinPrefs = compose.activity.getSharedPreferences("parent_pin", 0).all.toMap()
        ParentPinStore(compose.activity).set("2468")
    }
    @After fun restorePin() {
        val editor = compose.activity.getSharedPreferences("parent_pin", 0).edit().clear()
        originalPinPrefs.forEach { (key, value) -> when (value) {
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
        } }
        editor.commit()
    }

    private fun openParents() {
        if (compose.onAllNodesWithTag("open_settings").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("open_settings").performClick()
        }
        compose.onNodeWithTag("open_parent_settings").performScrollTo().performClick()
        compose.onNodeWithTag("parent_gate_answer").performTextInput("2468")
        compose.onNodeWithTag("parent_gate_unlock").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("api_key_input").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("api_key_input").performScrollTo().assertIsDisplayed()
    }

    @Test fun parentEntryIsProtectedAndUnsubmittedDraftIsClearedOnExit() {
        openParents()
        compose.runOnIdle {
            assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        }
        compose.onNodeWithTag("api_key_input").performTextInput("test-draft-never-save-or-send")
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("open_parent_settings").assertIsDisplayed()
        compose.onNodeWithTag("api_key_input").assertDoesNotExist()
        openParents()
        compose.onNodeWithTag("api_key_input")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("open_parent_settings").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    @Test fun backgroundingClosesParentSettingsAndClearsTheUnsubmittedKey() {
        openParents()
        compose.onNodeWithTag("api_key_input").performTextInput("test-draft-never-save-or-send")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithTag("api_key_input").assertDoesNotExist()
        compose.onNodeWithTag("open_parent_settings").assertIsDisplayed()
        openParents()
        compose.onNodeWithTag("api_key_input")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }
}
