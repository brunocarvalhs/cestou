package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.notifications

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class NotificationBellButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun show(count: Int, onClick: () -> Unit = {}) {
        composeTestRule.setContent { CestouTheme { NotificationBellButton(unreadCount = count, onClick = onClick) } }
    }

    @Test
    fun `no badge when there is nothing unread`() {
        show(0)

        composeTestRule.onNodeWithTag(NOTIFICATIONS_BELL_TAG).assertExists()
        composeTestRule.onNodeWithTag(NOTIFICATIONS_BADGE_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `badge shows the exact count up to the cap`() {
        show(NOTIFICATIONS_BADGE_MAX)

        composeTestRule.onNodeWithTag(NOTIFICATIONS_BADGE_TAG, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(NOTIFICATIONS_BADGE_COUNT_TAG, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(NOTIFICATIONS_BADGE_OVERFLOW_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `badge caps above nine`() {
        show(NOTIFICATIONS_BADGE_MAX + 1)

        composeTestRule.onNodeWithTag(NOTIFICATIONS_BADGE_OVERFLOW_TAG, useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(NOTIFICATIONS_BADGE_COUNT_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `click is reported`() {
        var clicked = false
        show(2) { clicked = true }

        composeTestRule.onNodeWithTag(NOTIFICATIONS_BELL_TAG).performClick()

        assertTrue(clicked)
    }
}
