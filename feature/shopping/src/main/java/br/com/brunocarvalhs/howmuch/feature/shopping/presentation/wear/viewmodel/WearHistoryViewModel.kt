package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.GroupHistoryByMonthUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearHistoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Histórico do relógio (sdd-056): reusa o agrupamento por mês do celular, mais recente primeiro. */
@HiltViewModel
internal class WearHistoryViewModel @Inject constructor(
    shoppingRepository: ShoppingRepository,
    private val groupHistoryByMonth: GroupHistoryByMonthUseCase
) : ViewModel() {

    val uiState: StateFlow<WearHistoryUiState> = shoppingRepository.observeAll()
        .catch { emit(emptyList<Shopping>()) }
        .map { all ->
            WearHistoryUiState(
                months = StableList(groupHistoryByMonth(all).asReversed()),
                isLoading = false
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WearHistoryUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
