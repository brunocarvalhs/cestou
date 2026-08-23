package br.com.brunocarvalhs.howmuch.core.analytics.service

import android.os.Bundle
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.exception.AnalyticsException
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    private val analytics: FirebaseAnalytics,
    private val crashReporter: CrashReporter
) : AnalyticsTracker {

    override fun trackEvent(name: String, params: Map<String, Any?>) {
        try {
            analytics.logEvent(name, params.toBundle())
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

    override fun setUserId(id: String?) {
        analytics.setUserId(id)
    }

    override fun setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
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
