package br.com.brunocarvalhs.howmuch.feature.cart.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.cart.R
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.intent.CartIntent
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.CartUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Split out of CartScreenTest to keep TooManyFunctions under the detekt threshold — scoped to
// the activation-funnel fix: the empty-state "add first item" CTA and demoting checkout when
// the cart is empty. CartScreen no longer takes a windowSizeClass at all (the bottom bar used
// to hide on Expanded/tablet sizes via `!isExpanded`; removing the parameter entirely, rather
// than just the gating, makes that regression structurally impossible instead of just tested).
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CartScreenAddProductTest {

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
    fun `shows the add-first-product button in the empty state and triggers onToggleProductPicker`() {
        var clickedSource: String? = null

        composeTestRule.setContent {
            CestouTheme {
                CartScreen(
                    uiState = CartUiState(shopping = shopping),
                    intent = CartIntent(onToggleProductPicker = { source -> clickedSource = source })
                )
            }
        }

        // The empty-state content (icon + title + description + button) can be taller than the
        // Robolectric test window's viewport, so the button needs an explicit scroll into view
        // before it's safe to click — same as any item inside a scrollable LazyColumn.
        composeTestRule.onNodeWithTag("cart_empty_add_button").performScrollTo().performClick()

        assertEquals("empty_state", clickedSource)
    }

    @Test
    fun `hides the checkout button and widens the add button when the cart is empty`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        composeTestRule.setContent {
            CestouTheme {
                CartScreen(uiState = CartUiState(shopping = shopping), intent = CartIntent())
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.cart_checkout)).assertDoesNotExist()
        composeTestRule.onNodeWithTag("cart_bottom_bar_add_button").assertExists()
    }

    @Test
    fun `shows the checkout button once the cart has at least one product`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val product = Product(id = "p1", name = "Arroz", quantity = 1.0)

        composeTestRule.setContent {
            CestouTheme {
                CartScreen(
                    uiState = CartUiState(shopping = shopping, products = StableList(listOf(product))),
                    intent = CartIntent()
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.cart_checkout)).assertExists()
    }
}
