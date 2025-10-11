package br.com.brunocarvalhs.howmuch.app.foundation.sdks

import android.content.Context
import androidx.startup.Initializer
import br.com.brunocarvalhs.howmuch.BuildConfig
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.getId
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.metrics.AddTrace

class CrashlyticsInitializer : Initializer<Unit> {

    @AddTrace(name = "SDK-Firebase-Crashlytics")
    override fun create(context: Context) {
        FirebaseCrashlytics.getInstance().apply {
            isCrashlyticsCollectionEnabled = BuildConfig.DEBUG.not()
            setUserId(context.getId())
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return listOf(FirebaseInitializer::class.java)
    }
}
