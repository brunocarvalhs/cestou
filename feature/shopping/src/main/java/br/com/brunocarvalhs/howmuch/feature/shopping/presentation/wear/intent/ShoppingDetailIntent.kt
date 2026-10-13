package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent

import br.com.brunocarvalhs.howmuch.core.domain.model.Product

internal data class ShoppingDetailIntent(
    val onBack: () -> Unit = {},
    val onToggle: (Product) -> Unit = {},
    /** Text from the watch's voice/keyboard input; blank text is ignored. */
    val onAddItem: (String) -> Unit = {}
)
