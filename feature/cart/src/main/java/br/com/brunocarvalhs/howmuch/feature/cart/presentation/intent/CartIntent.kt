package br.com.brunocarvalhs.howmuch.feature.cart.presentation.intent

import br.com.brunocarvalhs.howmuch.core.domain.model.Product

internal data class CartIntent(
    val onRefresh: () -> Unit = {},
    val onToggleProductPicker: () -> Unit = {},
    val onShareShopping: () -> Unit = {},
    val onDeleteProduct: (Product) -> Unit = {}, //
    val onEditProduct: (Product) -> Unit = {}, //
    val onUpdateQuantity: (Product, Double) -> Unit = { _, _ -> },
    val onTogglePurchased: (Product, Boolean) -> Unit = { _, _ -> }, //
    val onToggleFinishPurchaseSheet: () -> Unit = {},
    val onClearPurchased: () -> Unit = {}, //
    val onShowShareOptions: () -> Unit = {},
    val onMoveProduct: (Product, String) -> Unit = { _, _ -> }
)
