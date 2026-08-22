package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.model.ChatMessage

@Composable
fun ChatScreen(
    listState: LazyListState,
    messages: List<ChatMessage>,
    loading: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = messages, key = { it.id }) {
            AiMessageBubble(it)
        }
        if (loading) {
            item {
                AiTypingIndicator(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Preview
@Composable
private fun ChatScreenPreview() {
    ChatScreen(
        listState = LazyListState(),
        messages = listOf(
            ChatMessage(
                id = 1,
                text = "Olá, tudo bem?",
                sender = ChatMessage.Sender.USER
            ),
            ChatMessage(
                id = 2,
                text = "Tudo bem, como posso ajudar?",
                sender = ChatMessage.Sender.ASSISTANT
            ),
            ChatMessage(
                id = 3,
                text = "Gostaria de adicionar um item à minha lista.",
                sender = ChatMessage.Sender.USER
            ),
            ChatMessage(
                id = 4,
                text = "Certo, qual item você gostaria de adicionar?",
                sender = ChatMessage.Sender.ASSISTANT
            ),
        ),
        loading = false
    )
}
