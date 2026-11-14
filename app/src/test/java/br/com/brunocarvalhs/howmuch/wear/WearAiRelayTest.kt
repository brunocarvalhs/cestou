package br.com.brunocarvalhs.howmuch.wear

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalDecision
import br.com.brunocarvalhs.howmuch.core.ai.model.ApprovalRequest
import br.com.brunocarvalhs.howmuch.core.ai.model.REQUIRE_WRITE_CONFIRMATION_KEY
import br.com.brunocarvalhs.howmuch.core.common.wearable.AiFailure
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearAiMessage
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableAiLink
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.exception.AiProviderUnavailableException
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.orchestrator.AgentOrchestrator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test

/** Lado do celular da entrada rápida de IA do relógio. */
class WearAiRelayTest {

    private val orchestrator = mockk<AgentOrchestrator>(relaxed = true)
    private val session = mockk<AiSession>(relaxed = true)
    private val sync = mockk<WearableAiLink>(relaxed = true)
    private val pending = MutableStateFlow<ApprovalRequest?>(null)

    private lateinit var relay: WearAiRelay

    @Before
    fun setUp() {
        every { session.userId } returns "user-1"
        every { session.pendingApproval } returns pending
        coEvery { sync.sendAiMessage(any(), any()) } returns true
        relay = WearAiRelay(orchestrator, session, sync)
    }

    @Test
    fun `sends the agent answer back to the watch that asked`() {
        coEvery { orchestrator.chat(any(), any()) } returns flowOf("Leite", " adicionado")

        relay.answer("node-1", "req-1", "adicionar leite")

        coVerify(timeout = WAIT_MS) {
            sync.sendAiMessage("node-1", WearAiMessage.Reply("req-1", "Leite adicionado"))
        }
    }

    @Test
    fun `the watch request asks for approval of write actions`() {
        coEvery { orchestrator.chat(any(), any()) } returns flowOf("ok")

        relay.answer("node-1", "req-1", "adicionar leite")

        coVerify(timeout = WAIT_MS) {
            orchestrator.chat(
                "adicionar leite",
                match { it.toMetadata()[REQUIRE_WRITE_CONFIRMATION_KEY] == true }
            )
        }
    }

    @Test
    fun `starts each watch prompt from an empty history`() {
        coEvery { orchestrator.chat(any(), any()) } returns flowOf("ok")

        relay.answer("node-1", "req-1", "adicionar leite")

        coVerify(timeout = WAIT_MS) { orchestrator.chat("adicionar leite", any()) }
        verify(timeout = WAIT_MS) { orchestrator.clearHistory() }
    }

    @Test
    fun `reports no provider when the phone has no AI available`() {
        coEvery { orchestrator.chat(any(), any()) } throws AiProviderUnavailableException()

        relay.answer("node-1", "req-1", "adicionar leite")

        coVerify(timeout = WAIT_MS) {
            sync.sendAiMessage("node-1", WearAiMessage.Failure("req-1", AiFailure.NO_PROVIDER))
        }
    }

    @Test
    fun `reports a generic error when the agent fails`() {
        coEvery { orchestrator.chat(any(), any()) } throws IllegalStateException("boom")

        relay.answer("node-1", "req-1", "adicionar leite")

        coVerify(timeout = WAIT_MS) {
            sync.sendAiMessage("node-1", WearAiMessage.Failure("req-1", AiFailure.ERROR))
        }
    }

    @Test
    fun `does not answer when the phone has no signed-in user`() {
        every { session.userId } returns null

        relay.answer("node-1", "req-1", "adicionar leite")

        coVerify(timeout = WAIT_MS) {
            sync.sendAiMessage("node-1", WearAiMessage.Failure("req-1", AiFailure.ERROR))
        }
        coVerify(exactly = 0) { orchestrator.chat(any(), any()) }
    }

    @Test
    fun `resolves the approval the watch answered`() {
        pending.value = ApprovalRequest(actionId = "action-1", summary = "Lista de teste")

        relay.confirm(actionId = "action-1", approved = true)

        verify { session.resolveApproval(ApprovalDecision.APPROVED) }
    }

    @Test
    fun `refused approval resolves as rejected`() {
        pending.value = ApprovalRequest(actionId = "action-1", summary = "Lista de teste")

        relay.confirm(actionId = "action-1", approved = false)

        verify { session.resolveApproval(ApprovalDecision.REJECTED) }
    }

    @Test
    fun `ignores an approval for an action that is not pending`() {
        pending.value = ApprovalRequest(actionId = "action-1", summary = "Lista de teste")

        relay.confirm(actionId = "stale-action", approved = true)

        verify(exactly = 0) { session.resolveApproval(any()) }
    }

    private companion object {
        const val WAIT_MS = 2_000L
    }
}
