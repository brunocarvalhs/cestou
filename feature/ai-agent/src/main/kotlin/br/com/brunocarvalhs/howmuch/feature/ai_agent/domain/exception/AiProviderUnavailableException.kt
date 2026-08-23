package br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.exception

import br.com.brunocarvalhs.howmuch.core.domain.exception.BusinessRuleException

/**
 * Lançada quando todos os providers de IA (Gemini, OpenRouter) estão desligados via
 * feature flag remota, geralmente porque um bug foi identificado em produção.
 */
class AiProviderUnavailableException :
    BusinessRuleException(tag = TAG, message = "Nenhum provider de IA disponível no momento") {

    companion object {
        private const val TAG = "ai_provider_unavailable"
    }
}
