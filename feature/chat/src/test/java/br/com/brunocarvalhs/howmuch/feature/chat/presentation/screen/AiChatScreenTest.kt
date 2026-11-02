package br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreUiR

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
    fun `tapping send reports the intent when there is text to send`() {
        var sendCalled = false

        composeTestRule.setContent {
            CestouTheme {
                AiChatScreen(
                    state = AiChatUiState(input = "quanto gastei?"),
                    intent = AiChatIntent(onSendMessage = { sendCalled = true }),
                )
            }
        }

        val sendDescription = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(CoreUiR.string.content_description_send_message)
        composeTestRule.onNodeWithContentDescription(sendDescription).performClick()

        assertEquals(true, sendCalled)
    }

    @Test
    fun `shows the voice button instead of send while the input is empty`() {
        composeTestRule.setContent {
            CestouTheme {
                AiChatScreen(state = AiChatUiState(input = ""), intent = AiChatIntent())
            }
        }

        val voiceDescription = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.ai_chat_voice_content_description)
        val sendDescription = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(CoreUiR.string.content_description_send_message)

        composeTestRule.onNodeWithContentDescription(voiceDescription).assertExists()
        composeTestRule.onNodeWithContentDescription(sendDescription).assertDoesNotExist()
    }

    @Test
    fun `tapping the voice button does not crash when audio permission is not yet granted`() {
        composeTestRule.setContent {
            CestouTheme {
                AiChatScreen(state = AiChatUiState(input = ""), intent = AiChatIntent())
            }
        }

        val voiceDescription = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.ai_chat_voice_content_description)
        composeTestRule.onNodeWithContentDescription(voiceDescription).performClick()

        composeTestRule.onNodeWithContentDescription(voiceDescription).assertExists()
    }
}
