package br.com.brunocarvalhs.howmuch.analytics

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "analytics_usage"
private const val KEY_LAST_OPEN_DAY = "last_open_day"
private const val THREE_DAYS = 3L
private const val WEEK = 7L
private const val TWO_WEEKS = 14L
private const val MONTH = 30L

/**
 * Sessões e retorno do cliente: `app_foreground`/`app_background` (com a duração da sessão) e
 * `returned_after_days`, a base das análises de retenção.
 */
@Singleton
internal class AppUsageTracker internal constructor(
    context: Context,
    private val analyticsTracker: AnalyticsTracker,
    private val nowMillis: () -> Long
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        analyticsTracker: AnalyticsTracker
    ) : this(context, analyticsTracker, System::currentTimeMillis)

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var foregroundSince: Long? = null

    fun onForeground() {
        if (foregroundSince != null) return
        val now = nowMillis()
        foregroundSince = now
        analyticsTracker.trackEvent(AnalyticsEvents.APP_FOREGROUND)
        trackReturn(today = TimeUnit.MILLISECONDS.toDays(now))
    }

    fun onBackground() {
        val since = foregroundSince ?: return
        foregroundSince = null
        analyticsTracker.trackEvent(
            AnalyticsEvents.APP_BACKGROUND,
            mapOf(AnalyticsParams.SESSION_SECONDS to TimeUnit.MILLISECONDS.toSeconds(nowMillis() - since))
        )
    }

    private fun trackReturn(today: Long) {
        val last = prefs.getLong(KEY_LAST_OPEN_DAY, -1L)
        prefs.edit().putLong(KEY_LAST_OPEN_DAY, today).apply()
        if (last < 0 || today <= last) return
        analyticsTracker.trackEvent(
            AnalyticsEvents.RETURNED_AFTER_DAYS,
            mapOf(AnalyticsParams.DAYS to daysBucket(today - last))
        )
    }

    private fun daysBucket(days: Long): String = when {
        days <= 1 -> "1"
        days <= THREE_DAYS -> "2-3"
        days <= WEEK -> "4-7"
        days <= TWO_WEEKS -> "8-14"
        days <= MONTH -> "15-30"
        else -> "30+"
    }
}
