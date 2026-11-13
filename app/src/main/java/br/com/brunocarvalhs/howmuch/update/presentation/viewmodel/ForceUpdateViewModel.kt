package br.com.brunocarvalhs.howmuch.update.presentation.viewmodel

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.BuildConfig
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.RemoteConfigDefaults
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.RemoteVariableKeys
import br.com.brunocarvalhs.howmuch.update.ForceUpdateController
import br.com.brunocarvalhs.howmuch.update.ForceUpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado do force update para a Activity principal (sdd-059). [check] confere a versão mínima
 * remota contra a instalada e [state] diz à tela se segue, se abre a atualização do Play ou se
 * bloqueia.
 */
@HiltViewModel
internal class ForceUpdateViewModel @Inject constructor(
    private val controller: ForceUpdateController,
    private val remoteVariables: RemoteVariableService,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _state = MutableStateFlow<ForceUpdateState>(ForceUpdateState.Idle)

    /** `Required` pede a atualização do Play; `Blocked` mostra a tela de bloqueio. */
    val state: StateFlow<ForceUpdateState> = _state.asStateFlow()

    /**
     * Confere o mínimo remoto. A Activity chama a cada onStart, então cada abertura do app faz uma
     * checagem, e não uma por recomposição.
     */
    fun check() {
        viewModelScope.launch {
            val minVersionCode = remoteVariables.getLong(
                RemoteVariableKeys.APP_MIN_VERSION_CODE,
                RemoteConfigDefaults.APP_MIN_VERSION_CODE_DEFAULT
            )
            val installedVersionCode = BuildConfig.VERSION_CODE.toLong()
            val next = controller.check(minVersionCode, installedVersionCode)
            track(previous = _state.value, next = next, minVersionCode, installedVersionCode)
            _state.value = next
        }
    }

    /** Abre a atualização IMMEDIATE do Play. Sem atualização conhecida, não faz nada. */
    fun start(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        controller.startImmediateUpdate(launcher)
    }

    private fun track(
        previous: ForceUpdateState,
        next: ForceUpdateState,
        minVersionCode: Long,
        installedVersionCode: Long
    ) {
        if (previous == next) return
        val event = when (next) {
            is ForceUpdateState.Required -> AnalyticsEvents.FORCE_UPDATE_REQUIRED
            ForceUpdateState.Blocked -> AnalyticsEvents.FORCE_UPDATE_BLOCKED
            ForceUpdateState.Idle -> return
        }
        analyticsTracker.trackEvent(
            event,
            mapOf(
                AnalyticsParams.MIN_VERSION_CODE to minVersionCode,
                AnalyticsParams.INSTALLED_VERSION_CODE to installedVersionCode
            )
        )
    }
}
