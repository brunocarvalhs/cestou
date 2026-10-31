package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.feature.chat.domain.model.ChatSession
import br.com.brunocarvalhs.howmuch.feature.chat.domain.repository.ChatHistoryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiHistoryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val chatHistoryRepository = mockk<ChatHistoryRepository>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private lateinit var viewModel: AiHistoryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { chatHistoryRepository.getSessions() } returns listOf(
            ChatSession(id = "s1", title = "Session 1")
        )
        viewModel = AiHistoryViewModel(chatHistoryRepository, analyticsTracker)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init tracks analytics screen view and loads sessions`() {
        verify { analyticsTracker.trackScreenView("ai_chat_history", "AiHistoryViewModel") }
        assertEquals(1, viewModel.uiState.value.sessions.size)
        assertEquals("s1", viewModel.uiState.value.sessions.first().id)
    }

    @Test
    fun `onSelectSession updates selectedSessionId in uiState`() {
        viewModel.intent.onSelectSession("s1")

        assertEquals("s1", viewModel.uiState.value.selectedSessionId)
    }

    @Test
    fun `onDeleteSession deletes session and reloads`() = runTest {
        viewModel.intent.onDeleteSession("s1")

        coVerify { chatHistoryRepository.deleteSession("s1") }
    }

    @Test
    fun `onNewChat creates new session and tracks event`() = runTest {
        val newSession = ChatSession(id = "s2", title = "Nova conversa")
        coEvery { chatHistoryRepository.createNewSession(any()) } returns newSession

        viewModel.intent.onNewChat()

        coVerify { chatHistoryRepository.createNewSession() }
        verify { analyticsTracker.trackEvent("ai_chat_session_created") }
        assertEquals("s2", viewModel.uiState.value.selectedSessionId)
    }
}
