package br.com.brunocarvalhs.howmuch.feature.chat.presentation.component

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen.AiChatScreen
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiTurnInfo
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.MessageVote
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.ReasonChip
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.Vote
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MessageFeedbackBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun bar(
        vote: MessageVote?,
        onVote: (Long, Vote) -> Unit = { _, _ -> },
        onReason: (Long, ReasonChip) -> Unit = { _, _ -> }
    ) {
        composeTestRule.setContent {
            CestouTheme { MessageFeedbackBar(messageId = 7L, vote = vote, onVote = onVote, onReason = onReason) }
        }
    }

    private fun chat(state: AiChatUiState) {
        composeTestRule.setContent {
            CestouTheme { AiChatScreen(state = state, intent = AiChatIntent()) }
        }
    }

    private val answer = ChatMessage(id = 1L, text = "hi", sender = ChatMessage.Sender.ASSISTANT)

    @Test
    fun `tapping an emoji reports the vote`() {
        var voted: Pair<Long, Vote>? = null
        bar(vote = null, onVote = { id, v -> voted = id to v })

        composeTestRule.onNodeWithTag("message_vote_down").performClick()

        assertEquals(7L to Vote.DOWN, voted)
    }

    @Test
    fun `the bar locks after voting`() {
        bar(vote = MessageVote(Vote.UP))

        composeTestRule.onNodeWithTag("message_vote_up").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("message_vote_down").assertIsNotEnabled()
    }

    @Test
    fun `thumbs down offers the reason chips and reports the chosen one`() {
        var reason: ReasonChip? = null
        bar(vote = MessageVote(Vote.DOWN), onReason = { _, r -> reason = r })

        composeTestRule.onNodeWithTag("message_vote_reason_too_slow").performClick()

        assertEquals(ReasonChip.TOO_SLOW, reason)
    }

    @Test
    fun `thumbs up shows no reason chips`() {
        bar(vote = MessageVote(Vote.UP))

        composeTestRule.onNodeWithTag("message_vote_reason_other").assertDoesNotExist()
    }

    @Test
    fun `the chat shows the bar for answers of this session when feedback is enabled`() {
        chat(
            AiChatUiState(
                messages = listOf(answer),
                turns = mapOf(1L to AiTurnInfo("gemini", 0)),
                feedbackEnabled = true
            )
        )

        composeTestRule.onNodeWithTag("message_vote_up").assertExists()
    }

    @Test
    fun `the chat hides the bar when feedback is disabled`() {
        chat(AiChatUiState(messages = listOf(answer), turns = mapOf(1L to AiTurnInfo("gemini", 0))))

        composeTestRule.onNodeWithTag("message_vote_up").assertDoesNotExist()
    }
}
