package br.com.brunocarvalhs.howmuch.feature.feedback.domain.model

/**
 * Tudo que a política precisa lembrar entre aberturas do app. Carimbos em epoch millis, `0` quando
 * nunca aconteceu. Nenhum campo carrega dado pessoal.
 */
internal data class FeedbackState(
    val firstSeenAt: Long = 0L,
    val sessions: Int = 0,
    val purchasesFinished: Int = 0,
    val lastShownAt: Long = 0L,
    val lastNpsAt: Long = 0L,
    val lastCsatAtByFeature: Map<String, Long> = emptyMap(),
    val consecutiveDismissals: Int = 0,
    val lastDismissedAt: Long = 0L,
    val lastLowScoreAt: Long = 0L,
    val lastStoreReviewAt: Long = 0L
)

/** Parâmetros ajustáveis por Remote Config, lidos a cada decisão. */
internal data class FeedbackConfig(
    val enabled: Boolean = true,
    val storeReviewEnabled: Boolean = true,
    val npsCooldownDays: Long = DEFAULT_NPS_COOLDOWN_DAYS,
    val csatCooldownDays: Long = DEFAULT_CSAT_COOLDOWN_DAYS
) {
    companion object {
        const val DEFAULT_NPS_COOLDOWN_DAYS = 90L
        const val DEFAULT_CSAT_COOLDOWN_DAYS = 30L
    }
}

/** O que a política precisa saber do momento e que não é histórico gravado. */
internal data class PromptContext(
    val analyticsEnabled: Boolean,
    val accountAgeDays: Long?,
    val shownThisSession: Boolean
)
