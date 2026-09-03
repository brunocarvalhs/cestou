package br.com.brunocarvalhs.howmuch.feature.products.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.Product

@Stable
internal data class ProductUiState(
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null,
)
