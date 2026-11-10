package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList

/** Pesquisar (sdd-056): lista completa, ativas primeiro e depois arquivadas, filtrada pelo título. */
@Stable
internal data class WearSearchUiState(
    val query: String = "",
    val active: StableList<Shopping> = StableList(),
    val archived: StableList<Shopping> = StableList(),
    val isLoading: Boolean = true
)
