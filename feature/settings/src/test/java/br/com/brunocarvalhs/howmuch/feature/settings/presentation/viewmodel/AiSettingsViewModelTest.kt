package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateAiSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel.AiSettingsViewModel
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
class AiSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val settingsRepository = mockk<SettingsRepository>()
    private val updateAiSettingsUseCase = mockk<UpdateAiSettingsUseCase>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads ai settings from the repository`() {
        every { settingsRepository.getSettings() } returns
            flowOf(AppSettings(aiModel = "gpt", customPrompt = "concise"))

        val vm = AiSettingsViewModel(settingsRepository, updateAiSettingsUseCase)

        assertEquals("gpt", vm.uiState.value.aiModel)
        assertEquals("concise", vm.uiState.value.customPrompt)
    }

    @Test
    fun `onUpdateAiSettings forwards to the use case`() = runTest {
        every { settingsRepository.getSettings() } returns flowOf(AppSettings())
        coEvery { updateAiSettingsUseCase("gpt", "concise", 0.5f) } returns Unit
        val vm = AiSettingsViewModel(settingsRepository, updateAiSettingsUseCase)

        vm.intent.onUpdateAiSettings("gpt", "concise", 0.5f)

        coVerify { updateAiSettingsUseCase("gpt", "concise", 0.5f) }
    }

    @Test
    fun `onBack navigates back`() {
        every { settingsRepository.getSettings() } returns flowOf(AppSettings())
        val vm = AiSettingsViewModel(settingsRepository, updateAiSettingsUseCase)
        vm.setNavigator(navigator)

        vm.intent.onBack()

        verify { navigator.goBack() }
    }
}
