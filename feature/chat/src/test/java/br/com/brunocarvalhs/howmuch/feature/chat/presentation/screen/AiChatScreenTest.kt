package br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.domain.entity.ChatMessage
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Teste de layout via Robolectric (JVM), não androidTest: valida a composição real da tela
// sem depender de emulador, adequado para rodar na esteira de CI.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AiChatScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renders the chat title`() {
        val title = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.ai_chat_title)

        composeTestRule.setContent {
            CestouTheme {
                AiChatScreen(state = AiChatUiState(), intent = AiChatIntent())
            }
        }

        composeTestRule.onNodeWithText(title).assertExists()
    }

    @Test
    fun `renders a sent message`() {
        val state = AiChatUiState(
            messages = listOf(ChatMessage(text = "how much did I spend?", sender = ChatMessage.Sender.USER))
        )

        composeTestRule.setContent {
            CestouTheme {
                AiChatScreen(state = state, intent = AiChatIntent())
            }
        }

        composeTestRule.onNodeWithText("how much did I spend?").assertExists()
    }
}
