package br.com.brunocarvalhs.howmuch

import android.app.Application
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CestouApplication : Application() {

    @Inject
    lateinit var agentRegistry: AgentRegistry

    override fun onCreate() {
        super.onCreate()
        Timber.d("🚀 ${agentRegistry.getAll().size} Agentes de IA registrados na inicialização.")
    }
}
