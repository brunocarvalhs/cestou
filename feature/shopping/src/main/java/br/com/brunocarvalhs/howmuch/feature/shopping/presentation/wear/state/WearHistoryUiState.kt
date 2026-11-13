package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth

/** Histórico (sdd-056): meses com compras finalizadas, mais recente primeiro. */
@Stable
internal data class WearHistoryUiState(
    val months: StableList<HistoryMonth> = StableList(),
    val isLoading: Boolean = true
)
