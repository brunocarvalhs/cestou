package br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.extensions.trackAction
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsBuckets
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsElements
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.analytics.service.FirstTimeEventTracker
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiTurnInfo
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.ReasonChip
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.Vote
import java.io.IOException
import kotlin.time.TimeMark

private const val INPUT_TEXT = "text"
private const val INPUT_VOICE = "voice"
private const val FEATURE_AI_CHAT = "ai_chat"
private const val FEATURE_VOICE = "voice_input"
private const val ERROR_NETWORK = "network"
private const val ERROR_GENERIC = "generic"
private const val PROVIDER_NONE = "none"

/** Motivos de falha da entrada por voz, enviados como `reason`. */
internal enum class VoiceFailure(val value: String) {
    UNSUPPORTED("unsupported"),
    RECOGNITION("recognition")
}

/**
 * Telemetria do chat com a IA. Só tamanhos em faixas, latência e tipo de erro: o texto das
 * mensagens (do usuário ou da IA) nunca sai daqui.
 */
internal class AiChatAnalytics(
    private val analyticsTracker: AnalyticsTracker,
    private val firstTimeEventTracker: FirstTimeEventTracker
) {

    fun historyToggled() = analyticsTracker.trackAction(AnalyticsElements.AI_CHAT_TOGGLE_HISTORY)

    fun voiceStarted() {
        analyticsTracker.trackEvent(AnalyticsEvents.AI_VOICE_INPUT_STARTED)
        firstTimeEventTracker.trackFeatureFirstUsed(FEATURE_VOICE)
    }

    fun voiceFailed(failure: VoiceFailure) {
        analyticsTracker.trackEvent(
            AnalyticsEvents.AI_VOICE_INPUT_FAILED,
            mapOf(AnalyticsParams.REASON to failure.value)
        )
    }

    fun messageSent(textLength: Int, fromVoice: Boolean, hasShoppingContext: Boolean) {
        analyticsTracker.trackEvent(
            AnalyticsEvents.AI_CHAT_MESSAGE_SENT,
            mapOf(
                AnalyticsParams.INPUT_METHOD to if (fromVoice) INPUT_VOICE else INPUT_TEXT,
                AnalyticsParams.LENGTH_BUCKET to AnalyticsBuckets.length(textLength),
                AnalyticsParams.ENABLED to hasShoppingContext
            )
        )
        firstTimeEventTracker.trackFeatureFirstUsed(FEATURE_AI_CHAT)
    }

    /** Só tamanho, provider e contagens: o texto da resposta nunca sai do aparelho. */
    fun responseReceived(sentAt: TimeMark, responseLength: Int, session: AiSession) {
        analyticsTracker.trackEvent(
            AnalyticsEvents.AI_RESPONSE_RECEIVED,
            mapOf(
                AnalyticsParams.LATENCY_MS to sentAt.elapsedNow().inWholeMilliseconds,
                AnalyticsParams.LENGTH_BUCKET to AnalyticsBuckets.length(responseLength),
                AnalyticsParams.PROVIDER to (session.providerUsed ?: PROVIDER_NONE),
                AnalyticsParams.TOOL_CALLS to session.toolCallsThisTurn,
                AnalyticsParams.FALLBACK_USED to session.fallbackUsedThisTurn
            )
        )
    }

    /**
     * Polegar numa resposta. O toque no 👎 sai sem `reason`; o chip que vem depois repete o evento
     * com `reason`, então o total de 👎 é a contagem sem `reason` e os motivos são o detalhe.
     * Nunca leva o texto da resposta.
     */
    fun messageVoted(vote: Vote, turn: AiTurnInfo?, reason: ReasonChip? = null) {
        analyticsTracker.trackEvent(
            AnalyticsEvents.AI_MESSAGE_FEEDBACK,
            buildMap {
                put(AnalyticsParams.VALUE, vote.value)
                put(AnalyticsParams.PROVIDER, turn?.provider ?: PROVIDER_NONE)
                put(AnalyticsParams.TOOL_CALLS, turn?.toolCalls ?: 0)
                reason?.let { put(AnalyticsParams.REASON, it.value) }
            }
        )
    }

    fun trialMessageUsed() = analyticsTracker.trackEvent(AnalyticsEvents.AI_TRIAL_MESSAGE_USED)

    fun responseFailed(error: Exception) {
        analyticsTracker.trackEvent(
            AnalyticsEvents.AI_RESPONSE_FAILED,
            mapOf(AnalyticsParams.ERROR_TYPE to if (error is IOException) ERROR_NETWORK else ERROR_GENERIC)
        )
    }
}
