package br.com.brunocarvalhs.howmuch.core.analytics.model

/**
 * Nomes e valores das user properties do GA4 (limite de 25 propriedades, nome ≤ 24 e valor ≤ 36
 * caracteres). Nenhuma carrega dado pessoal: e-mail e nome nunca vão para o Analytics.
 */
object AnalyticsUserProperties {
    const val PLAN = "plan"
    const val EVER_PRO = "ever_pro"
    const val AI_TRIAL_USED = "ai_trial_used"
    const val ACCOUNT_AGE_BUCKET = "account_age_bucket"
    const val IS_TESTER = "is_tester"
    const val LISTS_BUCKET = "lists_bucket"
    const val APP_LANGUAGE = "app_language"
    const val CURRENCY = "currency"
    const val THEME = "theme"
    const val AI_PROVIDER = "ai_provider"
    const val NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val REMINDERS_ENABLED = "reminders_enabled"
    const val SORTING_MODE = "sorting_mode"
    const val NPS_CLASS = "nps_class"
    const val FORM_FACTOR = "form_factor"

    const val FORM_FACTOR_WEAR = "wear"

    const val PLAN_FREE = "free"
    const val PLAN_PRO = "pro"

    const val NPS_PROMOTER = "promoter"
    const val NPS_PASSIVE = "passive"
    const val NPS_DETRACTOR = "detractor"

    private const val NPS_DETRACTOR_MAX = 6
    private const val NPS_PASSIVE_MAX = 8
    private const val NEW_ACCOUNT_MAX_DAYS = 1L
    private const val WEEK_DAYS = 7L
    private const val MONTH_DAYS = 30L
    private const val FEW_LISTS_MAX = 3

    /** `0-1d`, `2-7d`, `8-30d` ou `30d+`. */
    fun accountAgeBucket(ageDays: Long): String = when {
        ageDays <= NEW_ACCOUNT_MAX_DAYS -> "0-1d"
        ageDays <= WEEK_DAYS -> "2-7d"
        ageDays <= MONTH_DAYS -> "8-30d"
        else -> "30d+"
    }

    /** `0`, `1-3` ou `4+`. */
    fun listsBucket(count: Int): String = when {
        count <= 0 -> "0"
        count <= FEW_LISTS_MAX -> "1-3"
        else -> "4+"
    }

    /** `detractor` (0-6), `passive` (7-8) ou `promoter` (9-10). */
    fun npsClass(score: Int): String = when {
        score <= NPS_DETRACTOR_MAX -> NPS_DETRACTOR
        score <= NPS_PASSIVE_MAX -> NPS_PASSIVE
        else -> NPS_PROMOTER
    }
}
