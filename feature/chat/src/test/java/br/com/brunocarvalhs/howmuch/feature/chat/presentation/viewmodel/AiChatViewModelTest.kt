package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiChatViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val assistantUseCase = mockk<CartAssistantUseCase>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val viewModel = AiChatViewModel(assistantUseCase, analyticsTracker)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init tracks an ai_chat screen_view`() {
        verify { analyticsTracker.trackScreenView("ai_chat", "AiChatViewModel") }
    }

    @Test
    fun `onInputChange updates the input text`() {
        viewModel.intent.onInputChange("how much did I spend?")

        assertEquals("how much did I spend?", viewModel.uiState.value.input)
    }

    @Test
    fun `onSendMessage is a no-op for blank input`() = runTest {
        viewModel.intent.onInputChange("   ")

        viewModel.intent.onSendMessage()

        assertEquals(0, viewModel.uiState.value.messages.size)
    }

    @Test
    fun `onSendMessage appends user then assistant messages and tracks the event`() = runTest {
        viewModel.intent.onInputChange("hello")
        coEvery { assistantUseCase("hello", any()) } returns flowOf("hi there")

        viewModel.intent.onSendMessage()

        assertEquals(2, viewModel.uiState.value.messages.size)
        assertEquals("hi there", viewModel.uiState.value.messages.last().text)
        assertEquals(false, viewModel.uiState.value.isLoading)
        verify { analyticsTracker.trackEvent(AnalyticsEvents.AI_CHAT_MESSAGE_SENT) }
    }

    @Test
    fun `setShoppingContext stores the shopping id`() {
        viewModel.setShoppingContext("list1")

        assertEquals("list1", viewModel.uiState.value.shoppingId)
    }
}
