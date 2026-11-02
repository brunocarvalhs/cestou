package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentContext
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.exception.AiProviderUnavailableException
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class NoProviderAvailableAgentTest {

    @Test
    fun `sendMessage throws AiProviderUnavailableException when collected`() = runTest {
        val context = mockk<AgentContext>()

        val result = runCatching { NoProviderAvailableAgent.sendMessage("hi", context).toList() }

        assertTrue(result.exceptionOrNull() is AiProviderUnavailableException)
    }
}
