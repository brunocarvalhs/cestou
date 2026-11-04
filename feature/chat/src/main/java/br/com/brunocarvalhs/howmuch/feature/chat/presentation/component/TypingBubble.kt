package br.com.brunocarvalhs.howmuch.feature.chat.presentation.component

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouAiLoader

private const val BUBBLE_CORNER_RADIUS_DP = 18
private const val BUBBLE_TAIL_RADIUS_DP = 4

/**
 * Lottie no balão e, fora dele, o texto do que a IA está fazendo, como no Gemini. O texto é só
 * andamento: some quando o turno acaba e nunca entra no histórico.
 */
@Composable
internal fun TypingBubble(@StringRes activityLabel: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = BUBBLE_CORNER_RADIUS_DP.dp,
                    topEnd = BUBBLE_CORNER_RADIUS_DP.dp,
                    bottomStart = BUBBLE_TAIL_RADIUS_DP.dp,
                    bottomEnd = BUBBLE_CORNER_RADIUS_DP.dp,
                ),
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CestouAiLoader()
                }
            }
        }
        AnimatedContent(targetState = activityLabel, label = "ai_activity") { label ->
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp).testTag("ai_chat_activity_text"),
            )
        }
    }
}
