package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.CartFlow
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.SpendingInsights
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ObserveHistoryUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.historyMonth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class HistoryViewModelTest {

    private val observeHistory = mockk<ObserveHistoryUseCase>()
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)

    private val sep = historyMonth(2026, 9, "s1")
    private val nov = historyMonth(2026, 11, "n1", "n2")

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = HistoryViewModel(observeHistory, analytics).also { it.setNavigator(navigator) }

    @Test
    fun `starts loading before the first emission`() {
        every { observeHistory() } returns MutableSharedFlow()

        assertTrue(viewModel().uiState.value.isLoading)
    }

    @Test
    fun `selects the most recent month once loaded`() {
        every { observeHistory() } returns flowOf(listOf(sep, nov))

        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals(YearMonth.of(2026, 11), state.selectedMonth)
        assertEquals(listOf("n1", "n2"), state.selectedPurchases.map { it.shopping.id })
    }

    @Test
    fun `onMonthSelected swaps the visible purchases and tracks it`() {
        every { observeHistory() } returns flowOf(listOf(sep, nov))
        val vm = viewModel()

        vm.intent.onMonthSelected(YearMonth.of(2026, 9))

        assertEquals(listOf("s1"), vm.uiState.value.selectedPurchases.map { it.shopping.id })
        verify { analytics.trackEvent(AnalyticsEvents.HISTORY_MONTH_SELECTED, mapOf("month" to "2026-09")) }
    }

    @Test
    fun `keeps the selection when the month still exists after a new emission`() {
        val source = MutableSharedFlow<List<HistoryMonth>>(replay = 1)
        every { observeHistory() } returns source
        source.tryEmit(listOf(sep, nov))
        val vm = viewModel()
        vm.intent.onMonthSelected(YearMonth.of(2026, 9))

        source.tryEmit(listOf(sep, historyMonth(2026, 11, "n1", "n2", "n3")))

        assertEquals(YearMonth.of(2026, 9), vm.uiState.value.selectedMonth)
    }

    @Test
    fun `resets to the latest month when the selected one disappears`() {
        val source = MutableSharedFlow<List<HistoryMonth>>(replay = 1)
        every { observeHistory() } returns source
        source.tryEmit(listOf(sep, nov))
        val vm = viewModel()
        vm.intent.onMonthSelected(YearMonth.of(2026, 9))

        source.tryEmit(listOf(nov))

        assertEquals(YearMonth.of(2026, 11), vm.uiState.value.selectedMonth)
    }

    @Test
    fun `no months means empty and no selection`() {
        every { observeHistory() } returns flowOf(emptyList())

        val state = viewModel().uiState.value

        assertTrue(state.isEmpty)
        assertNull(state.selectedMonth)
    }

    @Test
    fun `a failing flow surfaces an error and stops loading`() {
        every { observeHistory() } returns flow { throw IllegalStateException("offline") }

        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `onRetry subscribes again`() {
        var attempts = 0
        every { observeHistory() } answers {
            attempts++
            if (attempts == 1) flow { throw IllegalStateException("offline") } else flowOf(listOf(nov))
        }
        val vm = viewModel()

        vm.intent.onRetry()

        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.months.size)
    }

    @Test
    fun `onPurchaseClick opens the cart of that list and tracks it`() {
        every { observeHistory() } returns flowOf(listOf(nov))
        val vm = viewModel()
        val purchase = vm.uiState.value.selectedPurchases.first()

        vm.intent.onPurchaseClick(purchase)

        verify { navigator.navigate(CartFlow(purchase.shopping)) }
        verify { analytics.trackEvent(AnalyticsEvents.HISTORY_PURCHASE_OPENED, mapOf("shopping_id" to "n1")) }
    }

    @Test
    fun `onSeeAnalysis opens the spending insights`() {
        every { observeHistory() } returns flowOf(listOf(nov))

        viewModel().intent.onSeeAnalysis()

        verify { navigator.navigate(SpendingInsights) }
    }

    @Test
    fun `tracks the screen view and a single history_viewed with the purchase count`() {
        val source = MutableSharedFlow<List<HistoryMonth>>(replay = 1)
        every { observeHistory() } returns source
        source.tryEmit(listOf(sep, nov))
        viewModel()
        source.tryEmit(listOf(sep, nov))

        verify { analytics.trackScreenView("history", "HistoryViewModel") }
        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvents.HISTORY_VIEWED, mapOf("items_count" to 3)) }
    }
}
