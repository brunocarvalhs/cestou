package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList

@Stable
internal data class ShoppingDetailUiState(
    val title: String = "",
    val emoji: String = "",
    /** Sum of every item's price × quantity; items without a price count as zero. */
    val total: Double = 0.0,
    val budget: Double? = null,
    val pending: Int = 0,
    /** Pending items first, then purchased ones. */
    val items: StableList<Product> = StableList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val remaining: Double? get() = budget?.let { it - total }
}
