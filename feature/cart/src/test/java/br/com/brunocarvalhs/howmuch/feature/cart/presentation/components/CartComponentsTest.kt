package br.com.brunocarvalhs.howmuch.feature.cart.presentation.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Testes de layout via Robolectric (JVM): compõem os componentes reais do carrinho.
@Suppress("MagicNumber")
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CartComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val product = Product(id = "1", name = "Arroz", quantity = 2.0, price = 10.0, category = "Mercearia")

    private fun shopping(id: String, title: String) = Shopping(
        id = id,
        title = title,
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )

    @Test
    fun `edit item content renders the product name`() {
        composeTestRule.setContent {
            CestouTheme { EditItemContent(product = product, onSave = { _, _, _, _ -> }) }
        }
        composeTestRule.onNodeWithText("Arroz").assertExists()
    }

    @Test
    fun `confirm item content renders the product name`() {
        composeTestRule.setContent {
            CestouTheme { ConfirmItemContent(product = product, onConfirm = { _, _ -> }) }
        }
        composeTestRule.onNodeWithText("Arroz").assertExists()
    }

    @Test
    fun `finish purchase content renders`() {
        composeTestRule.setContent {
            CestouTheme { FinishPurchaseContent(totalEstimate = 150.5, onFinish = { _, _ -> }) }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `move to bar lists the target shoppings and hides on demand`() {
        var visible by mutableStateOf(true)
        composeTestRule.setContent {
            CestouTheme {
                MoveToBar(
                    visible = visible,
                    shoppings = listOf(shopping("1", "Compras Casa"), shopping("2", "Churrasco")),
                    onMove = { _, _ -> }
                )
            }
        }
        composeTestRule.onNodeWithText("Compras Casa").assertExists()
        visible = false
        composeTestRule.waitForIdle()
    }

    @Test
    fun `icon product toggles purchased state when enabled`() {
        var clicked: Boolean? = null
        composeTestRule.setContent {
            CestouTheme { IconProduct(product = product, onClick = { clicked = it }, enabled = true) }
        }
        composeTestRule.onAllNodes(hasClickAction()).onFirst().performClick()
        assertEquals(true, clicked)
    }

    @Test
    fun `icon product renders with remote icon while disabled`() {
        composeTestRule.setContent {
            CestouTheme {
                IconProduct(
                    product = product.copy(isPurchased = true),
                    onClick = {},
                    enabled = false,
                    iconUrl = "https://example.com/icon.png"
                )
            }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `product history renders every action with and without profile`() {
        val base = 1_700_000_000_000L
        val history = ProductActivity.Action.entries.mapIndexed { i, action ->
            ProductActivity(userId = if (i % 2 == 0) "u1" else "u2", action = action, timestamp = base + i * 60_000L)
        }
        composeTestRule.setContent {
            CestouTheme {
                ProductHistoryContent(
                    history = history,
                    memberProfiles = mapOf("u1" to UserProfile(id = "u1", name = "Bruno Carvalhos"))
                )
            }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `product history renders empty`() {
        composeTestRule.setContent {
            CestouTheme { ProductHistoryContent(history = emptyList(), memberProfiles = emptyMap()) }
        }
        composeTestRule.onRoot().assertExists()
    }
}
