package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.repository.MonthlyGoalRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateShoppingPreferencesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val settingsRepository = mockk<SettingsRepository>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val updateShoppingPreferencesUseCase = mockk<UpdateShoppingPreferencesUseCase>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)
    private val goalFlow = MutableStateFlow<Double?>(null)
    private val monthlyGoalRepository = mockk<MonthlyGoalRepository> {
        every { observeGoal() } returns goalFlow
        coEvery { setGoal(any()) } returns Result.success(Unit)
    }

    private fun newViewModel() = ShoppingSettingsViewModel(
        settingsRepository,
        updateShoppingPreferencesUseCase,
        analyticsTracker,
        monthlyGoalRepository
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { settingsRepository.getSettings() } returns flowOf(
            AppSettings(defaultListId = "list1", sortingMode = "NAME", remindersEnabled = true)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads shopping preferences from the repository`() {
        val vm = newViewModel()

        assertEquals("list1", vm.uiState.value.defaultListId)
        assertEquals("NAME", vm.uiState.value.sortingMode)
        assertEquals(true, vm.uiState.value.remindersEnabled)
    }

    @Test
    fun `onUpdateShoppingPreferences forwards to the use case`() = runTest {
        coEvery { updateShoppingPreferencesUseCase("list2", "DATE", false) } returns Unit
        val vm = newViewModel()

        vm.intent.onUpdateShoppingPreferences("list2", "DATE", false)

        coVerify { updateShoppingPreferencesUseCase("list2", "DATE", false) }
    }

    @Test
    fun `onBack navigates back`() {
        val vm = newViewModel()
        vm.setNavigator(navigator)

        vm.intent.onBack()

        verify { navigator.goBack() }
    }

    @Test
    fun `init loads the monthly goal from the account`() {
        goalFlow.value = 1200.0

        assertEquals(1200.0, newViewModel().uiState.value.monthlyGoal!!, 1e-9)
    }

    @Test
    fun `a new monthly goal is saved and reported by bucket, never by value`() = runTest {
        val vm = newViewModel()
        val params = slot<Map<String, Any>>()

        vm.intent.onUpdateMonthlyGoal(1200.0)

        coVerify { monthlyGoalRepository.setGoal(1200.0) }
        verify { analyticsTracker.trackEvent(AnalyticsEvents.MONTHLY_GOAL_SET, capture(params)) }
        assertEquals(true, params.captured[AnalyticsParams.HAS_GOAL])
        assertEquals("500-1500", params.captured[AnalyticsParams.GOAL_BUCKET])
        assertFalse(params.captured.values.any { it == 1200.0 || it == "1200" || it == "1200.0" })
    }

    @Test
    fun `an unchanged monthly goal is neither saved nor reported again`() = runTest {
        goalFlow.value = 800.0
        val vm = newViewModel()

        vm.intent.onUpdateMonthlyGoal(800.0)

        coVerify(exactly = 0) { monthlyGoalRepository.setGoal(any()) }
        verify(exactly = 0) { analyticsTracker.trackEvent(AnalyticsEvents.MONTHLY_GOAL_SET, any()) }
    }

    @Test
    fun `a zero or empty goal removes the goal`() = runTest {
        goalFlow.value = 800.0
        val vm = newViewModel()
        val params = slot<Map<String, Any>>()

        vm.intent.onUpdateMonthlyGoal(0.0)

        coVerify { monthlyGoalRepository.setGoal(null) }
        verify { analyticsTracker.trackEvent(AnalyticsEvents.MONTHLY_GOAL_SET, capture(params)) }
        assertEquals(false, params.captured[AnalyticsParams.HAS_GOAL])
        assertTrue(AnalyticsParams.GOAL_BUCKET !in params.captured)
    }
}
