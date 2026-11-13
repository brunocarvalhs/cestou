package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.common.wearable.AiFailure
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearAiMessage
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableAiLink
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearAiIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.WearAiUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearAiViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val sync = mockk<WearableAiLink>(relaxed = true)
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val messages = MutableSharedFlow<WearAiMessage>(extraBufferCapacity = 16)

    /** The request id the ViewModel generated, captured when the prompt is sent. */
    private var requestId = ""

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { sync.observeAiMessages(any()) } returns messages
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun phoneReachable(reachable: Boolean = true) {
        coEvery { sync.sendAiPrompt(any(), any()) } answers {
            requestId = firstArg()
            reachable
        }
    }

    private fun viewModel() = WearAiViewModel(sync, analytics)

    @Test
    fun `sends the prompt and shows the answer the phone sends back`() {
        phoneReachable()
        val vm = viewModel()

        vm.onIntent(WearAiIntent.Send("adicionar leite"))
        coVerify { sync.sendAiPrompt(requestId, "adicionar leite") }
        messages.tryEmit(WearAiMessage.Reply(requestId, "Leite adicionado"))

        assertEquals(WearAiUiState.Answer("Leite adicionado"), vm.uiState.value)
        verify { analytics.trackEvent(AnalyticsEvents.WEAR_AI_PROMPT_SENT, any()) }
    }

    @Test
    fun `reset goes back to the start after an answer`() {
        phoneReachable()
        val vm = viewModel()
        vm.onIntent(WearAiIntent.Send("adicionar leite"))
        messages.tryEmit(WearAiMessage.Reply(requestId, "Leite adicionado"))

        vm.onIntent(WearAiIntent.Reset)

        assertEquals(WearAiUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `does not send a blank prompt`() {
        phoneReachable()
        val vm = viewModel()

        vm.onIntent(WearAiIntent.Send("   "))

        coVerify(exactly = 0) { sync.sendAiPrompt(any(), any()) }
        assertEquals(WearAiUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `does not send a prompt above the length limit`() {
        phoneReachable()
        val vm = viewModel()

        vm.onIntent(WearAiIntent.Send("x".repeat(WEAR_AI_MAX_PROMPT_LENGTH + 1)))

        coVerify(exactly = 0) { sync.sendAiPrompt(any(), any()) }
    }

    @Test
    fun `shows phone unavailable when the prompt cannot be sent`() {
        phoneReachable(reachable = false)
        val vm = viewModel()

        vm.onIntent(WearAiIntent.Send("adicionar leite"))

        assertEquals(WearAiUiState.Failed(WearAiUiState.PHONE_UNAVAILABLE), vm.uiState.value)
    }

    @Test
    fun `shows the failure the phone reports`() {
        phoneReachable()
        val vm = viewModel()

        vm.onIntent(WearAiIntent.Send("adicionar leite"))
        messages.tryEmit(WearAiMessage.Failure(requestId, AiFailure.NO_PROVIDER))

        assertEquals(WearAiUiState.Failed(AiFailure.NO_PROVIDER), vm.uiState.value)
    }

    @Test
    fun `asks for confirmation before running a write action`() {
        phoneReachable()
        coEvery { sync.sendAiConfirm(any(), any(), any()) } returns true
        val vm = viewModel()

        vm.onIntent(WearAiIntent.Send("apagar a lista de teste"))
        messages.tryEmit(WearAiMessage.ConfirmRequest(requestId, "action-1", "Lista de teste"))

        assertEquals(
            WearAiUiState.Confirming(requestId, "action-1", "Lista de teste"),
            vm.uiState.value
        )
        coVerify(exactly = 0) { sync.sendAiConfirm(any(), any(), any()) }
    }

    @Test
    fun `sends the approval and waits for the final answer`() {
        phoneReachable()
        coEvery { sync.sendAiConfirm(any(), any(), any()) } returns true
        val vm = viewModel()
        vm.onIntent(WearAiIntent.Send("apagar a lista de teste"))
        messages.tryEmit(WearAiMessage.ConfirmRequest(requestId, "action-1", "Lista de teste"))

        vm.onIntent(WearAiIntent.Decide(approved = true))

        coVerify { sync.sendAiConfirm(requestId, "action-1", true) }
        assertEquals(WearAiUiState.Waiting, vm.uiState.value)
        messages.tryEmit(WearAiMessage.Reply(requestId, "Lista apagada"))
        assertEquals(WearAiUiState.Answer("Lista apagada"), vm.uiState.value)
        verify { analytics.trackEvent(AnalyticsEvents.WEAR_AI_ACTION_CONFIRMED, any()) }
    }

    @Test
    fun `a refused action is sent as refused`() {
        phoneReachable()
        coEvery { sync.sendAiConfirm(any(), any(), any()) } returns true
        val vm = viewModel()
        vm.onIntent(WearAiIntent.Send("apagar a lista de teste"))
        messages.tryEmit(WearAiMessage.ConfirmRequest(requestId, "action-1", "Lista de teste"))

        vm.onIntent(WearAiIntent.Decide(approved = false))

        coVerify { sync.sendAiConfirm(requestId, "action-1", false) }
    }

    @Test
    fun `decide does nothing when no action is waiting`() {
        phoneReachable()
        val vm = viewModel()

        vm.onIntent(WearAiIntent.Decide(approved = true))

        coVerify(exactly = 0) { sync.sendAiConfirm(any(), any(), any()) }
        assertEquals(WearAiUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `does not send a second prompt while one is waiting`() {
        phoneReachable()
        val vm = viewModel()

        vm.onIntent(WearAiIntent.Send("adicionar leite"))
        vm.onIntent(WearAiIntent.Send("adicionar pão"))

        coVerify(exactly = 1) { sync.sendAiPrompt(any(), any()) }
    }

    @Test
    fun `fails with timeout when the phone never answers`() {
        phoneReachable()
        val vm = viewModel()

        vm.onIntent(WearAiIntent.Send("adicionar leite"))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(WearAiUiState.Failed(WearAiUiState.TIMEOUT), vm.uiState.value)
    }
}
