package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.AiDockState
import br.com.brunocarvalhs.howmuch.feature.chat.domain.entity.ChatMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CartAiComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val messages = listOf(
        ChatMessage(id = 1, text = "Olá, tudo bem?", sender = ChatMessage.Sender.USER),
        ChatMessage(id = 2, text = "Tudo bem, como posso ajudar?", sender = ChatMessage.Sender.ASSISTANT)
    )

    @Test
    fun `message bubble renders user and assistant text`() {
        composeTestRule.setContent {
            CestouTheme {
                Column {
                    AiMessageBubble(message = messages[0])
                    AiMessageBubble(message = messages[1])
                }
            }
        }
        composeTestRule.onNodeWithText("Olá, tudo bem?").assertExists()
        composeTestRule.onNodeWithText("Tudo bem, como posso ajudar?").assertExists()
    }

    @Test
    fun `typing indicator renders`() {
        composeTestRule.setContent { CestouTheme { AiTypingIndicator() } }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `assistant chat renders messages while loading`() {
        composeTestRule.setContent {
            CestouTheme { CartAssistantChat(listState = LazyListState(), messages = messages, loading = true) }
        }
        composeTestRule.onNodeWithText("Olá, tudo bem?").assertExists()
    }

    @Test
    fun `input bar renders expanded and collapsed`() {
        composeTestRule.setContent {
            CestouTheme {
                Column {
                    AiInputBar(
                        value = "leite", expanded = true, onValueChange = {}, onSendClick = {},
                        onAddClick = {}, onFocused = {}, onNotFocused = {}
                    )
                    AiInputBar(
                        value = "", expanded = false, onValueChange = {}, onSendClick = {},
                        onAddClick = {}, onFocused = {}, onNotFocused = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("leite").assertExists()
    }

    @Test
    fun `dock renders every state`() {
        composeTestRule.setContent {
            CestouTheme {
                Column {
                    AiDockState.entries.forEach { state ->
                        CartAssistantDock(
                            state = state,
                            suggestions = listOf("Somar lista"),
                            messages = StableList(messages),
                            loading = true,
                            value = "",
                            onValueChange = {}, onSendClick = {}, onAddClick = {}, onToggleClick = {}
                        )
                    }
                }
            }
        }
        composeTestRule.onRoot().assertExists()
    }
}
