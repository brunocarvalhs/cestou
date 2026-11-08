package br.com.brunocarvalhs.howmuch.feature.cart.presentation.helper

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList

/**
 * Totais da barra inferior: [planned] soma a lista toda, [inCart] só o que já foi marcado como
 * pego. [unpricedCount] conta os itens sem preço, que entram como R$ 0,00 nas somas.
 */
internal data class CartTotals(
    val planned: Double = 0.0,
    val inCart: Double = 0.0,
    val unpricedCount: Int = 0
)

internal fun computeCartTotals(products: StableList<Product>): CartTotals {
    var planned = 0.0
    var inCart = 0.0
    var unpriced = 0
    products.forEach { product ->
        planned += product.total
        if (product.isPurchased) inCart += product.total
        if ((product.price ?: 0.0) <= 0.0) unpriced++
    }
    return CartTotals(planned, inCart, unpriced)
}
