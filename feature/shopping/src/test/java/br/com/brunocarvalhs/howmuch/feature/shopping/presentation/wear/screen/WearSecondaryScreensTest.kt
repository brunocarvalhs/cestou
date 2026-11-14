package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.common.wearable.AiFailure
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearAiIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearNotificationsIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearAiUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearNotificationsUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearAiViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearNotificationsViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WearSecondaryScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun notification(id: String, isRead: Boolean) = Notification(
        id = id,
        userId = "user-1",
        title = "Lista $id",
        message = "Mensagem $id",
        type = "list_joined",
        isRead = isRead,
        timestamp = 1L
    )

    @Test
    fun `notifications offer mark all read when there are unread ones`() {
        val intents = mutableListOf<WearNotificationsIntent>()
        val viewModel = mockk<WearNotificationsViewModel>(relaxed = true) {
            every { uiState } returns MutableStateFlow(
                WearNotificationsUiState(
                    items = StableList(listOf(notification("a", isRead = false))),
                    unreadCount = 1,
                    isLoading = false
                )
            )
            every { onIntent(any()) } answers { intents += firstArg<WearNotificationsIntent>() }
        }
        composeTestRule.setContent { WearNotificationsScreen(viewModel = viewModel) }

        composeTestRule.onNodeWithTag(TAG_NOTIFICATIONS_MARK_READ).performClick()

        assertEquals(listOf<WearNotificationsIntent>(WearNotificationsIntent.MarkAllRead), intents)
    }

    @Test
    fun `notifications show the empty state when the account is missing`() {
        val viewModel = mockk<WearNotificationsViewModel>(relaxed = true) {
            every { uiState } returns MutableStateFlow(WearNotificationsUiState(isLoading = false, signedOut = true))
        }
        composeTestRule.setContent { WearNotificationsScreen(viewModel = viewModel) }

        composeTestRule.onNodeWithTag(TAG_NOTIFICATIONS_EMPTY).assertIsDisplayed()
    }

    @Test
    fun `ai shows the confirmation and sends the approval`() {
        val intents = mutableListOf<WearAiIntent>()
        val viewModel = mockk<WearAiViewModel>(relaxed = true) {
            every { uiState } returns MutableStateFlow(
                WearAiUiState.Confirming(requestId = "req-1", actionId = "action-1", summary = "Lista de teste")
            )
            every { onIntent(any()) } answers { intents += firstArg<WearAiIntent>() }
        }
        composeTestRule.setContent { WearAiScreen(viewModel = viewModel) }

        composeTestRule.onNodeWithTag(TAG_AI_CONFIRM).performClick()

        assertEquals(listOf<WearAiIntent>(WearAiIntent.Decide(approved = true)), intents)
    }

    @Test
    fun `ai shows the answer the phone sent`() {
        val viewModel = mockk<WearAiViewModel>(relaxed = true) {
            every { uiState } returns MutableStateFlow(WearAiUiState.Answer("Leite adicionado"))
        }
        composeTestRule.setContent { WearAiScreen(viewModel = viewModel) }

        composeTestRule.onNodeWithTag(TAG_AI_ANSWER).assertIsDisplayed()
    }

    @Test
    fun `ai shows the failure state when the phone has no AI`() {
        val viewModel = mockk<WearAiViewModel>(relaxed = true) {
            every { uiState } returns MutableStateFlow(WearAiUiState.Failed(AiFailure.NO_PROVIDER))
        }
        composeTestRule.setContent { WearAiScreen(viewModel = viewModel) }

        composeTestRule.onNodeWithTag(TAG_AI_FAILED).assertIsDisplayed()
    }

    @Test
    fun `ai sends a ready question when it is tapped`() {
        val intents = mutableListOf<WearAiIntent>()
        val viewModel = mockk<WearAiViewModel>(relaxed = true) {
            every { uiState } returns MutableStateFlow(WearAiUiState.Idle)
            every { onIntent(any()) } answers { intents += firstArg<WearAiIntent>() }
        }
        composeTestRule.setContent { WearAiScreen(viewModel = viewModel) }

        composeTestRule.onNodeWithTag("${TAG_AI_SUGGESTION_PREFIX}0").performClick()

        assertEquals(1, intents.size)
        assertTrue((intents.single() as WearAiIntent.Send).prompt.isNotBlank())
    }

    @Test
    fun `ai hides the ready questions while the phone is answering`() {
        val viewModel = mockk<WearAiViewModel>(relaxed = true) {
            every { uiState } returns MutableStateFlow(WearAiUiState.Waiting)
        }
        composeTestRule.setContent { WearAiScreen(viewModel = viewModel) }

        composeTestRule.onAllNodesWithTag("${TAG_AI_SUGGESTION_PREFIX}0").assertCountEquals(0)
    }

    @Test
    fun `ai shows the waiting state while the phone is answering`() {
        val viewModel = mockk<WearAiViewModel>(relaxed = true) {
            every { uiState } returns MutableStateFlow(WearAiUiState.Waiting)
        }
        composeTestRule.setContent { WearAiScreen(viewModel = viewModel) }

        composeTestRule.onNodeWithTag(TAG_AI_WAITING).assertIsDisplayed()
        verify(exactly = 0) { viewModel.onIntent(any()) }
    }
}
