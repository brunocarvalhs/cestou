package br.com.brunocarvalhs.howmuch.feature.feedback.presentation.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.common.extensions.openEmail
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind
import br.com.brunocarvalhs.howmuch.feature.feedback.R
import br.com.brunocarvalhs.howmuch.feature.feedback.data.review.PlayReviewLauncher
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.usecase.DismissSurveyUseCase
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.usecase.SubmitCsatUseCase
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.usecase.SubmitNpsUseCase
import br.com.brunocarvalhs.howmuch.feature.feedback.presentation.helper.FeedbackTelemetry
import br.com.brunocarvalhs.howmuch.feature.feedback.presentation.intent.FeedbackSurveyIntent
import br.com.brunocarvalhs.howmuch.feature.feedback.presentation.state.FeedbackSurveyUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Mesmo endereço do canal de Sugestão/Feedback em Configurações. */
internal const val FEEDBACK_SUPPORT_EMAIL = "brunocarvalhs@outlook.com.br"

private const val NPS_LOW_MAX = 6
private const val CSAT_LOW_MAX = 2

@HiltViewModel
internal class FeedbackSurveyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val submitNps: SubmitNpsUseCase,
    private val submitCsat: SubmitCsatUseCase,
    private val dismissSurvey: DismissSurveyUseCase,
    private val reviewLauncher: PlayReviewLauncher,
    analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val telemetry = FeedbackTelemetry(analyticsTracker)
    private var prompt: FeedbackPrompt? = null

    private val _uiState = MutableStateFlow(FeedbackSurveyUiState())
    val uiState: StateFlow<FeedbackSurveyUiState> = _uiState.asStateFlow()

    val intent = FeedbackSurveyIntent(
        onScoreSelected = { score -> selectScore(score) },
        onDismiss = { dismiss() },
        onContactSupport = { contactSupport() },
    )

    /** Recebe o pedido da rota; chamadas repetidas (recomposição, rotação) não reiniciam a pesquisa. */
    fun setPrompt(kind: String, feature: String?) {
        if (prompt != null) return
        val surveyKind = SurveyKind.entries.firstOrNull { it.name.equals(kind, ignoreCase = true) } ?: return
        prompt = FeedbackPrompt(surveyKind, feature)
        _uiState.update { it.copy(kind = surveyKind, feature = feature) }
    }

    /** Abre o In-App Review; falhas ficam no [PlayReviewLauncher] e nunca bloqueiam a UI. */
    suspend fun requestStoreReview(activity: Activity): Boolean = reviewLauncher.launch(activity)

    private fun selectScore(score: Int) {
        val current = prompt
        if (current == null || _uiState.value.submitted || current.kind == SurveyKind.STORE_REVIEW) return
        if (current.kind == SurveyKind.NPS) submitNps(current, score) else submitCsat(current, score)
        telemetry.scoreSubmitted(current, score)
        val low = if (current.kind == SurveyKind.NPS) score <= NPS_LOW_MAX else score <= CSAT_LOW_MAX
        _uiState.update { it.copy(selectedScore = score, submitted = true, offerContact = low) }
    }

    private fun dismiss() {
        val current = prompt ?: return
        if (_uiState.value.submitted) return
        dismissSurvey(current)
        telemetry.dismissed(current)
        prompt = null
    }

    private fun contactSupport() {
        val current = prompt ?: return
        telemetry.contactOpened(current)
        context.openEmail(FEEDBACK_SUPPORT_EMAIL, context.getString(R.string.feedback_contact_subject))
    }
}
