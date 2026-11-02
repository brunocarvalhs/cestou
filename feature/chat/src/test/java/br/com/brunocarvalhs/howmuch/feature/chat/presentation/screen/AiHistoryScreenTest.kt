package br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.domain.model.ChatSession
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiHistoryIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiHistoryUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Teste de layout via Robolectric (JVM), não androidTest: valida a composição real da tela
// sem depender de emulador, adequado para rodar na esteira de CI.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AiHistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(resId: Int) =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(resId)

    @Test
    fun `shows empty message when there are no sessions`() {
        composeTestRule.setContent {
            CestouTheme {
                AiHistoryScreen(state = AiHistoryUiState(), intent = AiHistoryIntent())
            }
        }

        composeTestRule.onNodeWithText(string(R.string.ai_chat_history_empty)).assertExists()
    }

    @Test
    fun `hides empty message while loading`() {
        composeTestRule.setContent {
            CestouTheme {
                AiHistoryScreen(state = AiHistoryUiState(isLoading = true), intent = AiHistoryIntent())
            }
        }

        composeTestRule.onNodeWithText(string(R.string.ai_chat_history_empty)).assertDoesNotExist()
    }

    @Test
    fun `renders a session card with its title and last message`() {
        val session = ChatSession(
            id = "s1",
            title = "Compras da semana",
            lastMessage = "Quanto vou gastar nessa lista?",
            updatedAtEpochMillis = 0L,
        )

        composeTestRule.setContent {
            CestouTheme {
                AiHistoryScreen(state = AiHistoryUiState(sessions = listOf(session)), intent = AiHistoryIntent())
            }
        }

        composeTestRule.onNodeWithText("Compras da semana").assertExists()
        composeTestRule.onNodeWithText("Quanto vou gastar nessa lista?").assertExists()
    }

    @Test
    fun `hides the last message line when the session has none yet`() {
        val session = ChatSession(id = "s1", title = "Compras da semana", lastMessage = "")

        composeTestRule.setContent {
            CestouTheme {
                AiHistoryScreen(state = AiHistoryUiState(sessions = listOf(session)), intent = AiHistoryIntent())
            }
        }

        composeTestRule.onNodeWithText("Compras da semana").assertExists()
    }

    @Test
    fun `selecting a session reports its id`() {
        val session = ChatSession(id = "s1", title = "Compras da semana")
        var selectedId: String? = null

        composeTestRule.setContent {
            CestouTheme {
                AiHistoryScreen(
                    state = AiHistoryUiState(sessions = listOf(session)),
                    intent = AiHistoryIntent(onSelectSession = { selectedId = it }),
                )
            }
        }

        composeTestRule.onNodeWithText("Compras da semana").performClick()

        assertEquals("s1", selectedId)
    }

    @Test
    fun `deleting a session reports its id without selecting it`() {
        val session = ChatSession(id = "s1", title = "Compras da semana")
        var deletedId: String? = null
        var selectedId: String? = null

        composeTestRule.setContent {
            CestouTheme {
                AiHistoryScreen(
                    state = AiHistoryUiState(sessions = listOf(session)),
                    intent = AiHistoryIntent(
                        onDeleteSession = { deletedId = it },
                        onSelectSession = { selectedId = it },
                    ),
                )
            }
        }

        val deleteDescription = string(R.string.ai_chat_history_delete_content_description)
        composeTestRule.onNodeWithContentDescription(deleteDescription).performClick()

        assertEquals("s1", deletedId)
        assertEquals(null, selectedId)
    }

    @Test
    fun `tapping the new chat button reports the intent`() {
        var newChatCalled = false

        composeTestRule.setContent {
            CestouTheme {
                AiHistoryScreen(
                    state = AiHistoryUiState(),
                    intent = AiHistoryIntent(onNewChat = { newChatCalled = true }),
                )
            }
        }

        val newChatDescription = string(R.string.ai_chat_history_new_chat)
        composeTestRule.onNodeWithContentDescription(newChatDescription).performClick()

        assertEquals(true, newChatCalled)
    }

    @Test
    fun `tapping back reports the intent`() {
        var backCalled = false

        composeTestRule.setContent {
            CestouTheme {
                AiHistoryScreen(
                    state = AiHistoryUiState(),
                    intent = AiHistoryIntent(onBack = { backCalled = true }),
                )
            }
        }

        val closeDescription = string(R.string.ai_chat_close_content_description)
        composeTestRule.onNodeWithContentDescription(closeDescription).performClick()

        assertEquals(true, backCalled)
    }
}
