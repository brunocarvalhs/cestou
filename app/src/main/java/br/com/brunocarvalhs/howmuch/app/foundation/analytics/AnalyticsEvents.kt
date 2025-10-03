package br.com.brunocarvalhs.howmuch.app.foundation.analytics

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

object AnalyticsEvents : AnalyticsManager {

    internal val analytics: FirebaseAnalytics by lazy {
        Firebase.analytics
    }

    override fun trackScreen(screenName: String) {
        val bundle = Bundle().apply {
            putString(AnalyticsParam.SCREEN_NAME.paramName, screenName)
            putString(AnalyticsParam.SCREEN_CLASS.paramName, screenName)
        }
        analytics.logEvent(AnalyticsEvent.SCREEN_VIEW.eventName, bundle)
    }

    override fun trackEvent(
        event: AnalyticsEvent,
        params: Map<AnalyticsParam, Any>?
    ) {
        analytics.logEvent(event.eventName, params?.toBundle())
    }

    private fun Map<AnalyticsParam, Any>.toBundle(): Bundle {
        val bundle = Bundle()
        this.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key.paramName, value)
                is Int -> bundle.putInt(key.paramName, value)
                is Long -> bundle.putLong(key.paramName, value)
                is Double -> bundle.putDouble(key.paramName, value)
                is Float -> bundle.putDouble(key.paramName, value.toDouble())
                is Boolean -> bundle.putString(key.paramName, value.toString())
                else -> bundle.putString(key.paramName, value.toString())
            }
        }
        return bundle
    }
}
