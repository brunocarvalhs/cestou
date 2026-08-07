package br.com.brunocarvalhs.howmuch.feature.products.presentation.intent

import br.com.brunocarvalhs.howmuch.core.domain.entity.Product

internal data class ProductSuggestionIntent(
    val onAddProduct: (Product) -> Unit = {},
    val onSearchProduct: (String) -> Unit = {}
)
