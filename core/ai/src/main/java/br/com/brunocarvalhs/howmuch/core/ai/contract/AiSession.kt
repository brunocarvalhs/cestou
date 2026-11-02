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

    /**
     * `true` quando uma tool com efeito colateral real (ex. produto gravado no Firestore) já foi
     * despachada na tentativa de `sendMessage` em curso. [FallbackAgent] lê este sinal antes de
     * tentar o provider secundário: sem ele, uma falha de transporte *depois* da tool já ter
     * rodado faria o fallback reexecutar o pedido inteiro no outro provider, duplicando a tool
     * (sdd-012). Cada agente concreto zera este sinal no início do próprio `sendMessage`.
     */
    var toolExecutedThisTurn: Boolean
}

