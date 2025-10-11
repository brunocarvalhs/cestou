package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import br.com.brunocarvalhs.howmuch.ComposeTest
import org.junit.Test

class ShoppingCartInfoTest : ComposeTest() {
    @Test
    fun shoppingCartInfo_rendersWithoutCrash() {
        composeTestRule.setContent {
            ShoppingCartInfo(
                totalPrice = 0L
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
