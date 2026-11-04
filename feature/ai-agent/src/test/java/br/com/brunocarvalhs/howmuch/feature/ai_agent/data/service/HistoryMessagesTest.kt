package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.TextPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoryMessagesTest {

    private fun text(role: String, value: String) = Content(role = role, parts = listOf(TextPart(value)))

    @Test
    fun `a tool round becomes tool_calls followed by the matching tool message`() {
        val history = listOf(text("user", "cria"))
            .plus(toolRoundContents(listOf("create_shopping_list" to mapOf("title" to "Feira")), listOf("OK id-1")))
            .plus(text("model", "feito"))

        val messages = history.toOpenRouterMessages()

        assertEquals(listOf("user", "assistant", "tool", "assistant"), messages.map { it.role })
        val call = messages[1].toolCalls!!.single()
        assertEquals("create_shopping_list", call.function.name)
        assertEquals("""{"title":"Feira"}""", call.function.arguments)
        assertEquals(call.id, messages[2].toolCallId)
        assertEquals("OK id-1", messages[2].content)
        assertNull(messages[1].content)
    }

    @Test
    fun `a tool call without its response is dropped so the provider does not reject the request`() {
        val orphan = toolRoundContents(listOf("a" to emptyMap()), listOf("ok")).first()

        val messages = listOf(text("user", "oi"), orphan, text("user", "e agora?")).toOpenRouterMessages()

        assertEquals(listOf("user", "user"), messages.map { it.role })
    }

    @Test
    fun `plain text turns keep their roles`() {
        val messages = listOf(text("user", "oi"), text("model", "olá")).toOpenRouterMessages()

        assertEquals(listOf("user", "assistant"), messages.map { it.role })
    }
}
