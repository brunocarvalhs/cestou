package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.CartFlow
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.SpendHistoryMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.GetSpendHistoryUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.InProgressSpend
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.MonthBudget
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.MonthSpendSummary
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.SpendHistoryPurchase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.SpendHistoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
internal class SpendHistoryViewModel @Inject constructor(
    private val getSpendHistoryUseCase: GetSpendHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpendHistoryUiState(isLoading = true))
    val uiState: StateFlow<SpendHistoryUiState> = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    // Keyed by SpendHistoryPurchase.id (== Shopping.id), so tapping a purchase row can open the
    // full Shopping in the read-only cart without a second repository round trip.
    private var shoppingById: Map<String, Shopping> = emptyMap()

    init {
        load()
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    fun onRetry() {
        load()
    }

    fun onPurchaseClicked(purchase: SpendHistoryPurchase) {
        shoppingById[purchase.id]?.let { shopping ->
            _navigator?.navigate(CartFlow(shopping))
        }
    }

    private fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { getSpendHistoryUseCase(System.currentTimeMillis()) }
                .onSuccess { months -> onMonthsLoaded(months) }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = UiText.StringResource(R.string.spend_history_error_message)
                        )
                    }
                }
        }
    }

    private fun onMonthsLoaded(months: List<SpendHistoryMonth>) {
        shoppingById = months
            .flatMap { it.purchases }
            .associate { it.shopping.id to it.shopping }

        val uiMonths = months.map { month -> month.toUiModel() }

        _uiState.update {
            it.copy(
                isLoading = false,
                error = null,
                months = StableList(uiMonths),
                selectedMonthId = uiMonths.firstOrNull { month -> month.isCurrent }?.id
                    ?: uiMonths.lastOrNull()?.id
            )
        }
    }

    private fun SpendHistoryMonth.toUiModel(): MonthSpendSummary {
        val locale = Locale.getDefault()
        val firstDayOfMonth = yearMonth.atDay(1)
        val monthLabel = DateTimeFormatter.ofPattern("MMM", locale).format(firstDayOfMonth)
            .replaceFirstChar { it.titlecase(locale) }
        val fullLabel = DateTimeFormatter.ofPattern("MMMM", locale).format(firstDayOfMonth)
            .replaceFirstChar { it.titlecase(locale) }

        return MonthSpendSummary(
            id = yearMonth.toString(),
            monthLabel = monthLabel,
            fullLabel = fullLabel,
            total = total,
            isCurrent = isCurrent,
            budget = budget?.let { MonthBudget(amount = it.amount, spent = it.spent) },
            inProgress = inProgress?.let {
                InProgressSpend(total = it.total, listCount = it.listCount)
            },
            purchases = StableList(
                purchases.map { entry ->
                    SpendHistoryPurchase(
                        id = entry.shopping.id,
                        timestamp = entry.purchaseDate,
                        emoji = entry.shopping.emoji,
                        title = entry.shopping.title,
                        establishment = entry.shopping.description.takeIf { it.isNotBlank() },
                        total = entry.total,
                        isEstimated = entry.isEstimated,
                        isShared = entry.shopping.users.size > 1
                    )
                }
            )
        )
    }
}
