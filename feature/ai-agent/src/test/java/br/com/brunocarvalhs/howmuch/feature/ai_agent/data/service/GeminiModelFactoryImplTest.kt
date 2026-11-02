package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentParameter
import com.google.ai.client.generativeai.type.TextPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre a construção do [com.google.ai.client.generativeai.GenerativeModel] extraída de
 * [GeminiAgent] (sdd-026, A-03) — antes inline e não testável sem uma chamada real ao SDK.
 */
class GeminiModelFactoryImplTest {

    private val factory = GeminiModelFactoryImpl()

    private class FakeAction(
        override val id: String,
        override val description: String,
        override val parameters: List<AiAgentParameter>
    ) : AgentAction<String> {
        override suspend fun execute(
            arguments: Map<String, Any?>,
            session: br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession,
            metadata: Map<String, Any?>
        ): Result<String> = Result.success("")
    }

    @Test
    fun `create wires modelName, apiKey and systemPrompt into the GenerativeModel`() {
        val model = factory.create(
            modelName = "gemini-test-model",
            apiKey = "test-api-key",
            systemPrompt = "você é o assistente de testes",
            tools = emptyList()
        )

        // GenerativeModel's public constructor normalizes the name to "models/<name>" (fullModelName).
        assertEquals("models/gemini-test-model", model.modelName)
        assertEquals("test-api-key", model.apiKey)
        val systemText = model.systemInstruction?.parts?.filterIsInstance<TextPart>()?.joinToString { it.text }
        assertEquals("você é o assistente de testes", systemText)
    }

    @Test
    fun `create maps every AiAgentParameter type to the matching Schema`() {
        val action = FakeAction(
            id = "multi_type_action",
            description = "ação com todos os tipos de parâmetro",
            parameters = listOf(
                AiAgentParameter("qty", "integer", "quantidade", isRequired = true),
                AiAgentParameter("price", "number", "preço", isRequired = true),
                AiAgentParameter("done", "boolean", "concluído", isRequired = false),
                AiAgentParameter("name", "string", "nome", isRequired = true)
            )
        )

        val model = factory.create(
            modelName = "gemini-test-model",
            apiKey = "test-api-key",
            systemPrompt = "system",
            tools = listOf(action)
        )

        val declaration = model.tools?.single()?.functionDeclarations?.single()
        assertEquals("multi_type_action", declaration?.name)
        assertEquals(setOf("qty", "price", "done", "name"), declaration?.parameters?.map { it.name }?.toSet())
        assertEquals(listOf("qty", "price", "name"), declaration?.requiredParameters)
        assertTrue(declaration?.parameters?.all { it.name.isNotBlank() } == true)
    }
}
