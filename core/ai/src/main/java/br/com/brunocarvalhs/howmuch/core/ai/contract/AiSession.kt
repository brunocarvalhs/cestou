package br.com.brunocarvalhs.howmuch.core.ai.contract

import com.google.ai.client.generativeai.type.Content

/**
 * Interface marcadora para a sessão do Agente de IA.
 * Implementações concretas residem no módulo de orquestração (:feature:ai-agent).
 */
interface AiSession {
    val userId: String?
    val history: MutableList<Content>
}

