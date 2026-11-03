package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetLevel
import br.com.brunocarvalhs.howmuch.core.domain.budget.MonthlySpendingCalculator
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.MonthlyGoalRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ShoppingClearPurchasedUseCase
import br.com.brunocarvalhs.howmuch.core.domain.usecase.SortProductsUseCase
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.BudgetScope
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
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * sdd-036: linha de orçamento da lista, projeção do mês e avisos. Separado para respeitar o limite
 * de funções do Detekt.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelBudgetTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val products = MutableStateFlow(listOf(product("p1", 50.0)))
    private val goal = MutableStateFlow<Double?>(null)
    private val others = MutableStateFlow<List<Shopping>>(emptyList())
    private val repository = mockk<ShoppingRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val featureFlags = mockk<FeatureFlagService>()
    private val monthlyGoalRepository = mockk<MonthlyGoalRepository> { every { observeGoal() } returns goal }

    private fun product(id: String, price: Double) = Product(id = id, name = id, quantity = 1.0, price = price)

    private fun shopping(budget: Double?, status: Shopping.Status = Shopping.Status.NEW) = Shopping(
        id = "list1",
        title = "Weekly",
        description = "",
        price = if (status == Shopping.Status.FINISH) 80.0 else 0.0,
        status = status,
        users = emptyList(),
        roles = emptyMap(),
        budget = budget,
        finishedAt = if (status == Shopping.Status.FINISH) System.currentTimeMillis() else null
    )

    private fun finishedThisMonth(id: String, price: Double) = Shopping(
        id = id,
        title = id,
        description = "",
        price = price,
        status = Shopping.Status.FINISH,
        users = emptyList(),
        roles = emptyMap(),
        finishedAt = System.currentTimeMillis()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { featureFlags.isEnabled(FeatureFlagKeys.BUDGET_ALERTS_ENABLED, true) } returns true
        every { repository.observeAll() } returns others
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(shopping: Shopping): CartViewModel {
        every { repository.observeById(shopping.id) } returns MutableStateFlow(shopping)
        val settingsRepository =
            mockk<SettingsRepository> { every { getSettings() } returns flowOf(mockk(relaxed = true)) }
        val productsUseCase = mockk<ProductsUseCase>()
        coEvery { productsUseCase(shopping.id) } returns products
        val sortProductsUseCase = mockk<SortProductsUseCase>()
        every { sortProductsUseCase(any(), any()) } answers { firstArg() }
        val userRepository = mockk<UserRepository> { every { getUserProfile(any()) } returns flowOf(null) }
        return CartViewModel(
            SavedStateHandle(mapOf("shopping" to navJson.encodeToString(shopping))),
            repository,
            productsUseCase,
            mockk<ShoppingClearPurchasedUseCase>(relaxed = true),
            settingsRepository,
            sortProductsUseCase,
            userRepository,
            analyticsTracker,
            monthlyGoalRepository,
            MonthlySpendingCalculator(),
            featureFlags
        )
    }

    @Test
    fun `a list with a budget shows the total against it`() {
        val progress = viewModel(shopping(budget = 100.0)).uiState.value.listBudgetProgress!!

        assertEquals(50.0, progress.spent, 1e-9)
        assertEquals(100.0, progress.limit!!, 1e-9)
        assertEquals(BudgetLevel.OK, progress.level)
    }

    @Test
    fun `a list without budget shows no line`() {
        assertNull(viewModel(shopping(budget = null)).uiState.value.listBudgetProgress)
        assertNull(viewModel(shopping(budget = 0.0)).uiState.value.listBudgetProgress)
    }

    @Test
    fun `the month projection adds this open list to the finished ones of the month`() {
        goal.value = 1000.0
        others.value = listOf(finishedThisMonth("a", 300.0), finishedThisMonth("b", 100.0))

        val month = viewModel(shopping(budget = null)).uiState.value.monthProgress!!

        assertEquals(450.0, month.spent, 1e-9)
        assertEquals(1000.0, month.limit!!, 1e-9)
    }

    @Test
    fun `without a goal there is no month line`() {
        assertNull(viewModel(shopping(budget = 100.0)).uiState.value.monthProgress)
    }

    @Test
    fun `a finished list counts by what was paid and not by the items`() {
        goal.value = 1000.0
        others.value = listOf(finishedThisMonth("a", 300.0))

        val month = viewModel(shopping(budget = null, status = Shopping.Status.FINISH)).uiState.value.monthProgress!!

        assertEquals(380.0, month.spent, 1e-9) // 300 das outras + 80 pagos nesta lista
    }

    @Test
    fun `a list finished in an earlier month does not enter the current month`() {
        goal.value = 1000.0
        val old = shopping(budget = null, status = Shopping.Status.FINISH)
            .copy(finishedAt = Instant.parse("2020-01-10T10:00:00Z").toEpochMilli())

        val month = viewModel(old).uiState.value.monthProgress!!

        assertEquals(0.0, month.spent, 1e-9)
    }

    @Test
    fun `the kill switch removes the lines and the alerts`() {
        every { featureFlags.isEnabled(FeatureFlagKeys.BUDGET_ALERTS_ENABLED, true) } returns false
        goal.value = 1000.0

        val state = viewModel(shopping(budget = 100.0)).uiState.value

        assertNull(state.listBudgetProgress)
        assertNull(state.monthProgress)
    }

    @Test
    fun `opening a list already past 80 percent does not alert`() {
        products.value = listOf(product("p1", 90.0))

        assertNull(viewModel(shopping(budget = 100.0)).uiState.value.budgetAlert)
    }

    @Test
    fun `adding items past 80 percent alerts and acknowledging reports the event once`() {
        val vm = viewModel(shopping(budget = 100.0))

        products.value = listOf(product("p1", 50.0), product("p2", 40.0))

        val alert = vm.uiState.value.budgetAlert
        assertNotNull(alert)
        assertEquals(BudgetScope.LIST, alert!!.scope)
        assertEquals(BudgetLevel.WARNING, alert.level)

        vm.intent.onBudgetAlertShown()

        assertNull(vm.uiState.value.budgetAlert)
        verify(exactly = 1) {
            analyticsTracker.trackEvent(
                AnalyticsEvents.BUDGET_ALERT_SHOWN,
                mapOf(AnalyticsParams.SCOPE to "list", AnalyticsParams.LEVEL to "warning")
            )
        }
    }

    @Test
    fun `crossing the monthly goal alerts with the month scope`() {
        goal.value = 400.0
        others.value = listOf(finishedThisMonth("a", 300.0))
        val vm = viewModel(shopping(budget = null))

        products.value = listOf(product("p1", 50.0), product("p2", 70.0))

        assertEquals(BudgetScope.MONTH, vm.uiState.value.budgetAlert!!.scope)
        assertEquals(BudgetLevel.EXCEEDED, vm.uiState.value.budgetAlert!!.level)
    }
}
