package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent

import br.com.brunocarvalhs.howmuch.feature.products.app.domain.model.CommonProduct

internal data class CommonProductIntent(
    val onNewItemNameChange: (String) -> Unit = {},
    val onAddItem: () -> Unit = {},
    val onRemoveItem: (String) -> Unit = {},
    val onAddToShopping: (CommonProduct) -> Unit = {},
    val onAddAllToShopping: () -> Unit = {},
    val onMessageShown: () -> Unit = {}
)
