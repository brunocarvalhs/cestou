package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase.CartAssistantUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.GetQuestionSuggestionsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ShoppingClearPurchasedUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.SortProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.CartFlow
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.GetSettingsUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val repository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>()
    private val assistantUseCase = mockk<CartAssistantUseCase>()
    private val getQuestionSuggestionsUseCase = mockk<GetQuestionSuggestionsUseCase>()
    private val clearPurchasedUseCase = mockk<ShoppingClearPurchasedUseCase>()
    private val getSettingsUseCase = mockk<GetSettingsUseCase>()
    private val sortProductsUseCase = mockk<SortProductsUseCase>()

    private val shopping = Shopping(
        id = "list1",
        title = "Test List",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        coEvery { repository.observeById(any()) } returns flowOf(shopping)
        coEvery { repository.observeAll() } returns flowOf(emptyList())
        coEvery { productsUseCase(any()) } returns flowOf(emptyList())
        coEvery { getSettingsUseCase() } returns flowOf(mockk(relaxed = true))
        coEvery { sortProductsUseCase(any(), any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState should emit initial shopping`() = runTest {
        assertEquals("list1", shopping.id)
    }
}
