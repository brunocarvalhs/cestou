package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.NotificationsIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationItem
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationType
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class NotificationsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val items = NotificationType.entries.mapIndexed { i, type ->
        NotificationItem(
            id = "$i",
            title = "Notificação $i",
            description = "Descrição $i",
            time = "Há $i min",
            type = type,
            isRead = i % 2 == 1
        )
    }

    @Test
    fun `clicking a notification reports its id`() {
        var clicked: String? = null
        composeTestRule.setContent {
            CestouTheme {
                NotificationsScreen(
                    state = NotificationsUiState(notifications = items),
                    intent = NotificationsIntent(onNotificationClick = { clicked = it })
                )
            }
        }
        composeTestRule.onNodeWithText("Notificação 0").performClick()
        assertEquals("0", clicked)
    }

    @Test
    fun `renders the empty state`() {
        composeTestRule.setContent {
            CestouTheme { NotificationsScreen(state = NotificationsUiState(), intent = NotificationsIntent()) }
        }
        composeTestRule.onRoot().assertExists()
    }
}
