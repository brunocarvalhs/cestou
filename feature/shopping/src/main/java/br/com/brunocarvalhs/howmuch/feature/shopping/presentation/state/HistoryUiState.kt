package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state

import androidx.compose.runtime.Immutable
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryPurchase
import java.time.YearMonth

@Immutable
internal data class HistoryUiState(
    val isLoading: Boolean = true,
    val months: StableList<HistoryMonth> = StableList(),
    val selectedMonth: YearMonth? = null,
    val error: UiText? = null
) {
    val selectedPurchases: List<HistoryPurchase>
        get() = months.firstOrNull { it.key == selectedMonth }?.purchases.orEmpty()

    val isEmpty: Boolean
        get() = !isLoading && error == null && months.isEmpty()
}
