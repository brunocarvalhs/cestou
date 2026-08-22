package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent

import br.com.brunocarvalhs.howmuch.core.domain.model.Product

internal data class ProductSuggestionIntent(
    val onAddProduct: (Product) -> Unit = {},
    val onSearchProduct: (String) -> Unit = {}
)
