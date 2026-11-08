package br.com.brunocarvalhs.howmuch.core.remoteconfig.model

/**
 * Nomes das flags conhecidas no Firebase Remote Config, centralizados para evitar
 * strings soltas espalhadas pelos módulos que consultam [br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService].
 */
object FeatureFlagKeys {
    const val AI_OPENROUTER_ENABLED = "ai_openrouter_enabled"
    const val AI_GEMINI_ENABLED = "ai_gemini_enabled"

    // Login providers. Google and e-mail default to true at the call site (the Play reviewer must
    // always be able to sign in); every other provider defaults to false until it is switched on.
    const val AUTH_GOOGLE_ENABLED = "auth_google_enabled"
    const val AUTH_EMAIL_ENABLED = "auth_email_enabled"
    const val AUTH_PHONE_ENABLED = "auth_phone_enabled"
    const val AUTH_APPLE_ENABLED = "auth_apple_enabled"
    const val AUTH_MICROSOFT_ENABLED = "auth_microsoft_enabled"
    const val AUTH_FACEBOOK_ENABLED = "auth_facebook_enabled"
    const val AUTH_GITHUB_ENABLED = "auth_github_enabled"

    /** Pairing (and so signing in) a watch from the phone. Default on. */
    const val WEAR_LOGIN_ENABLED = "wear_login_enabled"

    /** Kill-switch do evento `ui_action` (cliques); os demais eventos de analytics seguem. Default on. */
    const val ANALYTICS_UI_ACTIONS_ENABLED = "feat_analytics_ui_actions"

    /** Kill-switch de todas as pesquisas de satisfação e do polegar do chat (sdd-034). Default on. */
    const val FEEDBACK_ENABLED = "feat_feedback_enabled"

    /**
     * Kill-switch do cartão da meta mensal, da linha de orçamento da lista e dos avisos de gasto
     * (sdd-036). Default on.
     */
    const val BUDGET_ALERTS_ENABLED = "feat_budget_alerts_enabled"

    /** Chips de sugestão e ações de adicionar itens no chat da IA (sdd-040). Default on. */
    const val PROACTIVE_AI_ENABLED = "feat_proactive_ai"

    /** Barra inferior do carrinho com "Total planejado" e "No carrinho" em tempo real (sdd-042). Default on. */
    const val REALTIME_CART_ENABLED = "feat_realtime_cart"

    /** Permite pedir a avaliação na Play Store (In-App Review). Default on. */
    const val FEEDBACK_STORE_REVIEW_ENABLED = "feedback_store_review_enabled"
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

    /**
     * The assistant's system prompt (behavior guidelines, scope, guardrails). Remote so it can
     * be tightened or corrected — e.g. a missed guardrail case — without waiting on a store
     * rollout; [br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.SystemPrompts] is the
     * compiled-in fallback for when Remote Config hasn't been fetched yet.
     */
    const val AI_SYSTEM_PROMPT = "ai_system_prompt"

    /** Dias mínimos entre dois NPS (default 90) e entre dois CSAT do mesmo recurso (default 30). */
    const val FEEDBACK_NPS_COOLDOWN_DAYS = "feedback_nps_cooldown_days"
    const val FEEDBACK_CSAT_COOLDOWN_DAYS = "feedback_csat_cooldown_days"
}
