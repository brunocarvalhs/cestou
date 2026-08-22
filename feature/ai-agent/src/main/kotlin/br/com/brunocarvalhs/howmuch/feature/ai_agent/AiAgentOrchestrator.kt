package br.com.brunocarvalhs.howmuch.feature.ai_agent

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentContext
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgentFactory
import br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity.AiAgentSession
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.GetSettingsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orquestrador principal do Agente de IA.
 * Gerencia a sessão, o histórico e a escolha do provedor de IA.
 * Esta classe é o "cérebro" da integração com IA no projeto.
 */
@Singleton
class AiAgentOrchestrator @Inject constructor(
    private val factory: AiAgentFactory,
    private val getSettingsUseCase: GetSettingsUseCase,
    val session: AiAgentSession
) {
    /**
     * Envia uma mensagem para o assistente e recebe o fluxo de resposta.
     * O histórico é mantido automaticamente na sessão.
     */
    suspend fun chat(prompt: String, context: AiAgentContext): Flow<String> {
        val settings = getSettingsUseCase().first()
        val agent = factory.create(settings)
        return agent.sendMessage(prompt, context)
    }

    /**
     * Limpa o histórico de conversa da sessão atual.
     */
    fun clearHistory() {
        session.history.clear()
    }
}
