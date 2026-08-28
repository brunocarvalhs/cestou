package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.wear.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList

@Stable
internal data class ShoppingDetailUiState(
    val title: String = "",
    val budget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val balance: Double = 0.0,
    val items: StableList<Product> = StableList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
