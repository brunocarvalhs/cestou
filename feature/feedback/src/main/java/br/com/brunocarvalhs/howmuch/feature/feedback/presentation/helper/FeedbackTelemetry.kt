package br.com.brunocarvalhs.howmuch.feature.feedback.presentation.helper

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsUserProperties
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind

/**
 * Telemetria das pesquisas. Só enums e inteiros: nunca texto digitado nem conteúdo de mensagem.
 */
internal class FeedbackTelemetry(private val analyticsTracker: AnalyticsTracker) {

    fun scoreSubmitted(prompt: FeedbackPrompt, score: Int) {
        when (prompt.kind) {
            SurveyKind.NPS -> {
                val npsClass = AnalyticsUserProperties.npsClass(score)
                analyticsTracker.trackEvent(
                    AnalyticsEvents.NPS_SUBMITTED,
                    mapOf(AnalyticsParams.SCORE to score, AnalyticsParams.VALUE to npsClass)
                )
                analyticsTracker.setUserProperty(AnalyticsUserProperties.NPS_CLASS, npsClass)
            }
            SurveyKind.CSAT -> analyticsTracker.trackEvent(
                AnalyticsEvents.CSAT_SUBMITTED,
                mapOf(AnalyticsParams.FEATURE to prompt.feature.orEmpty(), AnalyticsParams.SCORE to score)
            )
            SurveyKind.STORE_REVIEW -> Unit
        }
    }

    fun dismissed(prompt: FeedbackPrompt) = analyticsTracker.trackEvent(
        AnalyticsEvents.FEEDBACK_PROMPT_DISMISSED,
        kindParams(prompt)
    )

    fun contactOpened(prompt: FeedbackPrompt) = analyticsTracker.trackEvent(
        AnalyticsEvents.FEEDBACK_CONTACT_OPENED,
        kindParams(prompt)
    )

    private fun kindParams(prompt: FeedbackPrompt) = mapOf(
        AnalyticsParams.KIND to prompt.kind.name.lowercase(),
        AnalyticsParams.FEATURE to prompt.feature.orEmpty()
    )
}
