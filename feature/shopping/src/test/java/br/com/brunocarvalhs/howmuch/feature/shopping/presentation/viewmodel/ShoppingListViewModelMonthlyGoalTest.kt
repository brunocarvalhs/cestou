package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetLevel
import br.com.brunocarvalhs.howmuch.core.domain.budget.MonthlySpendingCalculator
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.MonthlyGoalRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ObserveUnreadNotificationsCountUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** sdd-036: cartão da meta mensal na Home. Separado para não estourar o limite de funções do Detekt. */
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelMonthlyGoalTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<ShoppingRepository>(relaxed = true)
    private val goal = MutableStateFlow<Double?>(null)
    private val monthlyGoalRepository = mockk<MonthlyGoalRepository> { every { observeGoal() } returns goal }
    private val featureFlags = mockk<FeatureFlagService>()
    private val settingsRepository = mockk<SettingsRepository> {
        every { getSettings() } returns flowOf(AppSettings())
    }
    private val observeUnread =
        mockk<ObserveUnreadNotificationsCountUseCase> { every { this@mockk.invoke() } returns flowOf(0) }
    private val authService = mockk<AuthService> { every { currentUser } returns AuthenticatedUser(id = "u1") }

    private fun finished(id: String, price: Double) = Shopping(
        id = id,
        title = id,
        description = "",
        price = price,
        status = Shopping.Status.FINISH,
        users = listOf("u1"),
        roles = emptyMap(),
        finishedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { featureFlags.isEnabled(FeatureFlagKeys.BUDGET_ALERTS_ENABLED, true) } returns true
        every { repository.observeAll() } returns flowOf(listOf(finished("a", 300.0), finished("b", 200.0)))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ShoppingListViewModel(
        repository,
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        settingsRepository,
        observeUnread,
        authService,
        mockk<AnalyticsTracker>(relaxed = true),
        mockk(relaxed = true),
        monthlyGoalRepository,
        MonthlySpendingCalculator(),
        featureFlags
    )

    @Test
    fun `without a goal the card state stays empty`() {
        assertNull(viewModel().uiState.value.monthlyGoalProgress)
    }

    @Test
    fun `with a goal the progress counts the finished lists of the month`() {
        goal.value = 1000.0

        val progress = viewModel().uiState.value.monthlyGoalProgress!!

        assertEquals(500.0, progress.spent, 1e-9)
        assertEquals(1000.0, progress.limit!!, 1e-9)
        assertEquals(BudgetLevel.OK, progress.level)
    }

    @Test
    fun `the card reacts when the goal changes`() {
        goal.value = 1000.0
        val vm = viewModel()

        goal.value = 600.0

        assertEquals(BudgetLevel.WARNING, vm.uiState.value.monthlyGoalProgress!!.level)
    }

    @Test
    fun `removing the goal removes the card`() {
        goal.value = 1000.0
        val vm = viewModel()

        goal.value = null

        assertNull(vm.uiState.value.monthlyGoalProgress)
    }

    @Test
    fun `the remote kill switch hides the card even with a goal`() {
        goal.value = 1000.0
        every { featureFlags.isEnabled(FeatureFlagKeys.BUDGET_ALERTS_ENABLED, true) } returns false

        assertNull(viewModel().uiState.value.monthlyGoalProgress)
    }
}
