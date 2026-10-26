package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.extensions.rememberCurrencyFormatter
import br.com.brunocarvalhs.howmuch.feature.cart.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Direct, non-CartScreen tests of CartBottomBar in isolation.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CartBottomBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows checkout and add buttons when the cart has products`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        composeTestRule.setContent {
            CestouTheme {
                CartBottomBar(
                    currencyFormatter = rememberCurrencyFormatter(),
                    totalAmount = 42.0,
                    hasProducts = true,
                    onFinished = {},
                    onAdd = {}
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.cart_checkout)).assertExists()
        composeTestRule.onNodeWithTag("cart_bottom_bar_add_button").assertExists()
    }

    @Test
    fun `shows only the labeled add button when the cart is empty`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        composeTestRule.setContent {
            CestouTheme {
                CartBottomBar(
                    currencyFormatter = rememberCurrencyFormatter(),
                    totalAmount = 0.0,
                    hasProducts = false,
                    onFinished = {},
                    onAdd = {}
                )
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.cart_checkout)).assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.cart_add_product_button)).assertExists()
    }

    @Test
    fun `clicking the add button invokes onAdd regardless of hasProducts`() {
        var addClicked = false

        composeTestRule.setContent {
            CestouTheme {
                CartBottomBar(
                    currencyFormatter = rememberCurrencyFormatter(),
                    totalAmount = 0.0,
                    hasProducts = false,
                    onFinished = {},
                    onAdd = { addClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("cart_bottom_bar_add_button").performClick()

        assertTrue(addClicked)
    }

    @Test
    fun `clicking the checkout button invokes onFinished`() {
        var finishedClicked = false

        composeTestRule.setContent {
            CestouTheme {
                CartBottomBar(
                    currencyFormatter = rememberCurrencyFormatter(),
                    totalAmount = 42.0,
                    hasProducts = true,
                    onFinished = { finishedClicked = true },
                    onAdd = {}
                )
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeTestRule.onNodeWithText(context.getString(R.string.cart_checkout)).performClick()

        assertEquals(true, finishedClicked)
    }
}
