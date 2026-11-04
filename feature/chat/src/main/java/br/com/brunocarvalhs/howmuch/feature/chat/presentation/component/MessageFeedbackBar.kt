package br.com.brunocarvalhs.howmuch.feature.chat.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.MessageVote
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.ReasonChip
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.Vote

private const val UNCHOSEN_ALPHA = 0.4f
private const val VOTE_TARGET_DP = 48
private const val VOTE_EMOJI_SP = 22

/**
 * Avaliação binária da resposta, por emoji: 👍 ou 👎. Depois do voto a barra trava (um voto por
 * mensagem) e, no 👎, oferece motivos de uma lista fechada.
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
    Column(modifier = modifier.padding(start = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VoteEmoji(
                emoji = "👍",
                description = stringResource(R.string.ai_chat_vote_up),
                chosen = vote?.value == Vote.UP,
                locked = vote != null,
                tag = "message_vote_up",
                onClick = { onVote(messageId, Vote.UP) },
            )
            VoteEmoji(
                emoji = "👎",
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
private fun VoteEmoji(
    emoji: String,
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
            .background(if (chosen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(enabled = !locked, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .testTag(tag),
    ) {
        Text(
            text = emoji,
            fontSize = VOTE_EMOJI_SP.sp,
            modifier = Modifier.alpha(if (locked && !chosen) UNCHOSEN_ALPHA else 1f),
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
