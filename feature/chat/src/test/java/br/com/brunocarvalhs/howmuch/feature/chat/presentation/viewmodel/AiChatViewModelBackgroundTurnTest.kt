package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.data.service.ChatTurnTrackerImpl
import br.com.brunocarvalhs.howmuch.feature.chat.domain.repository.ChatHistoryRepository
import br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase.CartAssistantUseCase
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper.SpeechToTextManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * sdd-035: o turno da IA roda fora do `viewModelScope`; fechar o chat não cancela a chamada nem
 * perde a resposta, e um chat reaberto no meio do turno não corrompe o `AiSession` singleton.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiChatViewModelBackgroundTurnTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val assistantUseCase = mockk<CartAssistantUseCase>()
    private val chatHistoryRepository = mockk<ChatHistoryRepository>(relaxed = true)
    private val aiTrialRepository = mockk<AiTrialRepository>(relaxed = true)
    private val aiSession = mockk<AiSession>(relaxed = true) {
        every { activeAction } returns MutableStateFlow(null)
    }
    private val tracker = ChatTurnTrackerImpl()
    private val speechToTextManager = mockk<SpeechToTextManager>(relaxed = true) {
        every { isListening } returns MutableStateFlow(false)
        every { recognizedText } returns MutableStateFlow(null)
        every { errorMessage } returns MutableStateFlow(null)
    }
    private val context = mockk<Context> {
        every { getString(R.string.ai_chat_error_network) } returns "network error"
        every { getString(R.string.ai_chat_error_generic) } returns "generic error"
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { chatHistoryRepository.load(any()) } returns emptyList()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel() = AiChatViewModel(
        assistantUseCase,
        chatHistoryRepository,
        mockk<AnalyticsTracker>(relaxed = true),
        aiTrialRepository,
        aiSession,
        speechToTextManager,
        mockk(relaxed = true),
        mockk(relaxed = true),
        context,
        CoroutineScope(SupervisorJob() + testDispatcher),
        tracker
    )

    private fun AiChatViewModel.send(text: String) {
        intent.onInputChange(text)
        intent.onSendMessage()
    }

    @Test
    fun `a late answer is saved after the chat screen is closed`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { assistantUseCase("hi", any()) } returns flow {
            gate.await()
            emit("answer")
        }
        val viewModel = newViewModel()
        viewModel.setShoppingContext("list1")

        viewModel.send("hi")
        viewModel.viewModelScope.cancel()
        gate.complete(Unit)

        coVerify { chatHistoryRepository.saveSession(any(), match { it.last().text == "answer" }, "list1", null) }
        assertEquals("answer", viewModel.uiState.value.messages.last().text)
    }

    @Test
    fun `a failure after the chat screen is closed saves the error bubble`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { assistantUseCase("hi", any()) } returns flow {
            gate.await()
            throw IOException("offline")
        }
        val viewModel = newViewModel()
        viewModel.setShoppingContext("list1")

        viewModel.send("hi")
        viewModel.viewModelScope.cancel()
        gate.complete(Unit)

        coVerify {
            chatHistoryRepository.saveSession(
                any(),
                match { it.last().text == "network error" },
                "list1",
                null
            )
        }
        assertTrue(tracker.inFlight.value.isEmpty())
    }

    @Test
    fun `the trial credit is spent after a late answer, never without one`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { assistantUseCase("hi", any()) } returns flow {
            gate.await()
            emit("answer")
        }
        coEvery { assistantUseCase("empty", any()) } returns emptyFlow()
        val viewModel = newViewModel()
        viewModel.setShoppingContext("list1")

        viewModel.send("hi")
        viewModel.viewModelScope.cancel()
        gate.complete(Unit)
        coVerify(exactly = 1) { aiTrialRepository.markFreeMessageUsed() }

        val other = newViewModel()
        other.setShoppingContext("list2")
        other.send("empty")
        coVerify(exactly = 1) { aiTrialRepository.markFreeMessageUsed() }
    }

    @Test
    fun `the turn is tracked while the answer is pending and released when it ends`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { assistantUseCase("hi", any()) } returns flow {
            gate.await()
            emit("answer")
        }
        val viewModel = newViewModel()
        viewModel.setShoppingContext("list1")

        viewModel.send("hi")
        assertEquals(setOf(viewModel.uiState.value.sessionId), tracker.inFlight.value)

        gate.complete(Unit)
        assertTrue(tracker.inFlight.value.isEmpty())
    }

    @Test
    fun `a chat without list or session gets a session so the late answer has somewhere to go`() = runTest {
        coEvery { assistantUseCase("hi", any()) } returns flowOf("answer")
        val viewModel = newViewModel()

        viewModel.send("hi")

        val sessionId = viewModel.uiState.value.sessionId
        assertNotNull(sessionId)
        coVerify {
            chatHistoryRepository.saveSession(sessionId!!, match { it.last().text == "answer" }, null, null)
        }
    }

    @Test
    fun `reopening a chat with a pending turn keeps the AiSession and blocks a second send`() = runTest {
        tracker.begin("list1", Job())
        val viewModel = newViewModel()

        viewModel.setShoppingContext("list1")
        viewModel.send("again")

        verify(exactly = 0) { aiSession.clearHistory() }
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.messages.none { it.text == "again" })
    }

    @Test
    fun `a reopened session reloads the history when the pending turn ends`() = runTest {
        val saved = listOf(
            ChatMessage(text = "hi", sender = ChatMessage.Sender.USER),
            ChatMessage(text = "answer", sender = ChatMessage.Sender.ASSISTANT)
        )
        tracker.begin("s1", Job())
        val viewModel = newViewModel()
        viewModel.loadSession("s1")

        coEvery { chatHistoryRepository.loadSession("s1") } returns saved
        tracker.end("s1")

        assertEquals(saved, viewModel.uiState.value.messages)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loading a session with a pending turn also waits for it`() = runTest {
        tracker.begin("s1", Job())
        val viewModel = newViewModel()

        viewModel.loadSession("s1")

        verify(exactly = 0) { aiSession.clearHistory() }
        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `cancelling all turns stops the call and does not save an answer`() = runTest {
        val gate = CompletableDeferred<Unit>()
        coEvery { assistantUseCase("hi", any()) } returns flow {
            gate.await()
            emit("answer")
        }
        val viewModel = newViewModel()
        viewModel.setShoppingContext("list1")
        viewModel.send("hi")

        tracker.cancelAll()
        gate.complete(Unit)

        assertTrue(tracker.inFlight.value.isEmpty())
        coVerify(
            exactly = 0
        ) { chatHistoryRepository.saveSession(any(), match { it.last().text == "answer" }, any(), any()) }
        assertNull(viewModel.uiState.value.messages.firstOrNull { it.text == "answer" })
    }
}
