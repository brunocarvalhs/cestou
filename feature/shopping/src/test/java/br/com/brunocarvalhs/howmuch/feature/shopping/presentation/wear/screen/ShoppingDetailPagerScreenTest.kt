package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.ShoppingDetailIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.ShoppingDetailUiState
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.ShoppingDetailViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Testes de layout via Robolectric (JVM) da tela de detalhe em três visões (Wear).
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ShoppingDetailPagerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val products = listOf(
        Product(id = "p1", name = "Arroz", quantity = 2.0, price = 25.0),
        Product(id = "p2", name = "Feijão", quantity = 1.0, price = 8.5)
    )

    private fun viewModel(state: ShoppingDetailUiState, intent: ShoppingDetailIntent = ShoppingDetailIntent()) =
        mockk<ShoppingDetailViewModel>(relaxed = true) {
            every { uiState } returns MutableStateFlow(state)
            every { this@mockk.intent } returns intent
        }

    private val state = ShoppingDetailUiState(
        title = "Café da manhã",
        total = 73.5,
        budget = 100.0,
        pending = 2,
        items = StableList(products)
    )

    @Test
    fun opensOnTotalPage() {
        composeTestRule.setContent {
            ShoppingDetailPagerScreen(viewModel = viewModel(state), onNavigateToAi = {})
        }

        composeTestRule.onNodeWithTag(TAG_DETAIL_TOTAL).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_DETAIL_SEARCH_BUTTON).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_DETAIL_AI_BUTTON).assertIsDisplayed()
    }

    @Test
    fun searchButtonOpensProductsWithSearchField() {
        composeTestRule.setContent {
            ShoppingDetailPagerScreen(viewModel = viewModel(state), onNavigateToAi = {})
        }

        composeTestRule.onNodeWithTag(TAG_DETAIL_SEARCH_BUTTON).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TAG_SEARCH_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag("wear_item_p1").assertIsDisplayed()
    }

    @Test
    fun aiButtonNavigatesToAssistant() {
        var openedAi = false
        composeTestRule.setContent {
            ShoppingDetailPagerScreen(viewModel = viewModel(state), onNavigateToAi = { openedAi = true })
        }

        composeTestRule.onNodeWithTag(TAG_DETAIL_AI_BUTTON).performClick()

        assertTrue(openedAi)
    }

    @Test
    fun swipingLeftShowsProductsThenBudget() {
        composeTestRule.setContent {
            ShoppingDetailPagerScreen(viewModel = viewModel(state), onNavigateToAi = {})
        }

        composeTestRule.onNodeWithTag(TAG_DETAIL_PAGER).performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("wear_item_p2").assertIsDisplayed()

        composeTestRule.onNodeWithTag(TAG_DETAIL_PAGER).performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_DETAIL_BUDGET_BAR).assertIsDisplayed()
    }

    @Test
    fun togglingAProductCallsIntent() {
        val toggled = mutableListOf<Product>()
        val intent = ShoppingDetailIntent(onToggle = { toggled += it })
        composeTestRule.setContent {
            ShoppingDetailPagerScreen(viewModel = viewModel(state, intent), onNavigateToAi = {})
        }

        composeTestRule.onNodeWithTag(TAG_DETAIL_SEARCH_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("wear_item_p1").performClick()

        assertEquals(listOf("p1"), toggled.map { it.id })
    }

    private val unpriced = Product(id = "p3", name = "Sabão", quantity = 1.0)

    @Test
    fun tappingUnpricedProductOpensValueInput() {
        val withUnpriced = state.copy(items = StableList(listOf(unpriced) + products))
        composeTestRule.setContent {
            ShoppingDetailPagerScreen(viewModel = viewModel(withUnpriced), onNavigateToAi = {})
        }

        composeTestRule.onNodeWithTag(TAG_DETAIL_SEARCH_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("wear_item_p3").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TAG_DETAIL_PRICE_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TAG_DETAIL_QUANTITY_FIELD).assertIsDisplayed()
    }

    @Test
    fun savingValueSendsPriceAndQuantityToIntent() {
        val saved = mutableListOf<Product>()
        val intent = ShoppingDetailIntent(onSaveDetails = { saved += it })
        val withUnpriced = state.copy(items = StableList(listOf(unpriced) + products))
        composeTestRule.setContent {
            ShoppingDetailPagerScreen(viewModel = viewModel(withUnpriced, intent), onNavigateToAi = {})
        }

        composeTestRule.onNodeWithTag(TAG_DETAIL_SEARCH_BUTTON).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("wear_item_p3").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(TAG_DETAIL_PRICE_FIELD).performTextInput("1250")
        composeTestRule.onNodeWithTag(TAG_DETAIL_QUANTITY_FIELD).performTextReplacement("2")
        composeTestRule.onNodeWithTag(TAG_DETAIL_SAVE_VALUE).performClick()

        assertEquals(1, saved.size)
        assertEquals("p3", saved.single().id)
        assertEquals(12.5, saved.single().price ?: 0.0, 0.0001)
        assertEquals(2.0, saved.single().quantity, 0.0001)
    }
}
