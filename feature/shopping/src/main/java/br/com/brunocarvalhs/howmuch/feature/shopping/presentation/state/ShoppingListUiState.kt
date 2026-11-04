package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentContext
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetProgress
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText

@Stable
internal data class ShoppingListUiState(
    val list: StableList<Shopping> = StableList(),
    val filteredList: StableList<Shopping> = StableList(),
    val groupedList: Map<String, StableList<Shopping>> = emptyMap(),
    val searchQuery: String = "",
    val selectedFilter: ShoppingFilter = ShoppingFilter.ALL,
    val filters: StableList<ShoppingFilter> = StableList(ShoppingFilter.entries),
    val sortingMode: String = "CATEGORY",
    val isLoading: Boolean = false,
    val isCreateSheetVisible: Boolean = false,
    val unreadNotifications: Int = 0,
    /** Progresso da meta mensal; `null` sem meta, sem sessão ou com os avisos desligados pela flag. */
    val monthlyGoalProgress: BudgetProgress? = null,
    val error: UiText? = null
) : AgentContext {
    override fun toMetadata(): Map<String, Any?> =
        mapOf(
            "shopping_lists" to list.map { it.id },
        )
}
