package br.com.brunocarvalhs.howmuch.core.ai.model

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WriteConfirmationTest {

    private class StubAction(
        override val id: String,
        override val requiresConfirmation: Boolean = false
    ) : AgentAction<Any?> {
        override val description: String = "stub"

        override suspend fun execute(
            arguments: Map<String, Any?>,
            session: AiSession,
            metadata: Map<String, Any?>
        ): Result<Any?> = Result.success(null)
    }

    private val confirmingRequest = mapOf(REQUIRE_WRITE_CONFIRMATION_KEY to true)

    @Test
    fun `a confirming request asks approval for a write action`() {
        assertTrue(requiresApproval(StubAction("create_shopping_list"), confirmingRequest))
    }

    @Test
    fun `a confirming request does not ask approval for a read action`() {
        assertFalse(requiresApproval(StubAction("get_all_shopping_lists"), confirmingRequest))
    }

    @Test
    fun `a request without the key keeps the action's own flag for a write action`() {
        assertFalse(requiresApproval(StubAction("create_shopping_list"), emptyMap()))
    }

    @Test
    fun `an action that already requires confirmation asks approval in any request`() {
        assertTrue(requiresApproval(StubAction("delete_shopping_list", requiresConfirmation = true), emptyMap()))
    }

    @Test
    fun `a key that is not the boolean true does not count`() {
        val notBoolean = mapOf(REQUIRE_WRITE_CONFIRMATION_KEY to "true")
        assertFalse(requiresApproval(StubAction("create_shopping_list"), notBoolean))
    }
}
