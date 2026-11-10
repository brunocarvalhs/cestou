package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.WEAR_HOME_RECENT_LIMIT
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearHomeIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearHomeUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.wearActiveLists
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Home do relógio (sdd-056): 5 listas recentes, "Mais", "Criar lista no celular". */
@HiltViewModel
internal class WearHomeViewModel @Inject constructor(
    shoppingRepository: ShoppingRepository,
    private val wearableSyncService: WearableSyncService
) : ViewModel() {

    private val showAll = MutableStateFlow(false)
    private val phoneUnavailable = MutableStateFlow(false)

    val uiState: StateFlow<WearHomeUiState> = combine(
        shoppingRepository.observeAll().catch { emit(emptyList<Shopping>()) },
        showAll,
        phoneUnavailable
    ) { all, expanded, unavailable ->
        val active = all.wearActiveLists()
        WearHomeUiState(
            lists = StableList(if (expanded) active else active.take(WEAR_HOME_RECENT_LIMIT)),
            hasMore = active.size > WEAR_HOME_RECENT_LIMIT,
            showAll = expanded,
            isLoading = false,
            phoneUnavailable = unavailable
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WearHomeUiState())

    fun onIntent(intent: WearHomeIntent) {
        when (intent) {
            WearHomeIntent.ToggleShowAll -> showAll.update { !it }
            WearHomeIntent.CreateListOnPhone -> openOnPhone(PATH_CREATE_LIST)
        }
    }

    private fun openOnPhone(path: String) {
        viewModelScope.launch {
            phoneUnavailable.value = !wearableSyncService.openPhoneApp(path)
        }
    }

    internal companion object {
        /** Caminho do deeplink `cestou://shopping/new` (sdd-056). */
        const val PATH_CREATE_LIST = "shopping/new"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
