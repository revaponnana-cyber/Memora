package com.memora.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.app.MainActivity
import com.memora.app.data.MemoraDatabase
import com.memora.app.feature.home.HomeTestTags
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke test (runs on a device or emulator):
 * the sentence agent records items, the list shows them, and the item detail
 * shows the current place on top with deduplicated past addresses below.
 */
@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabase() {
        runBlocking {
            withContext(Dispatchers.IO) {
                MemoraDatabase.get(ApplicationProvider.getApplicationContext<Context>())
                    .clearAllTables()
            }
        }
    }

    @Test
    fun bottomBarShowsFiveDestinations() {
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("Search").assertIsDisplayed()
        composeRule.onNodeWithText("Add").assertIsDisplayed()
        composeRule.onNodeWithText("Documents").assertIsDisplayed()
        composeRule.onNodeWithText("More").assertIsDisplayed()
    }

    @Test
    fun homeShowsSentenceInputAndItemsList() {
        composeRule.onNodeWithTag(HomeTestTags.SENTENCE_INPUT).assertIsDisplayed()
        composeRule.onNodeWithText("My items").assertIsDisplayed()
    }

    @Test
    fun sentenceRecordsItemWithoutAnyAddButton() {
        composeRule.onNodeWithTag(HomeTestTags.SENTENCE_INPUT)
            .performTextInput("i put my car keys in my drawer in bedroom")
        composeRule.onNodeWithTag(HomeTestTags.SENTENCE_SEND).performClick()

        // The item appears in the list with its nested place (drawer in bedroom).
        composeRule.onNodeWithText("car keys").assertIsDisplayed()
        composeRule.onNodeWithText("Bedroom ▸ Drawer").assertIsDisplayed()
    }

    @Test
    fun itemDetailShowsCurrentOnTopAndPastBelowWithoutDuplicates() {
        // A → B → A → B: two visits per place, but each address shows once.
        submitSentence("i put my car keys in my drawer in bedroom")
        submitSentence("i put my car keys at the window")
        submitSentence("i put my car keys in my drawer in bedroom")
        submitSentence("i put my car keys at the window")

        // List row shows the current place.
        composeRule.onNodeWithText("Window").assertIsDisplayed()

        // Open the item.
        composeRule.onNodeWithText("car keys").performClick()

        // Current location highlighted on top.
        composeRule.onNodeWithText("Current location").assertIsDisplayed()
        composeRule.onNodeWithText("Window").assertIsDisplayed()

        // Past addresses underneath — the drawer entry appears exactly once
        // although the item was there twice.
        composeRule.onNodeWithText("Previous places").assertIsDisplayed()
        composeRule.onAllNodesWithText("Bedroom ▸ Drawer").assertCountEquals(1)
    }

    @Test
    fun moreTabListsSections() {
        composeRule.onNodeWithText("More").performClick()
        // Stage 1: the tab must become selected (navigation works).
        composeRule.onNodeWithText("More").assertIsSelected()
        // Stage 2: section list content near the top is visible.
        composeRule.onNodeWithText("App sections").assertIsDisplayed()
        composeRule.onNodeWithText("Objects").assertIsDisplayed()
        // Stage 3: scrolling the LazyColumn reveals content below the fold.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Development status"))
        composeRule.onNodeWithText("Development status").assertIsDisplayed()
    }

    private fun submitSentence(text: String) {
        composeRule.onNodeWithTag(HomeTestTags.SENTENCE_INPUT).performTextInput(text)
        composeRule.onNodeWithTag(HomeTestTags.SENTENCE_SEND).performClick()
        // Wait for the agent round-trip to finish before the next step.
        composeRule.waitForIdle()
    }
}
