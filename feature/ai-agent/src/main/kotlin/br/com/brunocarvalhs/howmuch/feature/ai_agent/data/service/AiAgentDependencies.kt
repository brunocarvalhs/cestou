package br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service

import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.registry.AgentRegistry
import br.com.brunocarvalhs.howmuch.core.common.contract.CrashReporter

/**
 * Bundles the collaborators shared by every [br.com.brunocarvalhs.howmuch.core.ai.contract.AiAgent]
 * implementation (currently [GeminiAiAgent] and [OpenRouterAiAgent]), keeping their own
 * constructors down to just this plus their provider-specific model/apiKey.
 */
internal data class AiAgentDependencies(
    val session: AiSession,
    val crashReporter: CrashReporter,
    val registry: AgentRegistry = AgentRegistry,
    val systemPrompt: String = SystemPrompts.CESTOU_ASSISTANT
)
