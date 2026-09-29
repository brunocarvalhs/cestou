package br.com.brunocarvalhs.howmuch.feature.chat.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentFactory
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * UseCase responsável por mediar a comunicação entre a UI do carrinho e o Agente de IA.
 * Ele mantém a instância do serviço de IA para preservar o histórico da conversa durante a sessão.
 */
class CartAssistantUseCase @Inject constructor(
    private val agentFactory: AiAgentFactory,
    private val settingsRepository: SettingsRepository
) {
    /**
     * Envia uma mensagem para o assistente e recebe o fluxo de resposta.
     */
    suspend operator fun invoke(
        prompt: String,
        context: AiAgentContext
    ): Flow<String> {
        val settings = settingsRepository.getSettings().first()
        val agentService = agentFactory.create(settings)

        return agentService.sendMessage(prompt, context)
    }
}
