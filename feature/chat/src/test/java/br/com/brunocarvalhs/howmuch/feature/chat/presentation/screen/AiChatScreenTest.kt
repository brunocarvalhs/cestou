package br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState
import org.junit.Assert.assertEquals
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

    @Test
    fun `shows the upgrade banner when the trial is spent`() {
        val bannerText = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.ai_chat_upgrade_banner)

        composeTestRule.setContent {
            CestouTheme {
                AiChatScreen(state = AiChatUiState(), intent = AiChatIntent(), isSendEnabled = false)
            }
        }

        composeTestRule.onNodeWithText(bannerText).assertExists()
    }

    @Test
    fun `hides the upgrade banner when send is enabled`() {
        val bannerText = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.ai_chat_upgrade_banner)

        composeTestRule.setContent {
            CestouTheme {
                AiChatScreen(state = AiChatUiState(), intent = AiChatIntent(), isSendEnabled = true)
            }
        }

        composeTestRule.onNodeWithText(bannerText).assertDoesNotExist()
    }

    @Test
    fun `shows suggestion chips on the empty state and sends the tapped one`() {
        var selectedId: String? = null
        var selectedText: String? = null

        composeTestRule.setContent {
            CestouTheme {
                AiChatScreen(
                    state = AiChatUiState(),
                    intent = AiChatIntent(
                        onSuggestionSelected = { id, text ->
                            selectedId = id
                            selectedText = text
                        }
                    )
                )
            }
        }

        val recipeText = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.ai_chat_suggestion_recipe)

        composeTestRule.onNodeWithTag("ai_chat_suggestion_recipe").performClick()

        assertEquals("recipe", selectedId)
        assertEquals(recipeText, selectedText)
    }

    @Test
    fun `disables empty-state suggestion chips when the trial is spent`() {
        // Regression: the chips used to call onSuggestionSelected unconditionally, letting a
        // free user who already spent their one message keep chatting by tapping chips instead
        // of using the (correctly disabled) text field and send button.
        var tapped = false

        composeTestRule.setContent {
            CestouTheme {
                AiChatScreen(
                    state = AiChatUiState(),
                    intent = AiChatIntent(onSuggestionSelected = { _, _ -> tapped = true }),
                    isSendEnabled = false
                )
            }
        }

        composeTestRule.onNodeWithTag("ai_chat_suggestion_recipe").assertIsNotEnabled()
        assertEquals(false, tapped)
    }

    @Test
    fun `shows follow-up chips after the latest assistant reply`() {
        val state = AiChatUiState(
            messages = listOf(
                ChatMessage(text = "add milk", sender = ChatMessage.Sender.USER),
                ChatMessage(text = "done", sender = ChatMessage.Sender.ASSISTANT)
            )
        )

        composeTestRule.setContent {
            CestouTheme {
                AiChatScreen(state = state, intent = AiChatIntent())
            }
        }

        composeTestRule.onNodeWithTag("ai_chat_followup_add_all").assertExists()
    }

    @Test
    fun `disables follow-up chips when the trial is spent`() {
        val state = AiChatUiState(
            messages = listOf(
                ChatMessage(text = "add milk", sender = ChatMessage.Sender.USER),
                ChatMessage(text = "done", sender = ChatMessage.Sender.ASSISTANT)
            )
        )

        composeTestRule.setContent {
            CestouTheme {
                AiChatScreen(state = state, intent = AiChatIntent(), isSendEnabled = false)
            }
        }

        composeTestRule.onNodeWithTag("ai_chat_followup_add_all").assertIsNotEnabled()
    }

    @Test
    fun `hides follow-up chips while waiting for a reply`() {
        val state = AiChatUiState(
            messages = listOf(ChatMessage(text = "add milk", sender = ChatMessage.Sender.USER)),
            isLoading = true
        )

        composeTestRule.setContent {
            CestouTheme {
                AiChatScreen(state = state, intent = AiChatIntent())
            }
        }

        composeTestRule.onNodeWithTag("ai_chat_followup_add_all").assertDoesNotExist()
    }

    @Test
    fun `shows the error banner when the state carries one`() {
        composeTestRule.setContent {
            CestouTheme {
                AiChatScreen(
                    state = AiChatUiState(error = UiText.DynamicString("Could not reply")),
                    intent = AiChatIntent()
                )
            }
        }

        composeTestRule.onNodeWithText("Could not reply").assertExists()
    }
}
