package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.helpers

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.brunocarvalhs.howmuch.R

enum class TypeShopping(
    @param:StringRes val label: Int,
    val icon: ImageVector
) {
    CART(R.string.cart, Icons.Default.ShoppingCart),
    LIST(R.string.list, Icons.AutoMirrored.Filled.List)
}
