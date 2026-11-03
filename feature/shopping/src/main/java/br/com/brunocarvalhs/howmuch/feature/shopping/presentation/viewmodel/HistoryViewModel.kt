package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.CartFlow
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.SpendingInsights
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ObserveHistoryUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.HistoryIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.HistoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
internal class HistoryViewModel @Inject constructor(
    private val observeHistory: ObserveHistoryUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private var navigator: Navigator? = null
    private var observeJob: Job? = null
    private var viewedTracked = false

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    val intent = HistoryIntent(
        onMonthSelected = ::selectMonth,
        onPurchaseClick = { purchase ->
            analyticsTracker.trackEvent(
                AnalyticsEvents.HISTORY_PURCHASE_OPENED,
                mapOf(AnalyticsParams.SHOPPING_ID to purchase.shopping.id)
            )
            navigator?.navigate(CartFlow(purchase.shopping))
        },
        onSeeAnalysis = { navigator?.navigate(SpendingInsights) },
        onRetry = ::observe
    )

    init {
        analyticsTracker.trackScreenView(screenName = "history", screenClass = "HistoryViewModel")
        observe()
    }

    fun setNavigator(navigator: Navigator) {
        this.navigator = navigator
    }

    private fun observe() {
        observeJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        observeJob = viewModelScope.launch {
            observeHistory()
                .catch { error ->
                    Timber.e(error, "Failed to observe purchase history")
                    _uiState.update {
                        it.copy(isLoading = false, error = UiText.StringResource(R.string.history_error))
                    }
                }
                .collect(::onMonths)
        }
    }

    private fun onMonths(months: List<HistoryMonth>) {
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                error = null,
                months = StableList(months),
                selectedMonth = state.selectedMonth?.takeIf { selected -> months.any { it.key == selected } }
                    ?: months.lastOrNull()?.key
            )
        }
        if (!viewedTracked) {
            viewedTracked = true
            analyticsTracker.trackEvent(
                AnalyticsEvents.HISTORY_VIEWED,
                mapOf(AnalyticsParams.ITEMS_COUNT to months.sumOf { it.purchases.size })
            )
        }
    }

    private fun selectMonth(month: YearMonth) {
        if (_uiState.value.selectedMonth == month) return
        _uiState.update { it.copy(selectedMonth = month) }
        analyticsTracker.trackEvent(
            AnalyticsEvents.HISTORY_MONTH_SELECTED,
            mapOf(AnalyticsParams.MONTH to month.toString())
        )
    }
}
