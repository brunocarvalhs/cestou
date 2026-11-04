package br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** sdd-035: o chat abre na última mensagem, sem precisar rolar (lista invertida). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AiChatScreenScrollTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun conversation(size: Int) = (1..size).map {
        ChatMessage(
            text = "mensagem $it",
            sender = if (it % 2 == 0) ChatMessage.Sender.ASSISTANT else ChatMessage.Sender.USER
        )
    }

    @Test
    fun `a long conversation opens on the last message`() {
        val state = AiChatUiState(messages = conversation(60))

        composeTestRule.setContent {
            CestouTheme { AiChatScreen(state = state, intent = AiChatIntent()) }
        }

        composeTestRule.onNodeWithText("mensagem 60").assertIsDisplayed()
        composeTestRule.onNodeWithText("mensagem 1").assertDoesNotExist()
    }

    @Test
    fun `a short conversation shows every message`() {
        val state = AiChatUiState(messages = conversation(3))

        composeTestRule.setContent {
            CestouTheme { AiChatScreen(state = state, intent = AiChatIntent()) }
        }

        composeTestRule.onNodeWithText("mensagem 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("mensagem 3").assertIsDisplayed()
    }
}
