package br.com.brunocarvalhs.howmuch.feature.products.presentation.state

import br.com.brunocarvalhs.howmuch.core.domain.entity.Product

internal data class ProductUiState(
    val products: List<Product> = emptyList(),
    val errorMessage: String? = null,
)
