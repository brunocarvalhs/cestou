package br.com.brunocarvalhs.howmuch.core.analytics.service

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFS_NAME = "analytics_first_time"

/**
 * Dispara um evento só na primeira vez que o aparelho o vê (ativação e descoberta de recursos).
 * A marca fica em SharedPreferences por ser uma flag síncrona e minúscula: DataStore exigiria
 * suspender a ViewModel só para decidir se manda um evento.
 */
@Singleton
class FirstTimeEventTracker @Inject constructor(
    @ApplicationContext context: Context,
    private val analyticsTracker: AnalyticsTracker
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Ativação: [event] é um `FIRST_*` de [AnalyticsEvents]. Com a coleta desligada nada é marcado,
     * para quem religar depois ainda gerar o "primeiro uso".
     */
    fun trackOnce(event: String, extra: Map<String, Any?> = emptyMap()) {
        if (!analyticsTracker.isCollectionEnabled || !markSeen(event)) return
        analyticsTracker.trackEvent(event, extra)
    }

    /** Descoberta: manda `feature_first_used` com o [feature] (ex.: `ai_chat`, `photo_scan`). */
    fun trackFeatureFirstUsed(feature: String) {
        if (!analyticsTracker.isCollectionEnabled || !markSeen("feature_$feature")) return
        analyticsTracker.trackEvent(AnalyticsEvents.FEATURE_FIRST_USED, mapOf(AnalyticsParams.FEATURE to feature))
    }

    /** `true` quando [key] ainda não tinha sido marcada. */
    private fun markSeen(key: String): Boolean {
        if (prefs.getBoolean(key, false)) return false
        prefs.edit().putBoolean(key, true).apply()
        return true
    }
}
