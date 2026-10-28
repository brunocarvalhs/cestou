package br.com.brunocarvalhs.howmuch.core.ai.contract

import com.google.ai.client.generativeai.type.Content

/**
 * Interface marcadora para a sessão do Agente de IA.
 * Implementações concretas residem no módulo de orquestração (:feature:ai-agent).
 */
interface AiSession {
    val userId: String?
    val history: MutableList<Content>

    /**
     * Limpa [history] sem expor o tipo [Content] (do SDK do Gemini, dependência `implementation`
     * de `:core:ai`) a módulos que só precisam encerrar a sessão — ex. `AiChatViewModel` ao trocar
     * de lista e `MainViewModel` no logout (sdd-006, B-11) — e que por isso não têm esse SDK no
     * classpath de compilação.
     */
    fun clearHistory()
}

