package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.GetProductSuggestionsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.presentation.event.ProductSuggestionEvent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.ProductSuggestionViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ProductSuggestionViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getProductSuggestionsUseCase = mockk<GetProductSuggestionsUseCase>()
    private val productSaveUseCase = mockk<ProductSaveUseCase>()

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )
    private val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)

    private fun viewModel(): ProductSuggestionViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("shopping" to navJson.encodeToString(shopping)))
        return ProductSuggestionViewModel(savedStateHandle, getProductSuggestionsUseCase, productSaveUseCase)
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads suggestions for the current shopping list`() {
        every { getProductSuggestionsUseCase("list1") } returns flowOf(listOf(product))

        val vm = viewModel()

        assertEquals(listOf(product), vm.uiState.value.suggestions)
    }

    @Test
    fun `onSearchProduct updates the search query`() {
        every { getProductSuggestionsUseCase("list1") } returns flowOf(emptyList())
        val vm = viewModel()

        vm.intent.onSearchProduct("milk")

        assertEquals("milk", vm.uiState.value.searchQuery)
    }

    @Test
    fun `onAddProduct emits ProductAdded on success`() = runTest {
        every { getProductSuggestionsUseCase("list1") } returns flowOf(emptyList())
        coEvery { productSaveUseCase(product, "list1") } returns Result.success(Unit)
        val vm = viewModel()

        vm.intent.onAddProduct(product)

        assertEquals(ProductSuggestionEvent.ProductAdded, vm.events.first())
    }

    @Test
    fun `onAddProduct emits an Error event on failure`() = runTest {
        every { getProductSuggestionsUseCase("list1") } returns flowOf(emptyList())
        coEvery { productSaveUseCase(product, "list1") } returns Result.failure(IllegalStateException("boom"))
        val vm = viewModel()

        vm.intent.onAddProduct(product)

        assertTrue(vm.events.first() is ProductSuggestionEvent.Error)
    }
}
