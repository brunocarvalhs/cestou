package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.Recipe
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSearchUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.RecipeSearchUseCase
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.ProductSearchViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ProductSearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val searchUseCase = mockk<ProductSearchUseCase>()
    private val recipeSearchUseCase = mockk<RecipeSearchUseCase>()
    private val saveUseCase = mockk<ProductSaveUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )

    private fun viewModel(): ProductSearchViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("shopping" to navJson.encodeToString(shopping)))
        return ProductSearchViewModel(savedStateHandle, searchUseCase, recipeSearchUseCase, saveUseCase, analyticsTracker)
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
    fun `init tracks a product_search screen_view`() {
        viewModel()

        verify { analyticsTracker.trackScreenView("product_search", "ProductSearchViewModel") }
    }

    @Test
    fun `onQueryChange below the minimum length does not trigger a search`() = runTest {
        val vm = viewModel()

        vm.intent.onQueryChange("ab")

        coVerify(exactly = 0) { searchUseCase(any()) }
    }

    @Test
    fun `onQueryChange at the minimum length searches and tracks the event`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        coEvery { searchUseCase("milk") } returns Result.success(listOf(product))
        val vm = viewModel()

        vm.intent.onQueryChange("milk")

        assertEquals(listOf(product), vm.uiState.value.results)
        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.PRODUCT_SEARCH_PERFORMED,
                mapOf("search_mode" to "product", "query_length" to 4, "result_count" to 1)
            )
        }
    }

    @Test
    fun `onProductSelected saves the product and tracks the event`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        val vm = viewModel()

        vm.intent.onProductSelected(product)

        coVerify { saveUseCase(product, "list1") }
        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.PRODUCT_SELECTED,
                mapOf("shopping_id" to "list1", "product_id" to "p1")
            )
        }
    }

    @Test
    fun `onAddRecipeIngredients saves every ingredient and clears the recipe selection`() = runTest {
        val recipe = Recipe(
            id = "r1",
            name = "Bolo",
            description = "desc",
            ingredients = listOf(Product(id = "p1", name = "Flour", quantity = 1.0, price = 2.0))
        )
        val vm = viewModel()

        vm.intent.onAddRecipeIngredients(recipe)

        coVerify { saveUseCase(match { it.name == "Flour" }, "list1") }
        assertEquals(null, vm.uiState.value.selectedRecipe)
    }
}
