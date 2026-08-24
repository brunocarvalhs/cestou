package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.GetSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.UpdateNotificationSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.viewmodel.NotificationSettingsViewModel
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
class NotificationSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getSettingsUseCase = mockk<GetSettingsUseCase>()
    private val updateNotificationSettingsUseCase = mockk<UpdateNotificationSettingsUseCase>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getSettingsUseCase() } returns flowOf(AppSettings(notificationsEnabled = true, reminderTime = "09:00"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads notification settings from the repository`() {
        val vm = NotificationSettingsViewModel(getSettingsUseCase, updateNotificationSettingsUseCase)

        assertEquals(true, vm.uiState.value.notificationsEnabled)
        assertEquals("09:00", vm.uiState.value.reminderTime)
    }

    @Test
    fun `onUpdateNotificationSettings forwards to the use case`() = runTest {
        coEvery { updateNotificationSettingsUseCase(false, "20:00") } returns Unit
        val vm = NotificationSettingsViewModel(getSettingsUseCase, updateNotificationSettingsUseCase)

        vm.intent.onUpdateNotificationSettings(false, "20:00")

        coVerify { updateNotificationSettingsUseCase(false, "20:00") }
    }

    @Test
    fun `onBack navigates back`() {
        val vm = NotificationSettingsViewModel(getSettingsUseCase, updateNotificationSettingsUseCase)
        vm.setNavigator(navigator)

        vm.intent.onBack()

        verify { navigator.goBack() }
    }
}
