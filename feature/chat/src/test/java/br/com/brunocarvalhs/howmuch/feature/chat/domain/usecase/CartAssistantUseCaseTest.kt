package br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentFactory
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.GetSettingsUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CartAssistantUseCaseTest {

    private val agentFactory = mockk<AiAgentFactory>()
    private val getSettingsUseCase = mockk<GetSettingsUseCase>()
    private val useCase = CartAssistantUseCase(agentFactory, getSettingsUseCase)

    @Test
    fun `invoke creates the agent from current settings and forwards the prompt`() = runTest {
        val settings = AppSettings(aiProvider = "gemini")
        val context = mockk<AiAgentContext>()
        val agent = mockk<AiAgent>()
        every { getSettingsUseCase() } returns flowOf(settings)
        every { agentFactory.create(settings) } returns agent
        coEvery { agent.sendMessage("hello", context) } returns flowOf("hi there")

        val response = useCase("hello", context)

        assertEquals("hi there", response.first())
    }
}
