package br.com.brunocarvalhs.howmuch.core.ai.registry

import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentParameter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgentRegistryTest {

    /** Dublê de [AgentActionUseCase] sem depender das anotações `@AiAgentAction`/`@AiAgentParameter`
     * (sdd-027) — [AgentRegistry] só precisa de `id`, não da extração de metadados. */
    private class FakeActionUseCase(override val id: String) : AgentActionUseCase<Unit>() {
        override val description: String = "desc-$id"
        override val parameters: List<AiAgentParameter> = emptyList()
        override suspend fun execute(
            arguments: Map<String, Any?>,
            session: AiSession,
            metadata: Map<String, Any?>
        ): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun `find returns the action with a matching id from the injected set`() {
        val registry = AgentRegistry(actions = setOf(FakeActionUseCase("delete_shopping_list")))

        assertEquals("delete_shopping_list", registry.find("delete_shopping_list")?.id)
    }

    @Test
    fun `find returns null for an id not present in the injected set`() {
        val registry = AgentRegistry(actions = setOf(FakeActionUseCase("a")))

        assertNull(registry.find("unknown"))
    }

    @Test
    fun `getAll returns every action from the injected set`() {
        val registry = AgentRegistry(actions = setOf(FakeActionUseCase("a"), FakeActionUseCase("b")))

        assertEquals(2, registry.getAll().size)
    }

    @Test
    fun `getAll returns an empty list when the injected set is empty`() {
        val registry = AgentRegistry(actions = emptySet())

        assertEquals(0, registry.getAll().size)
    }

    @Test
    fun `two actions with the same id in the set keep only the last one`() {
        val first = FakeActionUseCase("a")
        val second = FakeActionUseCase("a")

        val registry = AgentRegistry(actions = linkedSetOf(first, second))

        assertEquals(1, registry.getAll().size)
        assertEquals(second, registry.find("a"))
    }
}
