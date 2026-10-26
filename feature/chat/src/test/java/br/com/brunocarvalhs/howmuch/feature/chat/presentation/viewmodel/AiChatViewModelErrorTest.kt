package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.feature.chat.domain.repository.ChatHistoryRepository
import br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase.CartAssistantUseCase
import io.mockk.coEvery
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// Split out of AiChatViewModelTest to keep TooManyFunctions under the detekt threshold —
// same fixture shape, scoped to the failed-response path instead of the happy path.
@OptIn(ExperimentalCoroutinesApi::class)
class AiChatViewModelErrorTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val assistantUseCase = mockk<CartAssistantUseCase>()
    private val chatHistoryRepository = mockk<ChatHistoryRepository>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val aiTrialRepository = mockk<AiTrialRepository>(relaxed = true)
    private val viewModel =
        AiChatViewModel(assistantUseCase, chatHistoryRepository, analyticsTracker, aiTrialRepository)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { chatHistoryRepository.load(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSendMessage surfaces an error and tracks the failure when the assistant throws`() = runTest {
        viewModel.intent.onInputChange("hello")
        coEvery { assistantUseCase("hello", any()) } throws RuntimeException("boom")

        viewModel.intent.onSendMessage()

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AI_CHAT_RESPONSE_FAILED,
                mapOf(AnalyticsParams.REASON to "RuntimeException")
            )
        }
    }

    @Test
    fun `onSendMessage clears a previous error on the next attempt`() = runTest {
        viewModel.intent.onInputChange("hello")
        coEvery { assistantUseCase("hello", any()) } throws RuntimeException("boom")
        viewModel.intent.onSendMessage()
        assertNotNull(viewModel.uiState.value.error)

        viewModel.intent.onInputChange("hello again")
        coEvery { assistantUseCase("hello again", any()) } returns flowOf("hi there")
        viewModel.intent.onSendMessage()

        assertNull(viewModel.uiState.value.error)
    }
}
