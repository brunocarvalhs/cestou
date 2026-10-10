package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeAction(
    override val id: String,
    override val description: String = "",
    override val parameters: List<AiAgentParameter> = emptyList()
) : AgentAction<String> {
    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiSession,
        metadata: Map<String, Any?>
    ): Result<String> = Result.success("ok")
}

class OpenRouterMappingTest {

    // --- openRouterRoleFor ---

    @Test
    fun `openRouterRoleFor maps the Gemini SDK's 'model' role to 'assistant'`() {
        assertEquals("assistant", openRouterRoleFor("model"))
    }

    @Test
    fun `openRouterRoleFor defaults a null role to 'user'`() {
        assertEquals("user", openRouterRoleFor(null))
    }

    @Test
    fun `openRouterRoleFor passes through any other role unchanged`() {
        assertEquals("user", openRouterRoleFor("user"))
        assertEquals("system", openRouterRoleFor("system"))
        assertEquals("tool", openRouterRoleFor("tool"))
    }

    // --- buildOpenRouterTools ---

    @Test
    fun `buildOpenRouterTools wraps parameters as a JSON Schema object, not a bare param map`() {
        val action = FakeAction(
            id = "create_shopping_list",
            parameters = listOf(
                AiAgentParameter(name = "title", type = "string", description = "titulo", isRequired = true)
            )
        )

        val tools = buildOpenRouterTools(listOf(action))

        val parameters = tools.single().function.parameters
        assertEquals("object", parameters.getValue("type").jsonPrimitive.content)
        assertTrue(parameters.getValue("properties") is JsonObject)
    }

    @Test
    fun `buildOpenRouterTools never uses a bare param name as a top-level schema key`() {
        // "title" is a reserved JSON Schema keyword; as a top-level key it used to make
        // providers reject the whole request ("... is not of type 'string'").
        val action = FakeAction(
            id = "create_shopping_list",
            parameters = listOf(
                AiAgentParameter(name = "title", type = "string", description = "titulo", isRequired = true)
            )
        )

        val parameters = buildOpenRouterTools(listOf(action)).single().function.parameters

        assertTrue("title" !in parameters.keys)
        val properties = parameters.getValue("properties").jsonObject
        assertTrue("title" in properties.keys)
    }

    @Test
    fun `buildOpenRouterTools only lists required params in 'required'`() {
        val action = FakeAction(
            id = "save_product_to_cart",
            parameters = listOf(
                AiAgentParameter(name = "name", type = "string", description = "", isRequired = true),
                AiAgentParameter(name = "shoppingId", type = "string", description = "", isRequired = false)
            )
        )

        val parameters = buildOpenRouterTools(listOf(action)).single().function.parameters

        val required = (parameters.getValue("required") as JsonArray).map { it.jsonPrimitive.content }
        assertEquals(listOf("name"), required)
    }

    @Test
    fun `buildOpenRouterTools produces an empty properties object for a no-arg action`() {
        val action = FakeAction(id = "get_all_shopping_lists")

        val parameters = buildOpenRouterTools(listOf(action)).single().function.parameters

        assertEquals(JsonObject(emptyMap()), parameters.getValue("properties"))
        assertEquals(JsonArray(emptyList()), parameters.getValue("required"))
    }

    @Test
    fun `buildOpenRouterTools maps one Tool per registered action, preserving id and description`() {
        val actions = listOf(
            FakeAction(id = "a", description = "desc a"),
            FakeAction(id = "b", description = "desc b")
        )

        val tools = buildOpenRouterTools(actions)

        assertEquals(listOf("a", "b"), tools.map { it.function.name })
        assertEquals(listOf("desc a", "desc b"), tools.map { it.function.description })
    }

    // --- parseFunctionCallArguments ---

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // Regression for the "Serializer for class 'Any' is not found" crash.
    @Test
    fun `parseFunctionCallArguments decodes a JSON object without throwing`() {
        val args = parseFunctionCallArguments(json, """{"title": "Feira da semana", "budget": 150.5}""")

        assertEquals(2, args.size)
    }

    @Test
    fun `parseFunctionCallArguments result works with the existing getString AgentAction helper`() {
        val args = parseFunctionCallArguments(json, """{"title": "Feira da semana"}""")

        assertEquals("Feira da semana", args.getString("title"))
    }

    @Test
    fun `parseFunctionCallArguments handles an empty argument object`() {
        val args = parseFunctionCallArguments(json, "{}")

        assertTrue(args.isEmpty())
    }
}
