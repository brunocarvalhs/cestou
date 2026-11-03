package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.screen

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationFilter
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationSectionKey
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.notifications.notificationItemTag
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.notifications.notificationUnreadDotTag
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.NotificationsIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationItem
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationSection
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationType
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class NotificationsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun item(id: String, isRead: Boolean, type: NotificationType = NotificationType.LIST_JOINED) =
        NotificationItem(id, "Title $id", "Description $id", "5 min", type, isRead, "s$id", "u$id")

    private val state = NotificationsUiState(
        isLoading = false,
        hasUnread = true,
        sections = StableList(
            listOf(
                NotificationSection(
                    NotificationSectionKey.TODAY,
                    StableList(listOf(item("1", isRead = false), item("2", isRead = true, NotificationType.OTHER)))
                ),
                NotificationSection(
                    NotificationSectionKey.YESTERDAY,
                    StableList(listOf(item("3", isRead = true, NotificationType.LIST_FINISHED)))
                )
            )
        ),
        senderProfiles = mapOf("u1" to UserProfile(id = "u1", name = "Ana Nunes"))
    )

    private fun show(state: NotificationsUiState, intent: NotificationsIntent = NotificationsIntent()) {
        composeTestRule.setContent { CestouTheme { NotificationsScreen(state = state, intent = intent) } }
    }

    @Test
    fun `clicking a notification reports its id`() {
        var clicked: String? = null
        show(state, NotificationsIntent(onNotificationClick = { clicked = it }))

        composeTestRule.onNodeWithTag(notificationItemTag("1")).performClick()

        assertEquals("1", clicked)
    }

    @Test
    fun `shows the unread dot only on unread rows`() {
        show(state)

        composeTestRule.onNodeWithTag(notificationUnreadDotTag("1"), useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithTag(notificationUnreadDotTag("2"), useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `renders every section row in the list`() {
        show(state)

        composeTestRule.onNodeWithTag(NOTIFICATIONS_LIST_TAG).assertExists()
        composeTestRule.onNodeWithTag(notificationItemTag("1")).assertExists()
        composeTestRule.onNodeWithTag(NOTIFICATIONS_LIST_TAG)
            .performScrollToNode(hasTestTag(notificationItemTag("3")))
        composeTestRule.onNodeWithTag(notificationItemTag("3")).assertExists()
    }

    @Test
    fun `mark all read is enabled with unread and reports the click`() {
        var clicked = false
        show(state, NotificationsIntent(onMarkAllRead = { clicked = true }))

        composeTestRule.onNodeWithTag(NOTIFICATIONS_MARK_ALL_READ_TAG).assertIsEnabled().performClick()

        assertTrue(clicked)
    }

    @Test
    fun `mark all read is disabled when everything is read`() {
        show(state.copy(hasUnread = false))

        composeTestRule.onNodeWithTag(NOTIFICATIONS_MARK_ALL_READ_TAG).assertIsNotEnabled()
    }

    @Test
    fun `selecting a filter chip reports it`() {
        var selected: NotificationFilter? = null
        show(state, NotificationsIntent(onFilter = { selected = it }))

        composeTestRule.onNodeWithTag("notifications_filter_lists").performClick()

        assertEquals(NotificationFilter.LISTS, selected)
    }

    @Test
    fun `renders the empty state`() {
        show(NotificationsUiState(isLoading = false))

        composeTestRule.onNodeWithTag("notifications_empty").assertExists()
        composeTestRule.onNodeWithTag(NOTIFICATIONS_LIST_TAG).assertDoesNotExist()
    }

    @Test
    fun `renders the loading state`() {
        show(NotificationsUiState(isLoading = true))

        composeTestRule.onNodeWithTag(NOTIFICATIONS_LOADING_TAG).assertExists()
    }

    @Test
    fun `back reports the callback`() {
        var back = false
        show(state, NotificationsIntent(onBack = { back = true }))

        composeTestRule.onNodeWithContentDescriptionBack().performClick()

        assertTrue(back)
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithContentDescriptionBack() =
    onNodeWithContentDescription(
        androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(br.com.brunocarvalhs.howmuch.core.ui.R.string.content_description_back)
    )
