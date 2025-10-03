package br.com.brunocarvalhs.howmuch.app.modules.products

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onRoot
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.ComposeTest
import org.junit.Test

class ProductScreenTest : ComposeTest() {
    @Test
    fun productScreen_rendersWithoutCrash() {
        composeTestRule.setContent {
            ProductFormScreen(
                shoppingCartId = "123",
                navController = rememberNavController(),
                viewModel = hiltViewModel()
            )
        }
        composeTestRule.onRoot().assertIsDisplayed()
    }
}
