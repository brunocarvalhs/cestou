package br.com.brunocarvalhs.howmuch.feature.cart.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentContext
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetProgress
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.helper.CartTotals
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.helper.computeCartTotals

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
    val error: UiText? = null,
    // Resolved once per list load (member ids + every Product.history entry's userId), not
    // per-row — see CartViewModel.resolveMemberProfiles. Rows only ever do a map lookup here.
    val memberProfiles: Map<String, UserProfile> = emptyMap(),
    /** Orçamento da lista contra o total dos itens; `null` sem orçamento válido ou com a flag desligada. */
    val listBudgetProgress: BudgetProgress? = null,
    /** Meta do mês projetada com esta lista (finalizadas do mês + total da lista); `null` sem meta. */
    val monthProgress: BudgetProgress? = null,
    val budgetAlert: BudgetAlert? = null,
    /** `false` com a flag `feat_realtime_cart` desligada: a barra volta a mostrar só o total geral. */
    val realtimeCartEnabled: Boolean = true,
    /** `false` com a flag `feat_price_comparator` desligada: o item do menu que abre o comparador some. */
    val priceComparatorEnabled: Boolean = true
) : AgentContext {
    /**
     * Total planejado x total no carrinho da barra inferior (sdd-042). Derivado dos [products], então
     * nunca fica fora de sincronia com a lista; calculado uma vez por instância, só quando lido.
     */
    val totals: CartTotals by lazy(LazyThreadSafetyMode.NONE) { computeCartTotals(products) }

    override fun toMetadata(): Map<String, Any?> =
        mapOf(
            "shopping_id" to (shopping?.id ?: ""),
            "products" to products.map { it.name }
        )
}
