package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingGetByIdUseCase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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
class ShoppingDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val shoppingGetByIdUseCase = mockk<ShoppingGetByIdUseCase>()
    private val productsUseCase = mockk<ProductsUseCase>()

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        budget = 100.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )

    private val products = listOf(
        Product(id = "p1", name = "Milk", quantity = 1.0, price = 10.0, category = "Dairy"),
        Product(id = "p2", name = "Bread", quantity = 2.0, price = 15.0, category = "Bakery")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): ShoppingDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("shoppingId" to "list1"))
        return ShoppingDetailViewModel(savedStateHandle, shoppingGetByIdUseCase, productsUseCase)
    }

    @Test
    fun `loads the shopping details and computes the spending balance`() {
        coEvery { shoppingGetByIdUseCase("list1") } returns Result.success(shopping)
        coEvery { productsUseCase("list1") } returns flowOf(products)

        val state = viewModel().uiState.value

        assertEquals("Weekly Groceries", state.title)
        assertEquals(100.0, state.budget, 0.0)
        assertEquals(40.0, state.totalSpent, 0.0)
        assertEquals(60.0, state.balance, 0.0)
        assertEquals(products, state.items.items)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `falls back to zero budget when the shopping has none set`() {
        coEvery { shoppingGetByIdUseCase("list1") } returns Result.success(shopping.copy(budget = null))
        coEvery { productsUseCase("list1") } returns flowOf(emptyList())

        val state = viewModel().uiState.value

        assertEquals(0.0, state.budget, 0.0)
        assertEquals(0.0, state.balance, 0.0)
    }

    @Test
    fun `surfaces the failure message when loading the shopping fails`() {
        coEvery { shoppingGetByIdUseCase("list1") } returns Result.failure(IllegalStateException("not found"))

        val state = viewModel().uiState.value

        assertEquals("not found", state.error)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `intent onBack delegates to the navigator once it is set`() {
        val navigator = mockk<Navigator>(relaxed = true)
        coEvery { shoppingGetByIdUseCase("list1") } returns Result.success(shopping)
        coEvery { productsUseCase("list1") } returns flowOf(products)
        val vm = viewModel()
        vm.setNavigator(navigator)

        vm.intent.onBack()

        verify { navigator.goBack() }
    }

    @Test
    fun `intent onBack is a no-op before a navigator is set`() {
        coEvery { shoppingGetByIdUseCase("list1") } returns Result.success(shopping)
        coEvery { productsUseCase("list1") } returns flowOf(products)

        viewModel().intent.onBack()
    }
}
