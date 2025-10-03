package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import br.com.brunocarvalhs.howmuch.ComposeTest
import org.junit.Test

class ShoppingCartItemTest : ComposeTest() {
    @Test
    fun shoppingCartItem_rendersWithoutCrash() {
        composeTestRule.setContent {
            ShoppingCartItem()
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
