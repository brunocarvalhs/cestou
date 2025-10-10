package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import br.com.brunocarvalhs.howmuch.ComposeTest
import org.junit.Test

class PriceInputTest : ComposeTest() {
    @Test
    fun priceInput_rendersWithoutCrash() {
        composeTestRule.setContent {
            PriceInput(
                price = 0L,
                onPriceChange = {}
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
