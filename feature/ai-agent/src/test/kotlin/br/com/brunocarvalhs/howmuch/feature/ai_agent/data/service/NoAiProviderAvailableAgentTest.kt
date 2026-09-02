package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.exception.AiProviderUnavailableException
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class NoAiProviderAvailableAgentTest {

    @Test
    fun `sendMessage throws AiProviderUnavailableException when collected`() = runTest {
        val context = mockk<AiAgentContext>()

        val result = runCatching { NoAiProviderAvailableAgent.sendMessage("hi", context).toList() }

        assertTrue(result.exceptionOrNull() is AiProviderUnavailableException)
    }
}
