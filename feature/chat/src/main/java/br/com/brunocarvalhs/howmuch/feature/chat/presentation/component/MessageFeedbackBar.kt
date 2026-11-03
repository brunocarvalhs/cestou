package br.com.brunocarvalhs.howmuch.feature.chat.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.MessageVote
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.ReasonChip
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.Vote

private const val UNCHOSEN_ALPHA = 0.4f
private const val VOTE_TARGET_DP = 36
private const val VOTE_ICON_DP = 18
private const val VOTE_GAP_DP = 4

/**
 * Avaliação binária da resposta: polegar para cima ou para baixo, com os ícones do próprio app (o
 * emoji destoava do resto). Depois do voto a barra trava (um voto por mensagem) e, no polegar para
 * baixo, oferece motivos de uma lista fechada.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MessageFeedbackBar(
    messageId: Long,
    vote: MessageVote?,
    onVote: (Long, Vote) -> Unit,
    onReason: (Long, ReasonChip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(VOTE_GAP_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VoteIcon(
                icon = if (vote?.value == Vote.UP) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                description = stringResource(R.string.ai_chat_vote_up),
                chosen = vote?.value == Vote.UP,
                locked = vote != null,
                tag = "message_vote_up",
                onClick = { onVote(messageId, Vote.UP) },
            )
            VoteIcon(
                icon = if (vote?.value == Vote.DOWN) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                description = stringResource(R.string.ai_chat_vote_down),
                chosen = vote?.value == Vote.DOWN,
                locked = vote != null,
                tag = "message_vote_down",
                onClick = { onVote(messageId, Vote.DOWN) },
            )
        }
        if (vote?.value == Vote.DOWN && vote.reason == null) {
            Text(
                text = stringResource(R.string.ai_chat_vote_reason_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ReasonChip.entries.forEach { chip ->
                    AssistChip(
                        onClick = { onReason(messageId, chip) },
                        label = { Text(text = stringResource(chip.label())) },
                        modifier = Modifier.testTag("message_vote_reason_${chip.value}"),
                    )
                }
            }
        }
    }
}

@Composable
private fun VoteIcon(
    icon: ImageVector,
    description: String,
    chosen: Boolean,
    locked: Boolean,
    tag: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(VOTE_TARGET_DP.dp)
            .clip(CircleShape)
            .clickable(enabled = !locked, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(tag),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(VOTE_ICON_DP.dp)
                .alpha(if (locked && !chosen) UNCHOSEN_ALPHA else 1f),
            tint = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun ReasonChip.label(): Int = when (this) {
    ReasonChip.WRONG_ANSWER -> R.string.ai_chat_vote_reason_wrong_answer
    ReasonChip.NOT_UNDERSTOOD -> R.string.ai_chat_vote_reason_not_understood
    ReasonChip.TOO_SLOW -> R.string.ai_chat_vote_reason_too_slow
    ReasonChip.DID_NOTHING -> R.string.ai_chat_vote_reason_did_nothing
    ReasonChip.OTHER -> R.string.ai_chat_vote_reason_other
}
