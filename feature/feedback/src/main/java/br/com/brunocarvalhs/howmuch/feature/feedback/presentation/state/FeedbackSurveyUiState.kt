package br.com.brunocarvalhs.howmuch.feature.feedback.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind

@Stable
internal data class FeedbackSurveyUiState(
    val kind: SurveyKind? = null,
    val feature: String? = null,
    val selectedScore: Int? = null,
    val submitted: Boolean = false,
    val offerContact: Boolean = false
)
