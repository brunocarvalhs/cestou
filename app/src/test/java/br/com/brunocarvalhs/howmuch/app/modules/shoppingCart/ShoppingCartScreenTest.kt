package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import androidx.hilt.navigation.compose.hiltViewModel
import br.com.brunocarvalhs.howmuch.ComposeTest
import org.junit.Test

class ShoppingCartScreenTest : ComposeTest() {
    @Test
    fun shoppingCartScreen_rendersWithoutCrash() {
        composeTestRule.setContent {
            ShoppingCartScreen(
                viewModel = hiltViewModel()
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
