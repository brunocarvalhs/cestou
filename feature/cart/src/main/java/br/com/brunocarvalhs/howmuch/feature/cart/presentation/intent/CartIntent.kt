package br.com.brunocarvalhs.howmuch.feature.cart.presentation.intent

import br.com.brunocarvalhs.howmuch.core.domain.model.Product

internal data class CartIntent(
    val onRefresh: () -> Unit = {},
    val onToggleProductPicker: () -> Unit = {},
    val onShareShopping: () -> Unit = {},
    val onPromptChanged: (String) -> Unit = {},
    val onOpenAi: () -> Unit = {},
    val onCloseAi: () -> Unit = {},
    val onSendPrompt: () -> Unit = {},
    val onToggleAi: () -> Unit = {},
    val onDeleteProduct: (Product) -> Unit = {},
    val onEditProduct: (Product) -> Unit = {},
    val onUpdateQuantity: (Product, Double) -> Unit = { _, _ -> },
    val onTogglePurchased: (Product, Boolean) -> Unit = { _, _ -> },
    val onToggleFinishPurchaseSheet: () -> Unit = {},
    val onClearPurchased: () -> Unit = {},
    val onShowShareOptions: () -> Unit = {},
    val onSuggestionClick: (String) -> Unit = {},
    val onMoveProduct: (Product, String) -> Unit = { _, _ -> }
)
