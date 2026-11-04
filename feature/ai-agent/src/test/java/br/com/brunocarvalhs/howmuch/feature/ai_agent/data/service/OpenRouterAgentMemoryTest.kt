package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentContext
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity.AgentSession
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Duas mensagens seguidas no mesmo chat, passando pelo corpo HTTP que o OpenRouter realmente recebe. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OpenRouterAgentMemoryTest {

    private val requestBodies = mutableListOf<String>()
    private val replies = ArrayDeque<String>()
    private val action = FakeAgentAction(
        id = "create_shopping_list",
        result = Result.success("lista id-123 criada")
    )
    private val registry = mockk<AgentRegistry> {
        every { getAll() } returns listOf(action)
        every { find("create_shopping_list") } returns action
    }
    private val session = AgentSession()
    private val context = mockk<AgentContext> { every { toMetadata() } returns emptyMap() }

    private val agent = OpenRouterAgent(
        dependencies = AgentDependencies(
            session = session,
            crashReporter = mockk(relaxed = true),
            subscriptionRepository = mockk(relaxed = true) {
                every { observeStatus() } returns flowOf(SubscriptionStatus.PRO)
            },
            registry = registry,
            analyticsTracker = mockk(relaxed = true),
            systemPrompt = "system"
        ),
        httpClientFactory = object : OpenRouterHttpClientFactory {
            override fun create(json: Json) = HttpClient(
                MockEngine { request ->
                    requestBodies += (request.body as TextContent).text
                    respond(
                        content = replies.removeFirst(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            ) { install(ContentNegotiation) { json(json) } }
        },
        model = "m",
        apiKey = "k"
    )

    private fun text(content: String) = """{"choices":[{"message":{"role":"assistant","content":"$content"}}]}"""

    private val toolCall = """{"choices":[{"message":{"role":"assistant","content":null,"tool_calls":[{"id":"c1",""" +
        """"type":"function","function":{"name":"create_shopping_list",""" +
        """"arguments":"{\"title\":\"Feira\"}"}}]}}]}"""

    @Test
    fun `the second message of the same chat carries the first one and what the action did`() = runTest {
        replies += toolCall
        replies += text("Lista Feira criada!")
        replies += text("Pronto.")

        agent.sendMessage("cria a lista Feira", context).toList()
        agent.sendMessage("e agora?", context).toList()

        val secondRequest = requestBodies.last()
        assertTrue(secondRequest.contains("cria a lista Feira"))
        assertTrue(secondRequest.contains("Lista Feira criada!"))
        assertTrue(secondRequest.contains("e agora?"))
        // A ação volta no formato nativo de ferramentas, não como texto solto.
        assertTrue(secondRequest.contains("\"tool_calls\""))
        assertTrue(secondRequest.contains("\"role\":\"tool\""))
        assertTrue(secondRequest.contains("id-123"))
        assertFalse(secondRequest.contains("Registro interno"))
    }

    @Test
    fun `a fresh session starts without the memory of another chat`() = runTest {
        replies += text("Oi!")
        replies += text("Oi de novo!")

        agent.sendMessage("primeira conversa", context).toList()
        session.clearHistory()
        agent.sendMessage("outra conversa", context).toList()

        assertFalse(requestBodies.last().contains("primeira conversa"))
    }

    @Test
    fun `the answer shown on screen is only the final text`() = runTest {
        replies += toolCall
        replies += text("Lista Feira criada!")

        val shown = agent.sendMessage("cria a lista Feira", context).toList()

        assertEquals(listOf("Lista Feira criada!"), shown)
    }
}
