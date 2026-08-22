package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouBrightGreen
import br.com.brunocarvalhs.howmuch.core.theme.CestouDarkGreen
import br.com.brunocarvalhs.howmuch.feature.chat.domain.entity.ChatMessage

@Composable
fun AiMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == ChatMessage.Sender.USER

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CestouBrightGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = CestouBrightGreen
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(0.85f),
            shape = RoundedCornerShape(
                topStart = if (isUser) 20.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 20.dp,
                bottomStart = 20.dp,
                bottomEnd = 20.dp
            ),
            color = if (isUser) CestouDarkGreen else Color.White,
            border = if (!isUser) BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f)) else null
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUser) Color.White else Color.Black
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AiMessageBubbleUserPreview() {
    AiMessageBubble(
        message = ChatMessage(
            text = "Quanto vou gastar na lista da semana?",
            sender = ChatMessage.Sender.USER
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun AiMessageBubbleAiPreview() {
    AiMessageBubble(
        message = ChatMessage(
            text = "Fiz a soma prévia dos itens da lista: Total Estimado R$ 50,70",
            sender = ChatMessage.Sender.ASSISTANT
        )
    )
}
