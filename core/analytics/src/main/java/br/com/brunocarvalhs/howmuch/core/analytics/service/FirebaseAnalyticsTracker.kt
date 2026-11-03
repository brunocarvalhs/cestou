package br.com.brunocarvalhs.howmuch.core.analytics.service

import android.os.Bundle
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.exception.AnalyticsException
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.util.AnalyticsSanitizer
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_USER_PROPERTY_VALUE = 36

@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    private val analytics: FirebaseAnalytics,
    private val crashReporter: CrashReporter,
    private val featureFlagService: FeatureFlagService
) : AnalyticsTracker {

    @Volatile
    override var isCollectionEnabled: Boolean = true
        private set

    override fun trackEvent(name: String, params: Map<String, Any?>) {
        val safeName = AnalyticsSanitizer.eventName(name) ?: run {
            crashReporter.recordException(AnalyticsException("Nome de evento inválido para o Firebase: '$name'"))
            return
        }
        if (safeName == AnalyticsEvents.UI_ACTION && !uiActionsEnabled()) return
        try {
            analytics.logEvent(safeName, AnalyticsSanitizer.params(params).toBundle())
        } catch (e: Exception) {
            crashReporter.recordException(AnalyticsException("Falha ao registrar evento '$name'", e))
        }
    }

    override fun trackScreenView(screenName: String, screenClass: String?) {
        trackEvent(
            name = FirebaseAnalytics.Event.SCREEN_VIEW,
            params = buildMap {
                put(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                screenClass?.let { put(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
            }
        )
    }

    /** Kill-switch remoto: desliga só o `ui_action` (volumoso) sem tirar os demais eventos. */
    private fun uiActionsEnabled(): Boolean =
        featureFlagService.isEnabled(FeatureFlagKeys.ANALYTICS_UI_ACTIONS_ENABLED, default = true)

    override fun setCollectionEnabled(enabled: Boolean) {
        isCollectionEnabled = enabled
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    override fun setUserId(id: String?) {
        analytics.setUserId(id)
    }

    override fun setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value?.take(MAX_USER_PROPERTY_VALUE))
    }

    private fun Map<String, Any?>.toBundle(): Bundle = Bundle().apply {
        this@toBundle.forEach { (key, value) ->
            when (value) {
                null -> Unit
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Double -> putDouble(key, value)
                is Float -> putFloat(key, value)
                is Boolean -> putString(key, value.toString())
                else -> putString(key, value.toString())
            }
        }
    }
}
