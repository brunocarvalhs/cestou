package br.com.brunocarvalhs.howmuch.core.ai.contract

import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings

/**
 * Factory para criação de instâncias de Agent baseadas nas configurações do usuário.
 */
interface AgentFactory {
    fun create(settings: AppSettings): Agent
}
