package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatModelsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `ChatRequest round-trips through serialization with tools and messages`() {
        val request = ChatRequest(
            model = "gpt-4",
            messages = listOf(
                Message(role = "system", content = "be nice"),
                Message(role = "user", content = "hi", toolCallId = "call-1")
            ),
            tools = listOf(
                Tool(
                    function = FunctionDeclaration(
                        name = "add_product",
                        description = "adds a product",
                        parameters = JsonObject(mapOf("name" to JsonPrimitive("string")))
                    )
                )
            ),
            temperature = 0.5f
        )

        val encoded = json.encodeToString(ChatRequest.serializer(), request)
        val decoded = json.decodeFromString(ChatRequest.serializer(), encoded)

        assertEquals(request, decoded)
    }

    @Test
    fun `ChatResponse round-trips through serialization with tool calls`() {
        val response = ChatResponse(
            choices = listOf(
                Choice(
                    message = Message(
                        role = "assistant",
                        content = null,
                        toolCalls = listOf(
                            ToolCall(
                                id = "call-1",
                                type = "function",
                                function = FunctionCall(name = "add_product", arguments = "{}")
                            )
                        )
                    )
                )
            )
        )

        val encoded = json.encodeToString(ChatResponse.serializer(), response)
        val decoded = json.decodeFromString(ChatResponse.serializer(), encoded)

        assertEquals(response, decoded)
        assertEquals("call-1", decoded.choices.first().message.toolCalls?.first()?.id)
    }

    @Test
    fun `Tool defaults type to function`() {
        val tool = Tool(function = FunctionDeclaration("x", "y", JsonObject(emptyMap())))

        assertEquals("function", tool.type)
    }

    @Test
    fun `Message content and tool fields default to null`() {
        val message = Message(role = "user")

        assertEquals(null, message.content)
        assertEquals(null, message.toolCalls)
        assertEquals(null, message.toolCallId)
    }
}
