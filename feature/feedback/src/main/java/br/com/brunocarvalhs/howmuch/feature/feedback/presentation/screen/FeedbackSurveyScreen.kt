package br.com.brunocarvalhs.howmuch.feature.feedback.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind
import br.com.brunocarvalhs.howmuch.feature.feedback.R
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.policy.FEATURE_AI_CHAT
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.policy.FEATURE_PHOTO_SCAN
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.policy.FEATURE_PURCHASE
import br.com.brunocarvalhs.howmuch.feature.feedback.presentation.components.LikertScale
import br.com.brunocarvalhs.howmuch.feature.feedback.presentation.components.NpsScale
import br.com.brunocarvalhs.howmuch.feature.feedback.presentation.intent.FeedbackSurveyIntent
import br.com.brunocarvalhs.howmuch.feature.feedback.presentation.state.FeedbackSurveyUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedbackSurveyScreen(
    state: FeedbackSurveyUiState,
    intent: FeedbackSurveyIntent,
    modifier: Modifier = Modifier
) {
    val kind = state.kind
    if (kind == null || kind == SurveyKind.STORE_REVIEW) return

    ModalBottomSheet(onDismissRequest = intent.onDismiss, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.submitted) {
                SubmittedContent(state, intent)
            } else {
                Text(
                    text = stringResource(questionRes(kind, state.feature)),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (kind == SurveyKind.NPS) {
                    NpsScale(selected = state.selectedScore, onSelect = intent.onScoreSelected)
                } else {
                    LikertScale(selected = state.selectedScore, onSelect = intent.onScoreSelected)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = intent.onDismiss, modifier = Modifier.testTag("feedback_dismiss")) {
                    Text(text = stringResource(R.string.feedback_not_now))
                }
            }
        }
    }
}

@Composable
private fun SubmittedContent(state: FeedbackSurveyUiState, intent: FeedbackSurveyIntent) = Column(
    horizontalAlignment = Alignment.CenterHorizontally
) {
    Text(text = stringResource(R.string.feedback_thanks_title), style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(if (state.offerContact) R.string.feedback_thanks_low else R.string.feedback_thanks),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    if (state.offerContact) {
        OutlinedButton(onClick = intent.onContactSupport, modifier = Modifier.testTag("feedback_contact")) {
            Text(text = stringResource(R.string.feedback_contact))
        }
    }
    Button(onClick = intent.onDismiss, modifier = Modifier.testTag("feedback_done")) {
        Text(text = stringResource(R.string.feedback_done))
    }
}

private fun questionRes(kind: SurveyKind, feature: String?): Int = when {
    kind == SurveyKind.NPS -> R.string.feedback_nps_question
    feature == FEATURE_PURCHASE -> R.string.feedback_csat_purchase
    feature == FEATURE_AI_CHAT -> R.string.feedback_csat_ai_chat
    feature == FEATURE_PHOTO_SCAN -> R.string.feedback_csat_photo_scan
    else -> R.string.feedback_csat_generic
}
