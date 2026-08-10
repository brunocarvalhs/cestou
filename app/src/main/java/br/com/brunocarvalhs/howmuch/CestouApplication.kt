package br.com.brunocarvalhs.howmuch

import android.app.Application
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class CestouApplication : Application() {

    @Inject
    lateinit var agentUseCases: Set<@JvmSuppressWildcards AgentActionUseCase<*>>

    override fun onCreate() {
        super.onCreate()
        Timber.d("🚀 ${agentUseCases.size} Agentes de IA registrados na inicialização.")
    }
}
