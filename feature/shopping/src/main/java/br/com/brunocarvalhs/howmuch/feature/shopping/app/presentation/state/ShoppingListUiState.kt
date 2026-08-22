package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state.AiDockState

@Stable
internal data class ShoppingListUiState(
    val list: StableList<Shopping> = StableList(),
    val filteredList: StableList<Shopping> = StableList(),
    val groupedList: Map<String, StableList<Shopping>> = emptyMap(),
    val searchQuery: String = "",
    val selectedFilter: ShoppingFilter = ShoppingFilter.ALL,
    val filters: StableList<ShoppingFilter> = StableList(ShoppingFilter.entries),
    val sortingMode: String = "CATEGORY",
    val prompt: String = "",
    val aiDockState: AiDockState = AiDockState.COLLAPSED,
    val aiMessages: StableList<ChatMessage> = StableList(),
    val aiSuggestions: StableList<String> = StableList(),
    val isAiLoading: Boolean = false,
    val isAiSuggestionsLoading: Boolean = false,
    val isLoading: Boolean = false,
    val isCreateSheetVisible: Boolean = false,
    val error: UiText? = null
) : AiAgentContext {
    override fun toMetadata(): Map<String, Any?> =
        mapOf(
            "shopping_lists" to list.map { it.id },
        )
}
