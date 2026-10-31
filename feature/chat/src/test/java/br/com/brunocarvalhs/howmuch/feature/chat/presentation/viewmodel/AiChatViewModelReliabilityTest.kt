package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
import br.com.brunocarvalhs.howmuch.feature.chat.R
import br.com.brunocarvalhs.howmuch.feature.chat.domain.repository.ChatHistoryRepository
import br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase.CartAssistantUseCase
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper.SpeechToTextManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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

/**
 * sdd-006 (B-08, B-11): error surfacing and AiSession history isolation across shopping lists.
 * Split out of [AiChatViewModelTest] to stay under Detekt's TooManyFunctions threshold.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiChatViewModelReliabilityTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val assistantUseCase = mockk<CartAssistantUseCase>()
    private val chatHistoryRepository = mockk<ChatHistoryRepository>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val aiTrialRepository = mockk<AiTrialRepository>(relaxed = true)
    private val speechToTextManager = mockk<SpeechToTextManager>(relaxed = true) {
        every { isListening } returns MutableStateFlow(false)
        every { recognizedText } returns MutableStateFlow(null)
        every { errorMessage } returns MutableStateFlow(null)
    }

    private val aiSession = mockk<AiSession>(relaxed = true)
    private val context = mockk<Context> {
        every { getString(R.string.ai_chat_error_network) } returns "network error"
        every { getString(R.string.ai_chat_error_generic) } returns "generic error"
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
            context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setShoppingContext clears the shared AiSession history when the shopping id changes`() {
        viewModel.setShoppingContext("list1")

        verify(exactly = 1) { aiSession.clearHistory() }
    }

    @Test
    fun `setShoppingContext does not re-clear history when the shopping id does not change`() {
        viewModel.setShoppingContext("list1")
        viewModel.setShoppingContext("list1")

        verify(exactly = 1) { aiSession.clearHistory() }
    }

    @Test
    fun `onSendMessage shows a network error bubble when the whole agent chain fails with IOException`() = runTest {
        viewModel.intent.onInputChange("hello")
        coEvery { assistantUseCase("hello", any()) } returns flow { throw IOException("offline") }

        viewModel.intent.onSendMessage()

        assertEquals("network error", viewModel.uiState.value.messages.last().text)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onSendMessage shows a generic error bubble when the whole agent chain fails for another reason`() = runTest {
        viewModel.intent.onInputChange("hello")
        coEvery { assistantUseCase("hello", any()) } returns flow { throw IllegalStateException("boom") }

        viewModel.intent.onSendMessage()

        assertEquals("generic error", viewModel.uiState.value.messages.last().text)
    }

    @Test
    fun `onSendMessage persists the error bubble like any other assistant message`() = runTest {
        viewModel.setShoppingContext("list1")
        viewModel.intent.onInputChange("hello")
        coEvery { assistantUseCase("hello", any()) } returns flow { throw IOException("offline") }

        viewModel.intent.onSendMessage()

        coVerify { chatHistoryRepository.save("list1", viewModel.uiState.value.messages) }
        assertTrue(viewModel.uiState.value.messages.last().text.isNotBlank())
    }
}
