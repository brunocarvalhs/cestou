package br.com.brunocarvalhs.howmuch.feature.cart.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.chat.domain.entity.ChatMessage

@Stable
internal data class CartUiState(
    val shopping: Shopping? = null,
    val products: StableList<Product> = StableList(),
    val aiSuggestions: StableList<String> = StableList(),
    val isAiSuggestionsLoading: Boolean = false,
    val sortingMode: String = "CATEGORY",
    val prompt: String = "",
    val aiDockState: AiDockState = AiDockState.COLLAPSED,
    val aiMessages: StableList<ChatMessage> = StableList(),
    val allShoppings: StableList<Shopping> = StableList(),
    val isAiLoading: Boolean = false,
    val isLoading: Boolean = false,
    val error: UiText? = null
) : AiAgentContext {
    override fun toMetadata(): Map<String, Any?> =
        mapOf(
            "shopping_id" to (shopping?.id ?: ""),
            "products" to products.map { it.name }
        )
}
