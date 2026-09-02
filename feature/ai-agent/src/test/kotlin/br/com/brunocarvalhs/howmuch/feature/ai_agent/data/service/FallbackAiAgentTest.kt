package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FallbackAiAgentTest {

    private val primary = mockk<AiAgent>()
    private val secondary = mockk<AiAgent>()
    private val context = mockk<AiAgentContext>()
    private val agent = FallbackAiAgent(primary, secondary)

    @Test
    fun `sendMessage emits from primary when it succeeds`() = runTest {
        coEvery { primary.sendMessage("hi", context) } returns flow { emit("from primary") }

        val result = agent.sendMessage("hi", context).toList()

        assertEquals(listOf("from primary"), result)
    }

    @Test
    fun `sendMessage falls back to secondary when primary throws`() = runTest {
        coEvery { primary.sendMessage("hi", context) } returns flow { throw RuntimeException("boom") }
        coEvery { secondary.sendMessage("hi", context) } returns flow { emit("from secondary") }

        val result = agent.sendMessage("hi", context).toList()

        assertEquals(listOf("from secondary"), result)
    }
}
