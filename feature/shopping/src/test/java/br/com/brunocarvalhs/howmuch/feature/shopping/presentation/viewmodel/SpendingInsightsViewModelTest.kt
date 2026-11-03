package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.CartFlow
import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.CategorySpending
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.GetCategorySpendingUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ObserveMonthlySpendingUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.historyMonth
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class SpendingInsightsViewModelTest {

    private val observeMonthly = mockk<ObserveMonthlySpendingUseCase>()
    private val categorySpending = mockk<GetCategorySpendingUseCase>()
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)

    private val sep = historyMonth(2026, 9, "s1")
    private val nov = historyMonth(2026, 11, "n1", "n2")

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { categorySpending(any()) } returns Result.success(listOf(CategorySpending("carnes", 200.0)))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        SpendingInsightsViewModel(observeMonthly, categorySpending, analytics, mockk(relaxed = true)).also {
            it.setNavigator(navigator)
        }

    @Test
    fun `starts loading before the first emission`() {
        every { observeMonthly() } returns MutableSharedFlow<List<HistoryMonth>>()

        assertTrue(viewModel().uiState.value.isLoading)
    }

    @Test
    fun `selects the latest month and loads its chart, ranking and purchases`() {
        every { observeMonthly() } returns flowOf(listOf(sep, nov))

        val state = viewModel().uiState.value

        assertEquals(YearMonth.of(2026, 11), state.selectedMonth)
        assertEquals(2, state.months.size)
        assertEquals(listOf("n1", "n2"), state.purchases.map { it.shopping.id })
        assertEquals(ProductCategory.CARNES, state.categories.single().category)
        assertFalse(state.categoriesLoading)
        verify(exactly = 0) { analytics.trackScreenView(any(), any()) }
    }

    @Test
    fun `selecting a month reloads only that month and tracks it`() {
        every { observeMonthly() } returns flowOf(listOf(sep, nov))
        val vm = viewModel()

        vm.intent.onMonthSelected(YearMonth.of(2026, 9))

        assertEquals(listOf("s1"), vm.uiState.value.purchases.map { it.shopping.id })
        coVerify { categorySpending(sep.purchases.map { it.shopping }) }
        verify {
            analytics.trackEvent(AnalyticsEvents.SPENDING_INSIGHTS_MONTH_SELECTED, mapOf("month" to "2026-09"))
        }
    }

    @Test
    fun `switching month cancels the read still in flight`() {
        every { observeMonthly() } returns flowOf(listOf(sep, nov))
        val slow = CompletableDeferred<Result<List<CategorySpending>>>()
        coEvery { categorySpending(nov.purchases.map { it.shopping }) } coAnswers { slow.await() }
        coEvery { categorySpending(sep.purchases.map { it.shopping }) } returns
            Result.success(listOf(CategorySpending("bebidas", 100.0)))
        val vm = viewModel()
        assertTrue(vm.uiState.value.categoriesLoading)

        vm.intent.onMonthSelected(YearMonth.of(2026, 9))
        slow.complete(Result.success(listOf(CategorySpending("carnes", 1.0))))

        assertEquals(ProductCategory.BEBIDAS, vm.uiState.value.categories.single().category)
    }

    @Test
    fun `a ranking failure keeps the chart and flags only the categories`() {
        every { observeMonthly() } returns flowOf(listOf(nov))
        coEvery { categorySpending(any()) } returns Result.failure(IllegalStateException("offline"))

        val state = viewModel().uiState.value

        assertNull(state.error)
        assertEquals(1, state.months.size)
        assertNotNull(state.categoriesError)
        assertTrue(state.categories.isEmpty())
    }

    @Test
    fun `onRetry reloads the categories after a ranking failure`() {
        every { observeMonthly() } returns flowOf(listOf(nov))
        coEvery { categorySpending(any()) } returnsMany listOf(
            Result.failure(IllegalStateException("offline")),
            Result.success(listOf(CategorySpending("carnes", 200.0)))
        )
        val vm = viewModel()

        vm.intent.onRetry()

        assertNull(vm.uiState.value.categoriesError)
        assertEquals(1, vm.uiState.value.categories.size)
    }

    @Test
    fun `a failing months flow shows the screen error and retry recovers`() {
        var attempts = 0
        every { observeMonthly() } answers {
            attempts++
            if (attempts == 1) flow { throw IllegalStateException("offline") } else flowOf(listOf(nov))
        }
        val vm = viewModel()
        assertNotNull(vm.uiState.value.error)

        vm.intent.onRetry()

        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.months.size)
    }

    @Test
    fun `no months means empty and never reads products`() {
        every { observeMonthly() } returns flowOf(emptyList())

        val state = viewModel().uiState.value

        assertTrue(state.isEmpty)
        coVerify(exactly = 0) { categorySpending(any()) }
    }

    @Test
    fun `flags when the products sum differs from the month total`() {
        every { observeMonthly() } returns flowOf(listOf(historyMonth(2026, 11, "n1", price = 500.0)))

        assertTrue(viewModel().uiState.value.categoriesDiverge)
    }

    @Test
    fun `does not flag when the products sum matches the month total`() {
        every { observeMonthly() } returns flowOf(listOf(nov))

        assertFalse(viewModel().uiState.value.categoriesDiverge)
    }

    @Test
    fun `onPurchaseClick opens the cart and tracks it`() {
        every { observeMonthly() } returns flowOf(listOf(nov))
        val vm = viewModel()
        val purchase = vm.uiState.value.purchases.first()

        vm.intent.onPurchaseClick(purchase)

        verify { navigator.navigate(CartFlow(purchase.shopping)) }
        verify {
            analytics.trackEvent(AnalyticsEvents.SPENDING_INSIGHTS_LIST_OPENED, mapOf("shopping_id" to "n1"))
        }
    }

    @Test
    fun `onBack goes back`() {
        every { observeMonthly() } returns flowOf(listOf(nov))

        viewModel().intent.onBack()

        verify { navigator.goBack() }
    }
}
