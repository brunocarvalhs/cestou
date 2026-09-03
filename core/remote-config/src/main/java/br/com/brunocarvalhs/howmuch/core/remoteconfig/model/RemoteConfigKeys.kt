package br.com.brunocarvalhs.howmuch.core.remoteconfig.model

/**
 * Nomes das flags conhecidas no Firebase Remote Config, centralizados para evitar
 * strings soltas espalhadas pelos módulos que consultam [br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService].
 */
object FeatureFlagKeys {
    const val AI_OPENROUTER_ENABLED = "ai_openrouter_enabled"
    const val AI_GEMINI_ENABLED = "ai_gemini_enabled"
}

/**
 * Nomes das variáveis remotas conhecidas no Firebase Remote Config, centralizados para evitar
 * strings soltas espalhadas pelos módulos que consultam [br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService].
 */
object RemoteVariableKeys {
    const val OPEN_ROUTER_API_KEY = "open_router_api_key"
    const val OPEN_ROUTER_MODEL = "open_router_model"
    const val GEMINI_API_KEY = "gemini_api_key"
    const val GEMINI_AGENT_MODEL = "gemini_agent_model"
}
