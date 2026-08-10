package br.com.brunocarvalhs.howmuch.core.ai.contract

import br.com.brunocarvalhs.howmuch.core.domain.entity.AppSettings

/**
 * Factory para criação de instâncias de AiAgent baseadas nas configurações do usuário.
 */
interface AiAgentFactory {
    fun create(settings: AppSettings): AiAgent
}
