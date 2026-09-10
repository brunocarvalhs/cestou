package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ShoppingClearPurchasedUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.SortProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.GetSettingsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Regression coverage for G12 (.specs/MVP-ROADMAP.md): `CartViewModel.observeData()` used to
 * launch a brand new `useCase(shopping.id).collect { ... }` coroutine every time settings
 * emitted, without cancelling the previous one, leaking one permanent product collector per
 * settings write anywhere in the app (theme, language, AI prefs, sorting mode...) and running
 * `sortProductsUseCase`/`resolveMemberProfiles` once per leaked collector. `flatMapLatest` must
 * cancel the previous product collector as soon as a new settings value arrives, so at most one
 * collector is ever active at a time, no matter how many settings writes happen in sequence, and
 * the products feeding the cart total (`CartScreen`'s `uiState.products.sumOf { it.total }`)
 * always reflect the latest settings/products pair.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelObserveDataTest {

    private companion object {
        const val SETTINGS_WRITE_COUNT = 4
        const val EXPECTED_TOTAL = 8.0
    }

    private val testDispatcher = UnconfinedTestDispatcher()

    private val repository = mockk<ShoppingRepository>()
    private val productsUseCase = mockk<ProductsUseCase>()
    private val clearPurchasedUseCase = mockk<ShoppingClearPurchasedUseCase>(relaxed = true)
    private val getSettingsUseCase = mockk<GetSettingsUseCase>()
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
            getSettingsUseCase,
            sortProductsUseCase,
            userRepository,
            analyticsTracker
        )
        vm.setNavigator(navigator)
        return vm
    }

    @Test
    fun `a new settings emission cancels the previous product collector instead of stacking a new one`() =
        runTest {
            val activeCollectors = AtomicInteger(0)
            val maxConcurrentCollectors = AtomicInteger(0)
            val totalCollectorStarts = AtomicInteger(0)

            // Never completes on its own (mirrors a real Firestore snapshot listener) so the only
            // way a previous collection stops is via `flatMapLatest` cancelling it.
            val trackedProducts: Flow<List<Product>> = flow {
                totalCollectorStarts.incrementAndGet()
                val current = activeCollectors.incrementAndGet()
                maxConcurrentCollectors.updateAndGet { previous -> maxOf(previous, current) }
                try {
                    emit(emptyList())
                    awaitCancellation()
                } finally {
                    activeCollectors.decrementAndGet()
                }
            }
            coEvery { productsUseCase(shopping.id) } returns trackedProducts

            val settingsFlow = MutableSharedFlow<AppSettings>(replay = 1)
            every { getSettingsUseCase() } returns settingsFlow
            settingsFlow.tryEmit(AppSettings(sortingMode = "CATEGORY"))

            viewModel()

            // Simulate several settings writes in sequence (e.g. theme, then language, then sorting).
            settingsFlow.tryEmit(AppSettings(sortingMode = "PRICE"))
            settingsFlow.tryEmit(AppSettings(sortingMode = "NAME"))
            settingsFlow.tryEmit(AppSettings(sortingMode = "CATEGORY"))

            assertEquals(SETTINGS_WRITE_COUNT, totalCollectorStarts.get())
            assertTrue(
                "expected at most one active product collector at a time, saw $maxConcurrentCollectors",
                maxConcurrentCollectors.get() <= 1
            )
            assertEquals(1, activeCollectors.get())
        }

    @Test
    fun `products stay correct for the total after several settings writes in sequence`() = runTest {
        val products = listOf(
            Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0),
            Product(id = "p2", name = "Bread", quantity = 1.0, price = 3.0)
        )
        coEvery { productsUseCase(shopping.id) } returns flowOf(products)
        every { sortProductsUseCase(products, "PRICE") } returns products.sortedBy { it.price }
        every { sortProductsUseCase(products, "NAME") } returns products.sortedBy { it.name }

        val settingsFlow = MutableSharedFlow<AppSettings>(replay = 1)
        every { getSettingsUseCase() } returns settingsFlow
        settingsFlow.tryEmit(AppSettings(sortingMode = "PRICE"))

        val vm = viewModel()

        settingsFlow.tryEmit(AppSettings(sortingMode = "NAME"))

        assertEquals(products.sortedBy { it.name }, vm.uiState.value.products.toList())
        assertEquals(EXPECTED_TOTAL, vm.uiState.value.products.sumOf { it.total }, 0.0)
    }
}
