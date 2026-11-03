package br.com.brunocarvalhs.howmuch.feature.feedback.domain.policy

import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackMoment
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.model.FeedbackConfig
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.model.FeedbackState
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.model.PromptContext
import java.util.concurrent.TimeUnit

internal const val FEATURE_PURCHASE = "purchase"
internal const val FEATURE_AI_CHAT = "ai_chat"
internal const val FEATURE_PHOTO_SCAN = "photo_scan"

private const val MIN_ACCOUNT_AGE_DAYS = 1L
private const val ANY_SURVEY_COOLDOWN_DAYS = 7L
private const val NPS_MIN_DAYS_SINCE_FIRST_USE = 7L
private const val NPS_MIN_SESSIONS = 3
private const val FIRST_PURCHASE = 1
private const val THIRD_PURCHASE = 3
private const val FIRST_DISMISSAL_BACKOFF_DAYS = 14L
private const val SECOND_DISMISSAL_BACKOFF_DAYS = 45L
private const val NEXT_DISMISSAL_BACKOFF_DAYS = 90L
private const val STORE_REVIEW_MIN_PURCHASES = 3
private const val STORE_REVIEW_MIN_DAYS_SINCE_FIRST_USE = 14L
private const val STORE_REVIEW_COOLDOWN_DAYS = 120L
private const val STORE_REVIEW_LOW_SCORE_DAYS = 30L

/**
 * Decide se e quando perguntar. Função pura sobre estado em memória: roda só em lambdas de intent,
 * nunca em recomposição. [clock] devolve epoch millis.
 */
internal class FeedbackPolicy(
    private val config: FeedbackConfig,
    private val clock: () -> Long
) {

    /** Pedido de pesquisa liberado para [moment], ou `null` para não perguntar. */
    fun evaluate(moment: FeedbackMoment, state: FeedbackState, ctx: PromptContext): FeedbackPrompt? {
        if (!canAsk(state, ctx)) return null
        val csat = csatFeature(moment, state)
        return when {
            csat != null -> csat.takeIf { csatOffCooldown(it, state) }?.let { FeedbackPrompt(SurveyKind.CSAT, it) }
            moment == FeedbackMoment.APP_RELATIONSHIP && npsEligible(state) -> FeedbackPrompt(SurveyKind.NPS)
            else -> null
        }
    }

    /**
     * Avaliação da loja disparada por marco de uso, nunca logo depois de uma pesquisa e nunca para
     * quem deu nota baixa recentemente (a Play desaconselha condicionar o pedido a uma pergunta de
     * sentimento).
     */
    fun shouldRequestStoreReview(state: FeedbackState, ctx: PromptContext): Boolean {
        val allowed = config.enabled && config.storeReviewEnabled && ctx.analyticsEnabled
        return allowed &&
            state.purchasesFinished >= STORE_REVIEW_MIN_PURCHASES &&
            daysSince(state.firstSeenAt) >= STORE_REVIEW_MIN_DAYS_SINCE_FIRST_USE &&
            daysSince(state.lastStoreReviewAt) >= STORE_REVIEW_COOLDOWN_DAYS &&
            daysSince(state.lastShownAt) >= ANY_SURVEY_COOLDOWN_DAYS &&
            daysSince(state.lastLowScoreAt) >= STORE_REVIEW_LOW_SCORE_DAYS
    }

    private fun canAsk(state: FeedbackState, ctx: PromptContext): Boolean {
        val allowed = config.enabled && ctx.analyticsEnabled && !ctx.shownThisSession
        val oldEnough = ctx.accountAgeDays == null || ctx.accountAgeDays >= MIN_ACCOUNT_AGE_DAYS
        return allowed && oldEnough &&
            daysSince(state.lastShownAt) >= ANY_SURVEY_COOLDOWN_DAYS &&
            daysSince(state.lastDismissedAt) >= dismissalBackoffDays(state.consecutiveDismissals)
    }

    private fun csatFeature(moment: FeedbackMoment, state: FeedbackState): String? = when (moment) {
        FeedbackMoment.PURCHASE_FINISHED -> FEATURE_PURCHASE.takeIf { purchaseMilestone(state.purchasesFinished) }
        FeedbackMoment.AI_CHAT_CLOSED -> FEATURE_AI_CHAT
        FeedbackMoment.PHOTO_SCAN_CONFIRMED -> FEATURE_PHOTO_SCAN
        FeedbackMoment.APP_RELATIONSHIP -> null
    }

    /** 1ª e 3ª compra finalizada; depois dela só o cooldown por recurso limita. */
    private fun purchaseMilestone(count: Int) = count == FIRST_PURCHASE || count >= THIRD_PURCHASE

    private fun csatOffCooldown(feature: String, state: FeedbackState) =
        daysSince(state.lastCsatAtByFeature[feature] ?: 0L) >= config.csatCooldownDays

    private fun npsEligible(state: FeedbackState) =
        state.sessions >= NPS_MIN_SESSIONS &&
            state.firstSeenAt > 0L &&
            daysSince(state.firstSeenAt) >= NPS_MIN_DAYS_SINCE_FIRST_USE &&
            daysSince(state.lastNpsAt) >= config.npsCooldownDays

    private fun dismissalBackoffDays(count: Int): Long = when (count) {
        0 -> 0L
        1 -> FIRST_DISMISSAL_BACKOFF_DAYS
        2 -> SECOND_DISMISSAL_BACKOFF_DAYS
        else -> NEXT_DISMISSAL_BACKOFF_DAYS
    }

    /** Dias desde [stamp]; um carimbo `0` (nunca aconteceu) conta como "há muito tempo". */
    private fun daysSince(stamp: Long): Long =
        if (stamp <= 0L) Long.MAX_VALUE else TimeUnit.MILLISECONDS.toDays(clock() - stamp)
}
