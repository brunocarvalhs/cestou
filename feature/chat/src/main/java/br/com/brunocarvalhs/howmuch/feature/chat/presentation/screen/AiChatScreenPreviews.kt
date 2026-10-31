package br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen

import androidx.compose.runtime.Composable
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.theme.PreviewCestouScreens
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.intent.AiChatIntent
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiChatUiState

private val previewMessages = listOf(
    ChatMessage(id = 1, text = "Quanto vou gastar nessa lista?", sender = ChatMessage.Sender.USER),
    ChatMessage(
        id = 2,
        text = "Com base nos itens adicionados, o total estimado é de R$ 87,40.",
        sender = ChatMessage.Sender.ASSISTANT,
    ),
    ChatMessage(
        id = 3, text = "Consegue sugerir algo mais barato?", sender = ChatMessage.Sender.USER,
    ),
)

@PreviewCestouScreens
@Composable
private fun AiChatScreenEmptyPreview() {
    CestouTheme {
        AiChatScreen(
            state = AiChatUiState(), intent = AiChatIntent(),
        )
    }
}

@PreviewCestouScreens
@Composable
private fun AiChatScreenConversationProgressPreview() {
    CestouTheme {
        AiChatScreen(
            state = AiChatUiState(
                isRecording = true,
            ), intent = AiChatIntent(),
        )
    }
}

@PreviewCestouScreens
@Composable
private fun AiChatScreenConversationPreview() {
    CestouTheme {
        AiChatScreen(
            state = AiChatUiState(
                messages = previewMessages, input = "Consegue sugerir algo mais barato?",
            ), intent = AiChatIntent(),
        )
    }
}

@PreviewCestouScreens
@Composable
private fun AiChatScreenLoadingPreview() {
    CestouTheme {
        AiChatScreen(
            state = AiChatUiState(
                messages = previewMessages, isLoading = true,
            ),
            intent = AiChatIntent(),
        )
    }
}

@PreviewCestouScreens
@Composable
private fun AiChatScreenTrialUsedPreview() {
    CestouTheme {
        AiChatScreen(
            state = AiChatUiState(messages = previewMessages),
            intent = AiChatIntent(),
            isSendEnabled = false,
        )
    }
}
