package br.com.brunocarvalhs.howmuch.app.modules.products.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import br.com.brunocarvalhs.howmuch.ComposeTest
import org.junit.Test

class QuantitySelectorTest : ComposeTest() {
    @Test
    fun quantitySelector_rendersWithoutCrash() {
        composeTestRule.setContent {
            QuantitySelector(
                quantity = 1,
                onQuantityChange = {}
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
