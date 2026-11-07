package com.memora.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.memora.app.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented smoke test (runs on a device or emulator):
 * the five bottom-bar destinations exist and navigation works.
 */
@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomBarShowsFiveDestinations() {
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("Search").assertIsDisplayed()
        composeRule.onNodeWithText("Add").assertIsDisplayed()
        composeRule.onNodeWithText("Documents").assertIsDisplayed()
        composeRule.onNodeWithText("More").assertIsDisplayed()
    }

    @Test
    fun homeQuickActionOpensAddScreen() {
        composeRule.onNodeWithText("Add item").performScrollTo().performClick()
        // The Add screen shows its mode selector.
        composeRule.onNodeWithText("Item").assertIsDisplayed()
        composeRule.onNodeWithText("Document").assertIsDisplayed()
    }

    @Test
    fun saveDocumentQuickActionOpensAddScreenInDocumentMode() {
        composeRule.onNodeWithText("Save document").performScrollTo().performClick()
        composeRule.onNodeWithText("Document").assertIsDisplayed()
    }

    @Test
    fun moreTabListsSections() {
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("App sections").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Development status").performScrollTo().assertIsDisplayed()
    }
}
