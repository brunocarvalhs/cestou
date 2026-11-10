package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Testes de layout via Robolectric (JVM) do menu Wear de shopping.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WearMenuScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `menu opens the history`() {
        var opened = 0
        composeTestRule.setContent {
            WearMenuScreen(onNavigateToHistory = { opened++ }, onNavigateToSettings = {})
        }
        composeTestRule.onNodeWithTag(TAG_MENU_HISTORY).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun `menu opens the settings`() {
        var opened = 0
        composeTestRule.setContent {
            WearMenuScreen(onNavigateToHistory = {}, onNavigateToSettings = { opened++ })
        }
        composeTestRule.onNodeWithTag(TAG_MENU_SETTINGS).performClick()
        assertEquals(1, opened)
    }
}
