package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.ChatMessage
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val BUBBLE_WIDTH_FRACTION = 0.82f
private const val BUBBLE_CORNER_LARGE = 20
private const val BUBBLE_CORNER_SMALL = 4
private const val ELEVATION_USER = 2
private const val ELEVATION_ASSISTANT = 6
private const val TIMESTAMP_ALPHA = 0.65f

@Composable
fun AiMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == ChatMessage.Sender.USER
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(BUBBLE_WIDTH_FRACTION),
            shape = RoundedCornerShape(
                topStart = BUBBLE_CORNER_LARGE.dp,
                topEnd = BUBBLE_CORNER_LARGE.dp,
                bottomStart = if (isUser) BUBBLE_CORNER_LARGE.dp else BUBBLE_CORNER_SMALL.dp,
                bottomEnd = if (isUser) BUBBLE_CORNER_SMALL.dp else BUBBLE_CORNER_LARGE.dp
            ),
            color = if (isUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            tonalElevation = if (isUser) ELEVATION_USER.dp else ELEVATION_ASSISTANT.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (message.sender == ChatMessage.Sender.ASSISTANT) {
                    Text(
                        text = stringResource(R.string.ai_sender_assistant),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(8.dp))
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        text = message.createdAt.atZone(ZoneId.systemDefault()).toLocalDateTime()
                            .format(formatter),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = TIMESTAMP_ALPHA)
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = TIMESTAMP_ALPHA)
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AiMessageBubbleUserPreview() {
    AiMessageBubble(
        message = ChatMessage(
            text = "Olá, como posso economizar hoje?",
            sender = ChatMessage.Sender.USER
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun AiMessageBubbleAiPreview() {
    AiMessageBubble(
        message = ChatMessage(
            text = "Você pode começar verificando as promoções de arroz e feijão no mercado mais próximo.",
            sender = ChatMessage.Sender.ASSISTANT
        )
    )
}
