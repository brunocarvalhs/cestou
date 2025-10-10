package br.com.brunocarvalhs.howmuch.app.foundation.sdks

import android.content.Context
import androidx.startup.Initializer
import br.com.brunocarvalhs.howmuch.BuildConfig
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.getId
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.perf.metrics.AddTrace

class AnalyticsInitializer : Initializer<Unit> {

    @AddTrace(name = "SDK-Firebase-Analytics")
    override fun create(context: Context) {
        FirebaseAnalytics.getInstance(context).apply {
            setAnalyticsCollectionEnabled(BuildConfig.DEBUG.not())
            setUserId(context.getId())
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(FirebaseInitializer::class.java)
    }
}
