package br.com.brunocarvalhs.howmuch.feature.cart.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.withActivity
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.intent.CartIntent
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.CartUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreUiR

// Teste de layout via Robolectric (JVM), não androidTest: valida a composição real da tela
// sem depender de emulador, adequado para rodar na esteira de CI.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CartScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )

    @Test
    fun `renders the current shopping list title`() {
        composeTestRule.setContent {
            CestouTheme {
                CartScreen(uiState = CartUiState(shopping = shopping), intent = CartIntent())
            }
        }

        composeTestRule.onNodeWithText("Weekly Groceries").assertExists()
    }

    @Test
    fun `renders without crashing when there is no shopping list yet`() {
        composeTestRule.setContent {
            CestouTheme {
                CartScreen(uiState = CartUiState(), intent = CartIntent())
            }
        }

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `hides the attribution avatar for a legacy product with no history`() {
        val sharedShopping = shopping.copy(users = listOf("user-a", "user-b"))
        val legacyProduct = Product(id = "p1", name = "Arroz", quantity = 1.0)

        composeTestRule.setContent {
            CestouTheme {
                CartScreen(
                    uiState = CartUiState(shopping = sharedShopping, products = StableList(listOf(legacyProduct))),
                    intent = CartIntent()
                )
            }
        }

        val historyDescription = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(CoreUiR.string.content_description_product_history)
        composeTestRule.onNodeWithContentDescription(historyDescription).assertDoesNotExist()
    }

    @Test
    fun `shows the attribution avatar for a product with history on a shared list`() {
        val sharedShopping = shopping.copy(users = listOf("user-a", "user-b"))
        val trackedProduct = Product(id = "p1", name = "Arroz", quantity = 1.0)
            .withActivity(ProductActivity.Action.ADDED, userId = "user-a")

        composeTestRule.setContent {
            CestouTheme {
                CartScreen(
                    uiState = CartUiState(shopping = sharedShopping, products = StableList(listOf(trackedProduct))),
                    intent = CartIntent()
                )
            }
        }

        val historyDescription = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(CoreUiR.string.content_description_product_history)
        composeTestRule.onNodeWithContentDescription(historyDescription).assertExists()
    }
}
