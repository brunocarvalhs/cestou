package br.com.brunocarvalhs.howmuch.feature.ai_agent.domain.entity

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import com.google.ai.client.generativeai.type.Content
import java.util.Locale

/**
 * Dados de sessão que persistem durante as interações com a IA.
 * Esta implementação é específica para o Google Gemini.
 */
data class AiAgentSession(
    override val userId: String? = null,
    val locale: Locale = Locale.getDefault(),
    override val history: MutableList<Content> = mutableListOf()
) : AiSession
