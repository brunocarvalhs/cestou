package br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.contract.Agent
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentContext
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentFactory
import br.com.brunocarvalhs.howmuch.core.domain.model.AiProvider
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CartAssistantUseCaseTest {

    private val agentFactory = mockk<AgentFactory>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val useCase = CartAssistantUseCase(agentFactory, settingsRepository)

    @Test
    fun `invoke creates the agent from current settings and forwards the prompt`() = runTest {
        val settings = AppSettings(aiProvider = AiProvider.GEMINI)
        val context = mockk<AgentContext>()
        val agent = mockk<Agent>()
        every { settingsRepository.getSettings() } returns flowOf(settings)
        every { agentFactory.create(settings) } returns agent
        coEvery { agent.sendMessage("hello", context) } returns flowOf("hi there")

        val response = useCase("hello", context)

        assertEquals("hi there", response.first())
    }
}
