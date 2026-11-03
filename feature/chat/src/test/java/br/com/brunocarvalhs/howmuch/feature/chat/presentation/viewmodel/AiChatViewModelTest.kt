package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsElements
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.analytics.service.FirstTimeEventTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.domain.repository.ChatHistoryRepository
import br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase.CartAssistantUseCase
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper.SpeechToTextManager
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AiChatViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val assistantUseCase = mockk<CartAssistantUseCase>()
    private val chatHistoryRepository = mockk<ChatHistoryRepository>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val aiTrialRepository = mockk<AiTrialRepository>(relaxed = true)
    private val firstTimeEventTracker = mockk<FirstTimeEventTracker>(relaxed = true)
    private val aiSession = mockk<AiSession>(relaxed = true)
    private val speechToTextManager = mockk<SpeechToTextManager>(relaxed = true) {
        every { isListening } returns MutableStateFlow(false)
        every { recognizedText } returns MutableStateFlow(null)
        every { errorMessage } returns MutableStateFlow(null)
    }
    private val context = mockk<Context> {
        every { getString(R.string.ai_chat_error_network) } returns "network error"
        every { getString(R.string.ai_chat_error_generic) } returns "generic error"
        every { getString(R.string.ai_chat_voice_not_supported) } returns "voice not supported"
    }
    private lateinit var viewModel: AiChatViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { chatHistoryRepository.load(any()) } returns emptyList()
        viewModel = AiChatViewModel(
            assistantUseCase,
            chatHistoryRepository,
            analyticsTracker,
            aiTrialRepository,
            aiSession,
            speechToTextManager,
            firstTimeEventTracker,
            context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init leaves the screen_view to ScreenTracker`() {
        verify(exactly = 0) { analyticsTracker.trackScreenView(any(), any()) }
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
        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AI_CHAT_MESSAGE_SENT,
                mapOf("input_method" to "text", "length_bucket" to "1-20", "enabled" to false)
            )
        }
    }

    @Test
    fun `onSelectHistoryItem tracks the click`() = runTest {
        viewModel.intent.onSelectHistoryItem("s1")

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.UI_ACTION,
                mapOf(AnalyticsParams.ELEMENT_ID to AnalyticsElements.AI_CHAT_SELECT_HISTORY)
            )
        }
    }

    @Test
    fun `onSendMessage marks the free trial as used`() = runTest {
        viewModel.intent.onInputChange("hello")
        coEvery { assistantUseCase("hello", any()) } returns flowOf("hi there")

        viewModel.intent.onSendMessage()

        coVerify { aiTrialRepository.markFreeMessageUsed() }
    }

    @Test
    fun `setShoppingContext stores the shopping id`() {
        viewModel.setShoppingContext("list1")

        assertEquals("list1", viewModel.uiState.value.shoppingId)
    }

    @Test
    fun `setShoppingContext loads persisted history for that shopping id`() {
        val persisted = listOf(
            ChatMessage(
                text = "previous message",
                sender = ChatMessage.Sender.USER
            )
        )
        coEvery { chatHistoryRepository.load("list1") } returns persisted

        viewModel.setShoppingContext("list1")

        assertEquals(persisted, viewModel.uiState.value.messages)
    }

    @Test
    fun `onSendMessage persists the updated history for the current shopping id`() = runTest {
        viewModel.setShoppingContext("list1")
        viewModel.intent.onInputChange("hello")
        coEvery { assistantUseCase("hello", any()) } returns flowOf("hi there")

        viewModel.intent.onSendMessage()

        coVerify { chatHistoryRepository.save("list1", viewModel.uiState.value.messages) }
    }

    @Test
    fun `startVoiceInput starts listening when available`() {
        every { speechToTextManager.isRecognitionAvailable() } returns true

        viewModel.intent.onStartVoiceInput()

        verify { speechToTextManager.startListening() }
    }

    @Test
    fun `a received answer tracks ai_response_received with latency and size bucket but never the text`() = runTest {
        every { aiSession.providerUsed } returns "openrouter"
        every { aiSession.toolCallsThisTurn } returns 2
        every { aiSession.fallbackUsedThisTurn } returns true
        viewModel.intent.onInputChange("hello")
        coEvery { assistantUseCase("hello", any()) } returns flowOf("hi there")

        viewModel.intent.onSendMessage()

        val params = slot<Map<String, Any?>>()
        verify { analyticsTracker.trackEvent(AnalyticsEvents.AI_RESPONSE_RECEIVED, capture(params)) }
        assertEquals(
            setOf("latency_ms", "length_bucket", "provider", "tool_calls", "fallback_used"),
            params.captured.keys
        )
        assertEquals("1-20", params.captured["length_bucket"])
        assertEquals("openrouter", params.captured["provider"])
        assertEquals(2, params.captured["tool_calls"])
        assertEquals(true, params.captured["fallback_used"])
        assertTrue(params.captured.values.none { it == "hi there" || it == "hello" })
    }

    @Test
    fun `the free trial message is tracked once the answer arrives`() = runTest {
        viewModel.intent.onInputChange("hello")
        coEvery { assistantUseCase("hello", any()) } returns flowOf("hi there")

        viewModel.intent.onSendMessage()

        verify { analyticsTracker.trackEvent(AnalyticsEvents.AI_TRIAL_MESSAGE_USED) }
    }

    @Test
    fun `a network failure tracks ai_response_failed as network`() = runTest {
        viewModel.intent.onInputChange("hello")
        every { context.getString(any()) } returns "error"
        coEvery { assistantUseCase("hello", any()) } throws IOException("offline")

        viewModel.intent.onSendMessage()

        verify { analyticsTracker.trackEvent(AnalyticsEvents.AI_RESPONSE_FAILED, mapOf("error_type" to "network")) }
        verify(exactly = 0) { analyticsTracker.trackEvent(AnalyticsEvents.AI_RESPONSE_RECEIVED, any()) }
    }

    @Test
    fun `any other failure tracks ai_response_failed as generic`() = runTest {
        viewModel.intent.onInputChange("hello")
        every { context.getString(any()) } returns "error"
        coEvery { assistantUseCase("hello", any()) } throws IllegalStateException("boom")

        viewModel.intent.onSendMessage()

        verify { analyticsTracker.trackEvent(AnalyticsEvents.AI_RESPONSE_FAILED, mapOf("error_type" to "generic")) }
    }

    @Test
    fun `voice input unavailable tracks ai_voice_input_failed as unsupported`() {
        every { speechToTextManager.isRecognitionAvailable() } returns false
        every { context.getString(any()) } returns "unsupported"

        viewModel.intent.onStartVoiceInput()

        verify { analyticsTracker.trackEvent(AnalyticsEvents.AI_VOICE_INPUT_FAILED, mapOf("reason" to "unsupported")) }
    }

    @Test
    fun `a message dictated by voice is sent as input_method voice`() = runTest {
        viewModel.intent.onVoiceResultReceived("hello")
        coEvery { assistantUseCase("hello", any()) } returns flowOf("hi there")

        viewModel.intent.onSendMessage()

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AI_CHAT_MESSAGE_SENT,
                match { it["input_method"] == "voice" }
            )
        }
    }
}
