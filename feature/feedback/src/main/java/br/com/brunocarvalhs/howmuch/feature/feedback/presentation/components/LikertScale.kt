package br.com.brunocarvalhs.howmuch.feature.feedback.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.brunocarvalhs.howmuch.feature.feedback.R

/** Emojis do Likert de 5 pontos, do pior (1) ao melhor (5); o emoji é o próprio controle de nota. */
internal val LIKERT_EMOJIS = listOf("😡", "🙁", "😐", "🙂", "😍")

private const val UNSELECTED_ALPHA = 0.55f
private const val EMOJI_SIZE_SP = 34
private const val TOUCH_TARGET_DP = 56

@Composable
internal fun LikertScale(
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val descriptions = listOf(
        R.string.feedback_likert_1,
        R.string.feedback_likert_2,
        R.string.feedback_likert_3,
        R.string.feedback_likert_4,
        R.string.feedback_likert_5
    ).map { stringResource(it) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LIKERT_EMOJIS.forEachIndexed { index, emoji ->
                val score = index + 1
                val isSelected = selected == score
                val label =
                    stringResource(R.string.feedback_likert_option, score, LIKERT_EMOJIS.size, descriptions[index])
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(TOUCH_TARGET_DP.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                        .clickable(role = Role.Button) { onSelect(score) }
                        .semantics {
                            contentDescription = label
                            role = Role.Button
                            this.selected = isSelected
                        }
                        .testTag("feedback_score_$score")
                ) {
                    Text(
                        text = emoji,
                        fontSize = EMOJI_SIZE_SP.sp,
                        modifier = Modifier.alpha(if (selected == null || isSelected) 1f else UNSELECTED_ALPHA)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.feedback_likert_low),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.feedback_likert_high),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
