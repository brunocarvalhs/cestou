package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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

    private fun setMenu(
        showAi: Boolean,
        onNotifications: () -> Unit = {},
        onAi: () -> Unit = {},
        onHistory: () -> Unit = {},
        onSettings: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            WearMenuScreen(
                showAi = showAi,
                onNavigateToNotifications = onNotifications,
                onNavigateToAi = onAi,
                onNavigateToHistory = onHistory,
                onNavigateToSettings = onSettings
            )
        }
    }

    @Test
    fun `menu opens the notifications`() {
        var opened = 0
        setMenu(showAi = false, onNotifications = { opened++ })

        composeTestRule.onNodeWithTag(TAG_MENU_NOTIFICATIONS).performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `menu opens the ai entry when the flag is on`() {
        var opened = 0
        setMenu(showAi = true, onAi = { opened++ })

        composeTestRule.onNodeWithTag(TAG_MENU_AI).performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `menu hides the ai entry when the flag is off`() {
        setMenu(showAi = false)

        composeTestRule.onAllNodesWithTag(TAG_MENU_AI).assertCountEquals(0)
    }

    @Test
    fun `menu opens the history`() {
        var opened = 0
        setMenu(showAi = false, onHistory = { opened++ })

        composeTestRule.onNodeWithTag(TAG_MENU_HISTORY).performClick()

        assertEquals(1, opened)
    }

    @Test
    fun `menu opens the settings`() {
        var opened = 0
        setMenu(showAi = false, onSettings = { opened++ })

        composeTestRule.onNodeWithTag(TAG_MENU_SETTINGS).performClick()

        assertEquals(1, opened)
    }
}
