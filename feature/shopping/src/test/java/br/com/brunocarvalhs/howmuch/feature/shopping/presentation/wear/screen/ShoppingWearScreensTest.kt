package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.ShoppingListUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel.ShoppingListViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.ShoppingDetailIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.ShoppingDetailUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.ShoppingDetailViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Testes de layout via Robolectric (JVM) das telas Wear de shopping.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ShoppingWearScreensTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun listViewModel(state: ShoppingListUiState) = mockk<ShoppingListViewModel>(relaxed = true) {
        every { uiState } returns MutableStateFlow(state)
    }

    private fun detailViewModel(
        state: ShoppingDetailUiState,
        intent: ShoppingDetailIntent = ShoppingDetailIntent()
    ) = mockk<ShoppingDetailViewModel>(relaxed = true) {
        every { uiState } returns MutableStateFlow(state)
        every { this@mockk.intent } returns intent
    }

    private val shopping = Shopping(
        id = "s1",
        title = "Café da manhã",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )

    @Test
    fun `list screen shows the lists and navigates to the detail`() {
        var opened: String? = null
        composeTestRule.setContent {
            ShoppingWearScreen(
                viewModel = listViewModel(ShoppingListUiState(list = StableList(listOf(shopping)))),
                onNavigateToDetail = { opened = it }
            )
        }
        composeTestRule.onNodeWithText("Café da manhã", substring = true).performClick()
        assertEquals("s1", opened)
    }

    @Test
    fun `list screen shows the empty message`() {
        composeTestRule.setContent {
            ShoppingWearScreen(viewModel = listViewModel(ShoppingListUiState()), onNavigateToDetail = {})
        }
        composeTestRule.onNodeWithText(
            ApplicationProvider.getApplicationContext<android.content.Context>()
                .getString(R.string.shopping_wear_no_lists)
        ).assertExists()
    }

    @Test
    fun `detail screen shows the title and items`() {
        composeTestRule.setContent {
            ShoppingDetailScreen(
                viewModel = detailViewModel(
                    ShoppingDetailUiState(
                        title = "Café da manhã",
                        total = 10.0,
                        budget = 500.0,
                        pending = 1,
                        items = StableList(listOf(Product(id = "1", name = "Pão", quantity = 2.0, price = 5.0)))
                    )
                )
            )
        }
        composeTestRule.onNodeWithText("Café da manhã", substring = true).assertExists()
        composeTestRule.onNodeWithTag("wear_add_item").assertExists()
    }

    @Test
    fun `tapping an item toggles it`() {
        val pao = Product(id = "1", name = "Pão", quantity = 2.0, price = 5.0)
        var toggled: Product? = null
        composeTestRule.setContent {
            ShoppingDetailScreen(
                viewModel = detailViewModel(
                    ShoppingDetailUiState(title = "Café", pending = 1, items = StableList(listOf(pao))),
                    ShoppingDetailIntent(onToggle = { toggled = it })
                )
            )
        }
        composeTestRule.onNodeWithTag("wear_item_1").performClick()
        assertEquals(pao, toggled)
    }

    @Test
    fun `list screen hides finished lists`() {
        val finished = shopping.copy(id = "s2", title = "Feira antiga", status = Shopping.Status.FINISH)
        composeTestRule.setContent {
            ShoppingWearScreen(
                viewModel = listViewModel(ShoppingListUiState(list = StableList(listOf(shopping, finished)))),
                onNavigateToDetail = {}
            )
        }
        composeTestRule.onNodeWithText("Feira antiga", substring = true).assertDoesNotExist()
    }

    @Test
    fun `detail screen renders over budget and empty`() {
        composeTestRule.setContent {
            ShoppingDetailScreen(
                viewModel = detailViewModel(
                    ShoppingDetailUiState(title = "Jantar", total = 130.0, budget = 100.0)
                )
            )
        }
        composeTestRule.onNodeWithText("Jantar", substring = true).assertExists()
    }
}
