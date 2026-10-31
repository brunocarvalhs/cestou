package br.com.brunocarvalhs.howmuch.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    .withZone(ZoneId.systemDefault())

private fun Instant.toTimeLabel(): String = timeFormatter.format(this)

private const val BUBBLE_MAX_WIDTH_DP = 280
private const val BUBBLE_CORNER_RADIUS_DP = 18
private const val BUBBLE_TAIL_RADIUS_DP = 4

@Composable
internal fun ChatBubble(message: ChatMessage) {
    val isUser = message.sender == ChatMessage.Sender.USER
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = RoundedCornerShape(
        topStart = BUBBLE_CORNER_RADIUS_DP.dp,
        topEnd = BUBBLE_CORNER_RADIUS_DP.dp,
        bottomStart = if (isUser) BUBBLE_CORNER_RADIUS_DP.dp else BUBBLE_TAIL_RADIUS_DP.dp,
        bottomEnd = if (isUser) BUBBLE_TAIL_RADIUS_DP.dp else BUBBLE_CORNER_RADIUS_DP.dp,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            modifier = Modifier.widthIn(max = BUBBLE_MAX_WIDTH_DP.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Markdown(
                    content = message.text,
                    colors = markdownColor(
                        text = textColor,
                        codeBackground = textColor.copy(alpha = 0.12f),
                        inlineCodeBackground = textColor.copy(alpha = 0.12f),
                        dividerColor = textColor.copy(alpha = 0.3f),
                    ),
                    typography = markdownTypography(
                        text = MaterialTheme.typography.bodyMedium,
                        paragraph = MaterialTheme.typography.bodyMedium,
                        ordered = MaterialTheme.typography.bodyMedium,
                        bullet = MaterialTheme.typography.bodyMedium,
                        list = MaterialTheme.typography.bodyMedium,
                    ),
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message.createdAt.toTimeLabel(),
                    color = textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}
