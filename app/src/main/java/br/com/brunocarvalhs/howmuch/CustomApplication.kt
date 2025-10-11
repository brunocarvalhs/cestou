package br.com.brunocarvalhs.howmuch

import android.app.Application
import android.os.StrictMode
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.openApp
import br.com.brunocarvalhs.howmuch.app.foundation.performance.PerformanceMonitor
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CustomApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
        registerActivityLifecycleCallbacks(PerformanceMonitor)

        this.openApp()
    }
}
