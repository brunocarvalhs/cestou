package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.GetSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.UpdateShoppingPreferencesUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.viewmodel.ShoppingSettingsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class ShoppingSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getSettingsUseCase = mockk<GetSettingsUseCase>()
    private val updateShoppingPreferencesUseCase = mockk<UpdateShoppingPreferencesUseCase>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getSettingsUseCase() } returns flowOf(
            AppSettings(defaultListId = "list1", sortingMode = "NAME", remindersEnabled = true)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads shopping preferences from the repository`() {
        val vm = ShoppingSettingsViewModel(getSettingsUseCase, updateShoppingPreferencesUseCase)

        assertEquals("list1", vm.uiState.value.defaultListId)
        assertEquals("NAME", vm.uiState.value.sortingMode)
        assertEquals(true, vm.uiState.value.remindersEnabled)
    }

    @Test
    fun `onUpdateShoppingPreferences forwards to the use case`() = runTest {
        coEvery { updateShoppingPreferencesUseCase("list2", "DATE", false) } returns Unit
        val vm = ShoppingSettingsViewModel(getSettingsUseCase, updateShoppingPreferencesUseCase)

        vm.intent.onUpdateShoppingPreferences("list2", "DATE", false)

        coVerify { updateShoppingPreferencesUseCase("list2", "DATE", false) }
    }

    @Test
    fun `onBack navigates back`() {
        val vm = ShoppingSettingsViewModel(getSettingsUseCase, updateShoppingPreferencesUseCase)
        vm.setNavigator(navigator)

        vm.intent.onBack()

        verify { navigator.goBack() }
    }
}
