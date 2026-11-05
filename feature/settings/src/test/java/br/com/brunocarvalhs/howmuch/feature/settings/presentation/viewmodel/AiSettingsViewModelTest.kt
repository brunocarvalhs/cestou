package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiModelRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateAiSettingsUseCase
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

private const val TEST_TEMPERATURE = 0.5f

@OptIn(ExperimentalCoroutinesApi::class)
class AiSettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val settingsRepository = mockk<SettingsRepository>()
    private val aiModelRepository = mockk<AiModelRepository>().apply {
        coEvery { getAvailableModels() } returns emptyList()
    }
    private val subscriptionRepository = mockk<SubscriptionRepository>().apply {
        every { observeStatus() } returns flowOf(SubscriptionStatus.FREE)
    }
    private val updateAiSettingsUseCase = mockk<UpdateAiSettingsUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AiSettingsViewModel(
        settingsRepository = settingsRepository,
        aiModelRepository = aiModelRepository,
        subscriptionRepository = subscriptionRepository,
        updateAiSettingsUseCase = updateAiSettingsUseCase,
        analyticsTracker = analyticsTracker
    )

    @Test
    fun `init loads ai settings from the repository`() {
        every { settingsRepository.getSettings() } returns
            flowOf(AppSettings(aiModel = "gpt"))

        val vm = createViewModel()

        assertEquals("gpt", vm.uiState.value.aiModel)
    }

    @Test
    fun `onUpdateAiSettings forwards to the use case`() = runTest {
        every { settingsRepository.getSettings() } returns flowOf(AppSettings())
        coEvery {
            updateAiSettingsUseCase(
                AiConfigTier.DEFAULT,
                "gpt",
                TEST_TEMPERATURE,
                null
            )
        } returns Unit
        val vm = createViewModel()

        vm.intent.onUpdateAiSettings(
            AiConfigTier.DEFAULT,
            "gpt",
            TEST_TEMPERATURE,
            null
        )

        coVerify {
            updateAiSettingsUseCase(
                AiConfigTier.DEFAULT,
                "gpt",
                TEST_TEMPERATURE,
                null
            )
        }
    }

    @Test
    fun `onBack navigates back`() {
        every { settingsRepository.getSettings() } returns flowOf(AppSettings())
        val vm = createViewModel()
        vm.setNavigator(navigator)

        vm.intent.onBack()

        verify { navigator.goBack() }
    }
}
