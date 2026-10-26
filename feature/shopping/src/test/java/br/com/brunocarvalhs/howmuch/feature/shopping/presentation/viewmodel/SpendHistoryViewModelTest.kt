package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.CartFlow
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.SpendHistoryBudget
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.SpendHistoryInProgress
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.SpendHistoryMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.SpendHistoryPurchaseEntry
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.GetSpendHistoryUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.SpendHistoryPurchase
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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

private const val CURRENT_MONTH_TOTAL = 24.6
private const val PREVIOUS_MONTH_TOTAL = 26.58
private const val BUDGET_AMOUNT = 300.0
private const val BUDGET_SPENT = 320.0
private const val IN_PROGRESS_TOTAL = 12.0
private const val IN_PROGRESS_LIST_COUNT = 1
private const val UNKNOWN_PURCHASE_TOTAL = 10.0
private const val ZERO_DELTA = 0.0
private const val MONTHS_IN_FIXTURE = 2

@OptIn(ExperimentalCoroutinesApi::class)
class SpendHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getSpendHistoryUseCase = mockk<GetSpendHistoryUseCase>()

    private fun yearMonth(year: Int, month: Int): YearMonth = YearMonth.of(year, month)

    private fun shopping(id: String, title: String = "List $id") = Shopping(
        id = id,
        title = title,
        description = "Market",
        price = CURRENT_MONTH_TOTAL,
        status = Shopping.Status.FINISH,
        users = listOf("u1"),
        roles = mapOf("u1" to "OWNER")
    )

    private fun months(current: YearMonth, purchases: List<SpendHistoryPurchaseEntry> = emptyList()) = listOf(
        SpendHistoryMonth(
            yearMonth = current.minusMonths(1),
            isCurrent = false,
            total = PREVIOUS_MONTH_TOTAL,
            budget = null,
            inProgress = null,
            purchases = emptyList()
        ),
        SpendHistoryMonth(
            yearMonth = current,
            isCurrent = true,
            total = purchases.sumOf { it.total },
            budget = SpendHistoryBudget(amount = BUDGET_AMOUNT, spent = BUDGET_SPENT),
            inProgress = SpendHistoryInProgress(total = IN_PROGRESS_TOTAL, listCount = IN_PROGRESS_LIST_COUNT),
            purchases = purchases
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState starts in a loading state before the use case resolves`() = runTest {
        coEvery { getSpendHistoryUseCase(any()) } returns emptyList()

        val viewModel = SpendHistoryViewModel(getSpendHistoryUseCase)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `init maps the use case output to the UI state, selecting the current month`() = runTest {
        val currentMonth = yearMonth(year = 2026, month = 6)
        val entry = SpendHistoryPurchaseEntry(
            shopping = shopping(id = "s1"),
            purchaseDate = System.currentTimeMillis(),
            total = CURRENT_MONTH_TOTAL,
            isEstimated = false
        )
        coEvery { getSpendHistoryUseCase(any()) } returns months(currentMonth, listOf(entry))

        val viewModel = SpendHistoryViewModel(getSpendHistoryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(MONTHS_IN_FIXTURE, state.months.size)
        assertEquals(currentMonth.toString(), state.selectedMonthId)

        val currentMonthUi = state.months.single { it.isCurrent }
        assertEquals(CURRENT_MONTH_TOTAL, currentMonthUi.total, ZERO_DELTA)
        assertEquals(1, currentMonthUi.purchases.size)
        assertEquals("s1", currentMonthUi.purchases.single().id)
        assertEquals(BUDGET_AMOUNT, currentMonthUi.budget?.amount ?: ZERO_DELTA, ZERO_DELTA)
        assertEquals(IN_PROGRESS_TOTAL, currentMonthUi.inProgress?.total ?: ZERO_DELTA, ZERO_DELTA)
    }

    @Test
    fun `init maps a use case failure to an error state`() = runTest {
        coEvery { getSpendHistoryUseCase(any()) } throws IllegalStateException("boom")

        val viewModel = SpendHistoryViewModel(getSpendHistoryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `onRetry reloads the history after a previous failure`() = runTest {
        coEvery { getSpendHistoryUseCase(any()) } throws IllegalStateException("boom")
        val viewModel = SpendHistoryViewModel(getSpendHistoryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        coEvery { getSpendHistoryUseCase(any()) } returns months(yearMonth(year = 2026, month = 6))
        viewModel.onRetry()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(MONTHS_IN_FIXTURE, viewModel.uiState.value.months.size)
    }

    @Test
    fun `onPurchaseClicked navigates to CartFlow for the matching shopping`() = runTest {
        val currentMonth = yearMonth(year = 2026, month = 6)
        val targetShopping = shopping(id = "s1")
        val entry = SpendHistoryPurchaseEntry(
            shopping = targetShopping,
            purchaseDate = System.currentTimeMillis(),
            total = CURRENT_MONTH_TOTAL,
            isEstimated = false
        )
        coEvery { getSpendHistoryUseCase(any()) } returns months(currentMonth, listOf(entry))
        val navigator = mockk<Navigator>(relaxed = true)

        val viewModel = SpendHistoryViewModel(getSpendHistoryUseCase)
        viewModel.setNavigator(navigator)
        testDispatcher.scheduler.advanceUntilIdle()

        val purchase = viewModel.uiState.value.months.single { it.isCurrent }.purchases.single()
        viewModel.onPurchaseClicked(purchase)

        verify { navigator.navigate(CartFlow(targetShopping)) }
    }

    @Test
    fun `onPurchaseClicked does nothing for an unknown purchase id`() = runTest {
        coEvery { getSpendHistoryUseCase(any()) } returns months(yearMonth(year = 2026, month = 6))
        val navigator = mockk<Navigator>(relaxed = true)

        val viewModel = SpendHistoryViewModel(getSpendHistoryUseCase)
        viewModel.setNavigator(navigator)
        testDispatcher.scheduler.advanceUntilIdle()

        val unknownPurchase = SpendHistoryPurchase(
            id = "unknown",
            timestamp = System.currentTimeMillis(),
            emoji = null,
            title = "Ghost",
            establishment = null,
            total = UNKNOWN_PURCHASE_TOTAL
        )
        viewModel.onPurchaseClicked(unknownPurchase)

        verify(exactly = 0) { navigator.navigate(any()) }
    }
}
