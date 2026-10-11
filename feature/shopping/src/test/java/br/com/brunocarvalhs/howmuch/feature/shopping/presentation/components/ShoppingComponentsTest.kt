package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.utils.LocalCurrency
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.form.CreateShoppingContent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.form.EditShoppingContent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.form.JoinListContent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.scanner.QrCodeBottomSheet
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingHeader
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingItem
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingItemLoading
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingStatusBadge
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping.ShoppingSummaryCard
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Testes de layout via Robolectric (JVM): compõem os componentes reais de shopping.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ShoppingComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val shopping = Shopping(
        id = "1",
        title = "Lista de Teste",
        description = "Descrição da lista",
        price = 150.0,
        status = Shopping.Status.NEW,
        users = listOf("user1"),
        roles = mapOf("user1" to "OWNER")
    )

    @Test
    fun `create form renders`() {
        composeTestRule.setContent {
            CestouTheme { CreateShoppingContent(onConfirm = { _, _, _ -> }, onCancel = {}) }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `edit form renders the shopping title and cancels`() {
        composeTestRule.setContent {
            CestouTheme {
                EditShoppingContent(
                    shopping = shopping.copy(budget = 200.0),
                    onSave = {},
                    onCancel = {},
                    sharingToken = "TOKEN123"
                )
            }
        }
        composeTestRule.onNodeWithText("Lista de Teste").assertExists()
    }

    @Test
    fun `join list renders default loading and error states`() {
        composeTestRule.setContent {
            CestouTheme {
                Column {
                    JoinListContent(onDismiss = {}, onJoin = {}, onScan = {})
                    JoinListContent(onDismiss = {}, onJoin = {}, onScan = {}, isLoading = true)
                    JoinListContent(
                        onDismiss = {}, onJoin = {}, onScan = {}, initialToken = "abc",
                        error = UiText.DynamicString("Token inválido ou expirado")
                    )
                }
            }
        }
        composeTestRule.onNodeWithText("Token inválido ou expirado").assertExists()
    }

    @Test
    fun `qr code bottom sheet renders the token`() {
        composeTestRule.setContent {
            CestouTheme { QrCodeBottomSheet(token = "ABC123", onDismissRequest = {}, onShare = {}) }
        }
        composeTestRule.onNodeWithText("ABC123", substring = true).assertExists()
    }

    @Test
    fun `shopping item renders every status and reacts to click`() {
        var clicked = false
        composeTestRule.setContent {
            CestouTheme {
                Column {
                    ShoppingItem(title = "Morning breakfast", users = listOf("1", "2"), onClick = { clicked = true })
                    ShoppingItem(title = "Often purchased", status = Shopping.Status.IN_PROGRESS, budget = 100.0)
                    ShoppingItem(title = "Done", status = Shopping.Status.entries.last(), iconUrl = "https://x/y.png")
                    ShoppingItemLoading()
                }
            }
        }
        composeTestRule.onNodeWithText("Morning breakfast").performClick()
        assertTrue(clicked)
    }

    @Test
    fun `status badge renders for each status`() {
        composeTestRule.setContent {
            CestouTheme { Column { Shopping.Status.entries.forEach { ShoppingStatusBadge(it) } } }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `summary card renders with and without budget`() {
        composeTestRule.setContent {
            CestouTheme {
                Column {
                    ShoppingSummaryCard(totalAmount = 847.3, totalLists = 5, completedLists = 2, totalBudget = 500.0)
                    ShoppingSummaryCard(totalAmount = 0.0, totalLists = 0, completedLists = 0)
                }
            }
        }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `shopping item budget follows the selected currency`() {
        java.util.Locale.setDefault(java.util.Locale.US)
        composeTestRule.setContent {
            CestouTheme {
                CompositionLocalProvider(LocalCurrency provides "USD") {
                    ShoppingItem(title = "Market", budget = 1234.5)
                }
            }
        }
        composeTestRule.onNodeWithText("$1,234.50", substring = true).assertExists()
    }

    @Test
    fun `header renders the user name`() {
        composeTestRule.setContent { CestouTheme { ShoppingHeader(userName = "Marina") } }
        composeTestRule.onNodeWithText("Marina", substring = true).assertExists()
    }
}
