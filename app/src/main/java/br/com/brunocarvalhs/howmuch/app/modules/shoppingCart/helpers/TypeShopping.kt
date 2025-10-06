package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.helpers

import androidx.annotation.StringRes
import br.com.brunocarvalhs.howmuch.R

enum class TypeShopping(
    @param:StringRes val label: Int
) {
    CART(R.string.cart),
    LIST(R.string.list)
}