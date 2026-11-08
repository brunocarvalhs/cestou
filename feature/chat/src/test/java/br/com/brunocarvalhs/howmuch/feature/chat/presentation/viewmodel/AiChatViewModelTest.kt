package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.model.AiHistoryTurn
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsElements
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.analytics.service.FirstTimeEventTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackMoment
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.FeedbackPromptRequester
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.data.service.ChatTurnTrackerImpl
import br.com.brunocarvalhs.howmuch.feature.chat.domain.repository.ChatHistoryRepository
import br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase.CartAssistantUseCase
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper.SpeechToTextManager
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.AiTurnInfo
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.MessageVote
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.ReasonChip
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.state.Vote
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
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
    private val aiSession = mockk<AiSession>(relaxed = true) {
        every { activeAction } returns MutableStateFlow(null)
        every { pendingApproval } returns MutableStateFlow(null)
    }
    private val feedbackPromptRequester = mockk<FeedbackPromptRequester>(relaxed = true) {
        every { isFeedbackEnabled() } returns true
    }
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
            feedbackPromptRequester,
            context,
            CoroutineScope(SupervisorJob() + testDispatcher),
            ChatTurnTrackerImpl(),
            mockk(relaxed = true)
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
    fun `onSuggestionClick sends the chip text, keeps the draft and tracks the click`() = runTest {
        coEvery { assistantUseCase("Frutas da estação", any()) } returns flowOf("- Manga")
        viewModel.intent.onInputChange("draft")

        viewModel.intent.onSuggestionClick("Frutas da estação")

        assertEquals("Frutas da estação", viewModel.uiState.value.messages.first().text)
        assertEquals("draft", viewModel.uiState.value.input)
        verify {
            analyticsTracker.trackEvent(AnalyticsEvents.AI_SUGGESTION_CLICKED, mapOf(AnalyticsParams.SOURCE to "chip"))
        }
    }

    @Test
    fun `onAddItems with several items sends one request and tracks the batch`() = runTest {
        every { context.getString(any(), *anyVararg()) } answers {
            "add: " + args.drop(1).flatMap { a -> if (a is Array<*>) a.toList() else listOf(a) }.joinToString()
        }
        coEvery { assistantUseCase(any(), any()) } returns flowOf("done")

        viewModel.intent.onAddItems(listOf("Picanha", "Carvão"))

        coVerify { assistantUseCase(match { it.contains("Picanha, Carvão") }, any()) }
        verify {
            analyticsTracker.trackEvent(AnalyticsEvents.AI_BATCH_ITEMS_ADDED, mapOf(AnalyticsParams.ITEMS_COUNT to 2))
        }
    }

    @Test
    fun `onAddItems with one item tracks a suggestion click, not a batch`() = runTest {
        every { context.getString(any(), *anyVararg()) } answers {
            "add: " + args.drop(1).flatMap { a -> if (a is Array<*>) a.toList() else listOf(a) }.joinToString()
        }
        coEvery { assistantUseCase(any(), any()) } returns flowOf("done")

        viewModel.intent.onAddItems(listOf("Picanha"))

        verify {
            analyticsTracker.trackEvent(AnalyticsEvents.AI_SUGGESTION_CLICKED, mapOf(AnalyticsParams.SOURCE to "item"))
        }
        verify(exactly = 0) { analyticsTracker.trackEvent(AnalyticsEvents.AI_BATCH_ITEMS_ADDED, any()) }
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
    fun `setShoppingContext opens a fresh chat and ignores the old history of that list`() {
        coEvery { chatHistoryRepository.load("list1") } returns listOf(
            ChatMessage(text = "previous message", sender = ChatMessage.Sender.USER)
        )

        viewModel.setShoppingContext("list1")

        assertTrue(viewModel.uiState.value.messages.isEmpty())
        assertEquals(null, viewModel.uiState.value.sessionId)
        coVerify(exactly = 0) { chatHistoryRepository.load(any()) }
    }

    @Test
    fun `the first message of a chat from a list goes to a new session linked to that list`() = runTest {
        viewModel.setShoppingContext("list1")
        viewModel.intent.onInputChange("hello")
        coEvery { assistantUseCase("hello", any()) } returns flowOf("hi there")

        viewModel.intent.onSendMessage()

        val sessionId = viewModel.uiState.value.sessionId!!
        coVerify { chatHistoryRepository.saveSession(sessionId, viewModel.uiState.value.messages, "list1", null) }
        coVerify(exactly = 0) { chatHistoryRepository.save(any(), any()) }
    }

    @Test
    fun `loadSession shows the saved messages and gives them back to the AI memory`() {
        val saved = listOf(
            ChatMessage(text = "add milk", sender = ChatMessage.Sender.USER),
            ChatMessage(text = "done", sender = ChatMessage.Sender.ASSISTANT)
        )
        coEvery { chatHistoryRepository.loadSession("s1") } returns saved

        viewModel.loadSession("s1")

        assertEquals(saved, viewModel.uiState.value.messages)
        verify {
            aiSession.restoreHistory(listOf(AiHistoryTurn(true, "add milk"), AiHistoryTurn(false, "done")))
        }
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

    private fun sendAndGetAssistantId(provider: String = "gemini", toolCalls: Int = 2): Long {
        every { aiSession.providerUsed } returns provider
        every { aiSession.toolCallsThisTurn } returns toolCalls
        coEvery { assistantUseCase("hello", any()) } returns flowOf("hi there")
        viewModel.intent.onInputChange("hello")
        viewModel.intent.onSendMessage()
        return viewModel.uiState.value.messages.last().id
    }

    @Test
    fun `a response records its turn and shows the feedback bar`() {
        val id = sendAndGetAssistantId()

        assertEquals(AiTurnInfo("gemini", 2), viewModel.uiState.value.turns[id])
        assertTrue(viewModel.uiState.value.feedbackEnabled)
    }

    @Test
    fun `thumbs up and down report provider and tool calls without any text`() {
        val id = sendAndGetAssistantId()

        viewModel.intent.onMessageVote(id, Vote.UP)

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AI_MESSAGE_FEEDBACK,
                mapOf(
                    AnalyticsParams.VALUE to "up",
                    AnalyticsParams.PROVIDER to "gemini",
                    AnalyticsParams.TOOL_CALLS to 2
                )
            )
        }
        assertEquals(MessageVote(Vote.UP), viewModel.uiState.value.votes[id])
    }

    @Test
    fun `thumbs down then a reason chip adds the reason`() {
        val id = sendAndGetAssistantId()

        viewModel.intent.onMessageVote(id, Vote.DOWN)
        viewModel.intent.onMessageVoteReason(id, ReasonChip.TOO_SLOW)

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AI_MESSAGE_FEEDBACK,
                mapOf(
                    AnalyticsParams.VALUE to "down",
                    AnalyticsParams.PROVIDER to "gemini",
                    AnalyticsParams.TOOL_CALLS to 2,
                    AnalyticsParams.REASON to "too_slow"
                )
            )
        }
        assertEquals(MessageVote(Vote.DOWN, ReasonChip.TOO_SLOW), viewModel.uiState.value.votes[id])
    }

    @Test
    fun `a second vote on the same message is ignored`() {
        val id = sendAndGetAssistantId()

        viewModel.intent.onMessageVote(id, Vote.UP)
        viewModel.intent.onMessageVote(id, Vote.DOWN)

        verify(exactly = 1) { analyticsTracker.trackEvent(AnalyticsEvents.AI_MESSAGE_FEEDBACK, any()) }
        assertEquals(Vote.UP, viewModel.uiState.value.votes[id]?.value)
    }

    @Test
    fun `a reason is ignored after thumbs up`() {
        val id = sendAndGetAssistantId()

        viewModel.intent.onMessageVote(id, Vote.UP)
        viewModel.intent.onMessageVoteReason(id, ReasonChip.OTHER)

        assertEquals(null, viewModel.uiState.value.votes[id]?.reason)
    }

    @Test
    fun `voting on a message with an unknown turn reports none and zero`() {
        every { feedbackPromptRequester.isFeedbackEnabled() } returns true
        val id = sendAndGetAssistantId()
        val unknown = id + 1
        viewModel.intent.onMessageVote(unknown, Vote.UP)

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AI_MESSAGE_FEEDBACK,
                mapOf(
                    AnalyticsParams.VALUE to "up",
                    AnalyticsParams.PROVIDER to "none",
                    AnalyticsParams.TOOL_CALLS to 0
                )
            )
        }
    }

    @Test
    fun `the feedback bar and votes are off when feedback is disabled`() {
        every { feedbackPromptRequester.isFeedbackEnabled() } returns false
        val id = sendAndGetAssistantId()

        viewModel.intent.onMessageVote(id, Vote.UP)

        assertEquals(false, viewModel.uiState.value.feedbackEnabled)
        verify(exactly = 0) { analyticsTracker.trackEvent(AnalyticsEvents.AI_MESSAGE_FEEDBACK, any()) }
    }

    private fun clearViewModel() = viewModel.intent.onClose()

    @Test
    fun `closing the chat after two answers requests the CSAT moment`() {
        sendAndGetAssistantId()
        viewModel.intent.onInputChange("hello")
        viewModel.intent.onSendMessage()

        clearViewModel()

        verify { feedbackPromptRequester.onMoment(FeedbackMoment.AI_CHAT_CLOSED) }
    }

    @Test
    fun `closing the chat after a single answer does not ask`() {
        sendAndGetAssistantId()

        clearViewModel()

        verify(exactly = 0) { feedbackPromptRequester.onMoment(any()) }
    }

    @Test
    fun `the running action shows up in the state without becoming a message`() = runTest {
        val active = MutableStateFlow<String?>(null)
        every { aiSession.activeAction } returns active
        val vm = AiChatViewModel(
            assistantUseCase, chatHistoryRepository, analyticsTracker, aiTrialRepository, aiSession,
            speechToTextManager, firstTimeEventTracker, feedbackPromptRequester, context,
            CoroutineScope(SupervisorJob() + testDispatcher), ChatTurnTrackerImpl(),
            mockk(relaxed = true)
        )

        active.value = "create_shopping_list"

        assertEquals("create_shopping_list", vm.uiState.value.activeAction)
        assertTrue(vm.uiState.value.messages.isEmpty())
    }
}
