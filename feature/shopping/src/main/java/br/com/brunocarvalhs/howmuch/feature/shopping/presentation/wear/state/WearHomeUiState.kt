package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList

/** Home do relógio: até 5 listas recentes, ou todas quando [showAll] (botão "Mais", sdd-056). */
@Stable
internal data class WearHomeUiState(
    val lists: StableList<Shopping> = StableList(),
    val hasMore: Boolean = false,
    val showAll: Boolean = false,
    val isLoading: Boolean = true,
    val phoneUnavailable: Boolean = false
)
