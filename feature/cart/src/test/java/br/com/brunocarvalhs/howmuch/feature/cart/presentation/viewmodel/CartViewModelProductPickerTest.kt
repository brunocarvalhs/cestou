package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ShoppingClearPurchasedUseCase
import br.com.brunocarvalhs.howmuch.core.domain.usecase.SortProductsUseCase
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ProductPickerRoute
import io.mockk.coEvery
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Split out of CartViewModelTest to keep TooManyFunctions under the detekt threshold —
// same fixture shape, scoped to the "add product" entry point (empty-state CTA and bottom-bar
// "+") that the Cart activation-funnel fix wires through CartViewModel.onToggleProductPicker.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelProductPickerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val repository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>()
    private val clearPurchasedUseCase = mockk<ShoppingClearPurchasedUseCase>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>()
    private val sortProductsUseCase = mockk<SortProductsUseCase>()
    private val userRepository = mockk<UserRepository>()
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
        every { settingsRepository.getSettings() } returns flowOf(mockk(relaxed = true))
        coEvery { productsUseCase(shopping.id) } returns flowOf(emptyList())
        every { sortProductsUseCase(any(), any()) } returns emptyList()
        every { userRepository.getUserProfile(any()) } returns flowOf(null)
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
            clearPurchasedUseCase,
            settingsRepository,
            sortProductsUseCase,
            userRepository,
            analyticsTracker
        )
        vm.setNavigator(navigator)
        return vm
    }

    @Test
    fun `onToggleProductPicker tracks the source and navigates to the product picker`() = runTest {
        val vm = viewModel()

        vm.intent.onToggleProductPicker("empty_state")

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.CART_ADD_PRODUCT_CLICKED,
                mapOf("shopping_id" to "list1", "source" to "empty_state")
            )
        }
        verify { navigator.navigate(ProductPickerRoute(shopping)) }
    }

    @Test
    fun `onToggleProductPicker tags the bottom-bar source separately from the empty state`() = runTest {
        val vm = viewModel()

        vm.intent.onToggleProductPicker("bottom_bar")

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.CART_ADD_PRODUCT_CLICKED,
                mapOf("shopping_id" to "list1", "source" to "bottom_bar")
            )
        }
    }
}
