package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.analytics.service.FirstTimeEventTracker
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.CartFlow
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.toSpending
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.GetCategorySpendingUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ObserveMonthlySpendingUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.SpendingInsightsIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.mapper.toCategoryShares
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.SpendingInsightsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.abs

private const val TOTAL_TOLERANCE = 0.01

private const val FEATURE_INSIGHTS = "spending_insights"

@HiltViewModel
internal class SpendingInsightsViewModel @Inject constructor(
    private val observeMonthlySpending: ObserveMonthlySpendingUseCase,
    private val getCategorySpending: GetCategorySpendingUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val firstTimeEventTracker: FirstTimeEventTracker
) : ViewModel() {

    private var navigator: Navigator? = null
    private var monthsJob: Job? = null
    private var viewedTracked = false

    private val months = MutableStateFlow<List<HistoryMonth>>(emptyList())
    private val selected = MutableStateFlow<YearMonth?>(null)
    private val categoriesRetry = MutableStateFlow(0)

    private val _uiState = MutableStateFlow(SpendingInsightsUiState())
    val uiState: StateFlow<SpendingInsightsUiState> = _uiState.asStateFlow()

    val intent = SpendingInsightsIntent(
        onMonthSelected = ::selectMonth,
        onPurchaseClick = { purchase ->
            analyticsTracker.trackEvent(
                AnalyticsEvents.SPENDING_INSIGHTS_LIST_OPENED,
                mapOf(AnalyticsParams.SHOPPING_ID to purchase.shopping.id)
            )
            navigator?.navigate(CartFlow(purchase.shopping))
        },
        onBack = { navigator?.goBack() },
        onRetry = ::retry
    )

    init {
        firstTimeEventTracker.trackFeatureFirstUsed(FEATURE_INSIGHTS)
        observeMonths()
        observeCategories()
    }

    fun setNavigator(navigator: Navigator) {
        this.navigator = navigator
    }

    private fun observeMonths() {
        monthsJob?.cancel()
        _uiState.update { it.copy(isLoading = true, error = null) }
        monthsJob = viewModelScope.launch {
            observeMonthlySpending()
                .catch { error ->
                    Timber.e(error, "Failed to observe monthly spending")
                    _uiState.update {
                        it.copy(isLoading = false, error = UiText.StringResource(R.string.insights_error))
                    }
                }
                .collect(::onMonths)
        }
    }

    private fun onMonths(list: List<HistoryMonth>) {
        val key = selected.value?.takeIf { current -> list.any { it.key == current } } ?: list.lastOrNull()?.key
        selected.value = key
        months.value = list
        _uiState.update {
            it.copy(
                isLoading = false,
                error = null,
                months = StableList(list.map(HistoryMonth::toSpending)),
                selectedMonth = key,
                purchases = StableList(list.firstOrNull { month -> month.key == key }?.purchases.orEmpty())
            )
        }
        if (!viewedTracked) {
            viewedTracked = true
            analyticsTracker.trackEvent(
                AnalyticsEvents.SPENDING_INSIGHTS_VIEWED,
                mapOf(AnalyticsParams.ITEMS_COUNT to list.size)
            )
        }
    }

    /**
     * Only the selected month is read, and a new selection cancels the read in flight
     * ([collectLatest]); the month is compared by value (plus the retry counter) so a
     * re-emission with the same purchases does not hit Firestore again.
     */
    private fun observeCategories() {
        viewModelScope.launch {
            combine(months, selected, categoriesRetry) { list, key, attempt ->
                list.firstOrNull { it.key == key } to attempt
            }
                .distinctUntilChanged()
                .collectLatest { (month, _) -> loadCategories(month) }
        }
    }

    private suspend fun loadCategories(month: HistoryMonth?) {
        if (month == null) {
            _uiState.update { it.copy(categories = StableList(), categoriesLoading = false, categoriesError = null) }
            return
        }
        _uiState.update { it.copy(categoriesLoading = true, categoriesError = null) }
        getCategorySpending(month.purchases.map { it.shopping })
            .onSuccess { spending ->
                val productsTotal = spending.sumOf { it.total }
                _uiState.update {
                    it.copy(
                        categories = StableList(spending.toCategoryShares()),
                        categoriesLoading = false,
                        categoriesDiverge = abs(productsTotal - month.total) > TOTAL_TOLERANCE
                    )
                }
            }
            .onFailure { error ->
                Timber.e(error, "Failed to load category spending")
                _uiState.update {
                    it.copy(
                        categories = StableList(),
                        categoriesLoading = false,
                        categoriesError = UiText.StringResource(R.string.insights_categories_error)
                    )
                }
            }
    }

    private fun selectMonth(month: YearMonth) {
        if (selected.value == month) return
        selected.value = month
        _uiState.update { state ->
            state.copy(
                selectedMonth = month,
                purchases = StableList(months.value.firstOrNull { it.key == month }?.purchases.orEmpty())
            )
        }
        analyticsTracker.trackEvent(
            AnalyticsEvents.SPENDING_INSIGHTS_MONTH_SELECTED,
            mapOf(AnalyticsParams.MONTH to month.toString())
        )
    }

    private fun retry() {
        if (_uiState.value.error != null) observeMonths()
        categoriesRetry.update { it + 1 }
    }
}
