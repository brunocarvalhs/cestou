package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import androidx.lifecycle.ViewModel
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearMenuUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Menu do relógio. Decide se a entrada de IA aparece, pela flag `wear_ai_quick_entry`
 * (default desligada). Notificações e Histórico não têm flag: são dados que já existem.
 */
@HiltViewModel
class WearMenuViewModel @Inject constructor(
    featureFlags: FeatureFlagService,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    val uiState: StateFlow<WearMenuUiState> = MutableStateFlow(
        WearMenuUiState(showAi = featureFlags.isEnabled(FeatureFlagKeys.WEAR_AI_QUICK_ENTRY, default = false))
    ).asStateFlow()

    init {
        analytics.trackEvent(AnalyticsEvents.WEAR_MENU_OPEN)
    }
}
