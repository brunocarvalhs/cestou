package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearSearchIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearSearchUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.wearActiveLists
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.wearArchivedLists
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Pesquisar (sdd-056, Q13): lista completa. O filtro roda em memória sobre a lista já observada,
 * sem nova consulta por tecla.
 */
@HiltViewModel
internal class WearSearchViewModel @Inject constructor(
    shoppingRepository: ShoppingRepository
) : ViewModel() {

    private val query = MutableStateFlow("")

    /** Ativas e arquivadas já ordenadas: recalculadas só quando a lista muda, não a cada tecla. */
    private val ordered: Flow<Pair<List<Shopping>, List<Shopping>>> = shoppingRepository.observeAll()
        .catch { emit(emptyList()) }
        .map { all -> all.wearActiveLists() to all.wearArchivedLists() }

    val uiState: StateFlow<WearSearchUiState> = combine(ordered, query) { (active, archived), text ->
        val needle = text.trim()
        WearSearchUiState(
            query = text,
            active = StableList(active.matching(needle)),
            archived = StableList(archived.matching(needle)),
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), WearSearchUiState())

    fun onIntent(intent: WearSearchIntent) {
        when (intent) {
            is WearSearchIntent.QueryChanged -> query.value = intent.text
        }
    }

    private fun List<Shopping>.matching(needle: String): List<Shopping> =
        if (needle.isEmpty()) this else filter { it.title.contains(needle, ignoreCase = true) }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
