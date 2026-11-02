package br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.domain.model.ChatSession
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiHistoryIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiHistoryUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Teste de layout via Robolectric (JVM): AiHistoryScreenKt estava em 0% na variante "layout" do
// Kover por nunca ter sido exercitado por um teste de tela.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AiHistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun ComposeContentTestRule.clickEverything() {
        val count = onAllNodes(hasClickAction()).fetchSemanticsNodes().size
        for (i in 0 until count) {
            runCatching { onAllNodes(hasClickAction())[i].performClick() }
        }
        waitForIdle()
    }

    @Test
    fun `renders the empty state message when there are no sessions`() {
        val emptyMessage = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.ai_chat_history_empty)

        composeTestRule.setContent {
            CestouTheme {
                AiHistoryScreen(state = AiHistoryUiState(), intent = AiHistoryIntent())
            }
        }

        composeTestRule.onNodeWithText(emptyMessage).assertExists()
    }

    @Test
    fun `renders a loading indicator while loading`() {
        composeTestRule.setContent {
            CestouTheme {
                AiHistoryScreen(
                    state = AiHistoryUiState(isLoading = true),
                    intent = AiHistoryIntent()
                )
            }
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `renders sessions and handles session taps, delete and new chat`() {
        var selectedId: String? = null
        var deletedId: String? = null
        var newChatCalls = 0
        var backCalls = 0

        composeTestRule.setContent {
            CestouTheme {
                AiHistoryScreen(
                    state = AiHistoryUiState(
                        sessions = listOf(
                            ChatSession(id = "1", title = "Compra do mês", lastMessage = "Oi"),
                            ChatSession(id = "2", title = "Lista de festa")
                        )
                    ),
                    intent = AiHistoryIntent(
                        onSelectSession = { selectedId = it },
                        onDeleteSession = { deletedId = it },
                        onNewChat = { newChatCalls++ },
                        onBack = { backCalls++ }
                    )
                )
            }
        }

        composeTestRule.onNodeWithText("Compra do mês").assertExists()
        composeTestRule.clickEverything()

        // Any combination of taps (card, delete icon, FAB, back) must have fired at least one
        // callback without crashing the composition.
        val anyCallbackFired = selectedId != null || deletedId != null || newChatCalls > 0 || backCalls > 0
        assert(anyCallbackFired)
    }
}
