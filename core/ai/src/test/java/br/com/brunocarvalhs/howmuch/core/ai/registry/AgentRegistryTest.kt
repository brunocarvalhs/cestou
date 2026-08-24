package br.com.brunocarvalhs.howmuch.core.ai.registry

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentActionProvider
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgentRegistryTest {

    private fun fakeAction(actionId: String) = object : AgentAction<Unit> {
        override val id = actionId
        override val description = "desc-$actionId"
        override suspend fun execute(
            arguments: Map<String, Any?>,
            session: AiSession,
            metadata: Map<String, Any?>
        ): Result<Unit> = Result.success(Unit)
    }

    @After
    fun tearDown() {
        AgentRegistry.clear()
    }

    @Test
    fun `register makes the action findable by id`() {
        val action = fakeAction("delete_shopping_list")

        AgentRegistry.register(action)

        assertEquals(action, AgentRegistry.find("delete_shopping_list"))
    }

    @Test
    fun `find returns null for an unknown id`() {
        assertNull(AgentRegistry.find("unknown"))
    }

    @Test
    fun `registerProvider converts and registers every provider`() {
        val action = fakeAction("update_shopping_list")
        val provider = mockk<AgentActionProvider>()
        io.mockk.every { provider.toAgentAction() } returns action

        AgentRegistry.registerProvider(provider)

        assertEquals(action, AgentRegistry.find("update_shopping_list"))
    }

    @Test
    fun `getAll returns every registered action`() {
        AgentRegistry.register(fakeAction("a"))
        AgentRegistry.register(fakeAction("b"))

        assertEquals(2, AgentRegistry.getAll().size)
    }

    @Test
    fun `clear removes every registered action`() {
        AgentRegistry.register(fakeAction("a"))

        AgentRegistry.clear()

        assertEquals(0, AgentRegistry.getAll().size)
    }

    @Test
    fun `register overwrites a previous action with the same id`() {
        val first = fakeAction("a")
        val second = fakeAction("a")

        AgentRegistry.register(first)
        AgentRegistry.register(second)

        assertEquals(second, AgentRegistry.find("a"))
        assertEquals(1, AgentRegistry.getAll().size)
    }
}
