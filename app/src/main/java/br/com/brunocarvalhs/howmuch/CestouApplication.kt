package br.com.brunocarvalhs.howmuch

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter
import br.com.brunocarvalhs.howmuch.push.PushChannel
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CestouApplication : Application() {

    @Inject
    lateinit var agentRegistry: AgentRegistry

    @Inject
    lateinit var crashReporter: CrashReporter

    override fun onCreate() {
        super.onCreate()
        PushChannel.ensureCreated(this)
        Timber.d("🚀 ${agentRegistry.getAll().size} Agentes de IA registrados na inicialização.")
        /**
         * WorkManager's default ContentProvider auto-init is disabled (AndroidManifest.xml)
         * because it crashed the whole app at process start on devices whose framework lacks
         * JobScheduler.forNamespace despite reporting SDK_INT >= 34 (Crashlytics fatal seen on a
         * Pixel 8 Pro). Initializing manually here, after Hilt injection, lets that failure be
         * caught instead: the daily shopping reminder (ShoppingReminderScheduler) just stays
         * unavailable on such devices.
         */
        initializeWorkManagerSafely(crashReporter) {
            WorkManager.initialize(this, Configuration.Builder().build())
        }
    }
}
