package br.com.brunocarvalhs.howmuch.core.common.initializer

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import br.com.brunocarvalhs.howmuch.core.common.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class TimberInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(
            FirebaseInitializer::class.java,
            CrashlyticsInitializer::class.java
        )
    }

    /**
     * Uma Tree que loga informações importantes para o Crashlytics.
     */
    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return
            }

            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.log(message)

            if (t != null) {
                crashlytics.recordException(t)
            }
        }
    }
}
