package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state

import br.com.brunocarvalhs.howmuch.core.domain.model.Product

internal data class ProductUiState(
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null,
)
