package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsBuckets
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.MonthlyGoalRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateShoppingPreferencesUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.ShoppingSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.ShoppingSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ShoppingSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val updateShoppingPreferencesUseCase: UpdateShoppingPreferencesUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val monthlyGoalRepository: MonthlyGoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingSettingsUiState())
    val uiState: StateFlow<ShoppingSettingsUiState> = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    val intent = ShoppingSettingsIntent(
        onUpdateShoppingPreferences = { defaultListId, sortingMode, remindersEnabled ->
            updateShoppingPreferences(defaultListId, sortingMode, remindersEnabled)
        },
        onUpdateMonthlyGoal = { goal -> updateMonthlyGoal(goal) },
        onBack = { _navigator?.goBack() }
    )

    init {
        observeSettings()
        observeMonthlyGoal()
    }

    /** A meta vem da conta (Firestore), não do DataStore das demais preferências. */
    private fun observeMonthlyGoal() {
        monthlyGoalRepository.observeGoal()
            .onEach { goal -> _uiState.update { it.copy(monthlyGoal = goal) } }
            .launchIn(viewModelScope)
    }

    /** Só grava e reporta quando o valor mudou; o evento leva a faixa da meta, nunca o valor. */
    private fun updateMonthlyGoal(goal: Double?) {
        val normalized = goal?.takeIf { it > 0.0 }
        if (normalized == _uiState.value.monthlyGoal) return
        viewModelScope.launch {
            monthlyGoalRepository.setGoal(normalized)
            analyticsTracker.trackEvent(
                AnalyticsEvents.MONTHLY_GOAL_SET,
                buildMap {
                    put(AnalyticsParams.HAS_GOAL, normalized != null)
                    normalized?.let { put(AnalyticsParams.GOAL_BUCKET, AnalyticsBuckets.goal(it)) }
                }
            )
        }
    }

    private fun observeSettings() {
        settingsRepository.getSettings()
            .onEach { settings ->
                _uiState.update {
                    it.copy(
                        defaultListId = settings.defaultListId,
                        sortingMode = settings.sortingMode,
                        remindersEnabled = settings.remindersEnabled
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun updateShoppingPreferences(defaultListId: String?, sortingMode: String, remindersEnabled: Boolean) {
        viewModelScope.launch {
            updateShoppingPreferencesUseCase(defaultListId, sortingMode, remindersEnabled)
            analyticsTracker.trackEvent(
                AnalyticsEvents.SETTINGS_SHOPPING_CHANGED,
                mapOf(AnalyticsParams.ENABLED to remindersEnabled, AnalyticsParams.VALUE to sortingMode.lowercase())
            )
        }
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }
}
