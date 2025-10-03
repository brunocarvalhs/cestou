package br.com.brunocarvalhs.howmuch.app.modules.products.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import br.com.brunocarvalhs.howmuch.ComposeTest
import org.junit.Test

class ProductNameInputTest : ComposeTest() {
    @Test
    fun productNameInput_rendersWithoutCrash() {
        composeTestRule.setContent {
            ProductNameInput(
                name = "",
                onNameChange = {},
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
