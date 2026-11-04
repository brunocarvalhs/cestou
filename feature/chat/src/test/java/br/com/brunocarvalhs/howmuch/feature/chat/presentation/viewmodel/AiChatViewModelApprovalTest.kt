package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalDecision
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalRequest
import br.com.brunocarvalhs.howmuch.feature.chat.data.service.ChatTurnTrackerImpl
import br.com.brunocarvalhs.howmuch.feature.chat.domain.service.AiTurnBackground
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper.SpeechToTextManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** sdd-037: o card de aprovação no estado do chat e as decisões do usuário. */
@OptIn(ExperimentalCoroutinesApi::class)
class AiChatViewModelApprovalTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val pending = MutableStateFlow<ApprovalRequest?>(null)
    private val aiSession = mockk<AiSession>(relaxed = true) {
        every { activeAction } returns MutableStateFlow(null)
        every { pendingApproval } returns pending
    }
    private val speechToTextManager = mockk<SpeechToTextManager>(relaxed = true) {
        every { isListening } returns MutableStateFlow(false)
        every { recognizedText } returns MutableStateFlow(null)
        every { errorMessage } returns MutableStateFlow(null)
    }
    private val request = ApprovalRequest(actionId = "delete_shopping_list", summary = "Mercado")
    private val background = mockk<AiTurnBackground>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = AiChatViewModel(
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk(relaxed = true),
        aiSession,
        speechToTextManager,
        mockk(relaxed = true),
        mockk(relaxed = true),
        mockk<Context>(relaxed = true),
        CoroutineScope(SupervisorJob() + testDispatcher),
        ChatTurnTrackerImpl(),
        background
    )

    @Test
    fun `an open approval request becomes the card in the state`() {
        val vm = viewModel()

        pending.value = request

        assertEquals(request, vm.uiState.value.pendingApproval)
    }

    @Test
    fun `the card goes away when the request is resolved`() {
        val vm = viewModel()
        pending.value = request

        pending.value = null

        assertNull(vm.uiState.value.pendingApproval)
    }

    @Test
    fun `approving hands the approval to the agent`() {
        viewModel().intent.onApprove()

        verify(exactly = 1) { aiSession.resolveApproval(ApprovalDecision.APPROVED) }
    }

    @Test
    fun `rejecting hands the rejection to the agent`() {
        viewModel().intent.onReject()

        verify(exactly = 1) { aiSession.resolveApproval(ApprovalDecision.REJECTED) }
    }

    @Test
    fun `leaving the chat with the card open dismisses the request`() {
        val vm = viewModel()
        pending.value = request

        vm.dismissPendingApproval()

        verify(exactly = 1) { aiSession.resolveApproval(ApprovalDecision.DISMISSED) }
    }

    @Test
    fun `leaving the chat without a card does not touch the approval`() {
        viewModel().dismissPendingApproval()

        verify(exactly = 0) { aiSession.resolveApproval(any()) }
    }

    @Test
    fun `an approval request out of sight asks the background to notify`() {
        val vm = viewModel()
        vm.setChatVisible(false)

        pending.value = request

        verify(exactly = 1) { background.approvalNeeded(any()) }
    }

    @Test
    fun `no request means no approval notification`() {
        viewModel()

        verify(exactly = 0) { background.approvalNeeded(any()) }
    }

    @Test
    fun `the chat visibility is forwarded to the background`() {
        val vm = viewModel()

        vm.setChatVisible(true)
        vm.setChatVisible(false)

        verify(exactly = 1) { background.setChatVisible(true) }
        verify(exactly = 1) { background.setChatVisible(false) }
    }
}
