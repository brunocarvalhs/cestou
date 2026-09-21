package br.com.brunocarvalhs.howmuch.wear

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class CestouWearApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (br.com.brunocarvalhs.howmuch.wear.BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
