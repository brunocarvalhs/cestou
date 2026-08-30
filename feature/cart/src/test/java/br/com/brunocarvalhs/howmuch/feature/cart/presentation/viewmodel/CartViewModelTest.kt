package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.CartFlow
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.ConfirmItemRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.FinishPurchaseRoute
import br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase.CartAssistantUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.GetQuestionSuggestionsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ShoppingClearPurchasedUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.SortProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.GetSettingsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val repository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>()
    private val assistantUseCase = mockk<CartAssistantUseCase>()
    private val getQuestionSuggestionsUseCase = mockk<GetQuestionSuggestionsUseCase>()
    private val clearPurchasedUseCase = mockk<ShoppingClearPurchasedUseCase>(relaxed = true)
    private val getSettingsUseCase = mockk<GetSettingsUseCase>()
    private val sortProductsUseCase = mockk<SortProductsUseCase>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observeById(shopping.id) } returns MutableStateFlow(shopping)
        every { repository.observeAll() } returns flowOf(emptyList())
        every { getSettingsUseCase() } returns flowOf(mockk(relaxed = true))
        coEvery { productsUseCase(shopping.id) } returns flowOf(emptyList())
        every { sortProductsUseCase(any(), any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): CartViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("shopping" to navJson.encodeToString(shopping)))
        val vm = CartViewModel(
            savedStateHandle,
            repository,
            productsUseCase,
            assistantUseCase,
            getQuestionSuggestionsUseCase,
            clearPurchasedUseCase,
            getSettingsUseCase,
            sortProductsUseCase,
            analyticsTracker
        )
        vm.setNavigator(navigator)
        return vm
    }

    @Test
    fun `init reads the shopping list from the route and tracks screen_view`() {
        val vm = viewModel()

        assertEquals(shopping, vm.uiState.value.shopping)
        verify { analyticsTracker.trackScreenView("cart", "CartViewModel") }
    }

    @Test
    fun `onDeleteProduct deletes the product and tracks the event`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        coEvery { productsUseCase.delete("p1", "list1") } returns Result.success(Unit)
        val vm = viewModel()

        vm.intent.onDeleteProduct(product)

        coVerify { productsUseCase.delete("p1", "list1") }
        coVerify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.CART_PRODUCT_DELETED,
                mapOf("shopping_id" to "list1", "product_id" to "p1")
            )
        }
    }

    @Test
    fun `onUpdateQuantity deletes the product when the new quantity is zero or less`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        coEvery { productsUseCase.delete("p1", "list1") } returns Result.success(Unit)
        val vm = viewModel()

        vm.intent.onUpdateQuantity(product, 0.0)

        coVerify { productsUseCase.delete("p1", "list1") }
        coVerify(exactly = 0) { productsUseCase.update(any(), any()) }
    }

    @Test
    fun `onTogglePurchased navigates to confirm item when the product has no price yet`() {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 0.0)
        val vm = viewModel()

        vm.intent.onTogglePurchased(product, true)

        verify { navigator.navigate(ConfirmItemRoute(product, "list1")) }
    }

    @Test
    fun `onTogglePurchased marks the product purchased directly when it already has a price`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        coEvery { productsUseCase.update(any(), "list1") } returns Result.success(Unit)
        val vm = viewModel()

        vm.intent.onTogglePurchased(product, true)

        coVerify { productsUseCase.update(product.copy(isPurchased = true), "list1") }
    }

    @Test
    fun `onClearPurchased delegates to the clear purchased use case`() = runTest {
        val vm = viewModel()

        vm.intent.onClearPurchased()

        coVerify { clearPurchasedUseCase("list1") }
    }

    @Test
    fun `onToggleFinishPurchaseSheet tracks the event and navigates to FinishPurchaseRoute`() {
        val vm = viewModel()

        vm.intent.onToggleFinishPurchaseSheet()

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.CART_FINISH_PURCHASE_STARTED,
                mapOf("shopping_id" to "list1")
            )
        }
        verify { navigator.navigate(FinishPurchaseRoute) }
    }

    @Test
    fun `onPromptChanged updates the prompt in the ui state`() {
        val vm = viewModel()

        vm.intent.onPromptChanged("how much did I spend?")

        assertEquals("how much did I spend?", vm.uiState.value.prompt)
    }
}
