package br.com.brunocarvalhs.howmuch.core.remoteconfig.model

/**
 * Nomes das flags conhecidas no Firebase Remote Config, centralizados para evitar
 * strings soltas espalhadas pelos módulos que consultam [br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService].
 *
 * **Organização:** por funcionalidade semântica (não por SDD).
 * **Granularidade:** MICRO — cada caso de uso específico tem seu próprio toggle para desligamento
 * independente em caso de bug.
 */
object FeatureFlagKeys {

    // ============================================================================
    // CATEGORY 1: AI Assistant & LLM Integration
    // ============================================================================

    // --- AI Providers (mutually exclusive or combinable) ---
    /** Habilita OpenRouter como provedor de LLM (modelos via proxy). Default on. */
    const val AI_OPENROUTER_ENABLED = "ai_openrouter_enabled"

    /** Habilita Gemini Vision API como provedor LLM + OCR. Default on. */
    const val AI_GEMINI_ENABLED = "ai_gemini_enabled"

    // --- Proactive AI Features (depende de pelo menos um provider) ---
    /** Chips de sugestão e ações de adicionar itens no chat. Default on. */
    const val PROACTIVE_AI_SUGGESTIONS_ENABLED = "feat_proactive_ai_suggestions"

    /** Ações rápidas (FABs) de AI no contexto de lista. Default on. */
    const val PROACTIVE_AI_ACTIONS_ENABLED = "feat_proactive_ai_actions"

    /** Legacy alias for all proactive AI features. Maps to suggestions. */
    const val PROACTIVE_AI_ENABLED = PROACTIVE_AI_SUGGESTIONS_ENABLED

    /** Entrada rápida de IA no relógio. Default off: o relógio só mostra a entrada com a flag ligada. */
    const val WEAR_AI_QUICK_ENTRY = "wear_ai_quick_entry"

    /**
     * Paywall com todos os planos (mensal, semestral e anual). Um toggle só para todos. Default off:
     * sem a flag, o paywall mostra só o mensal, como antes. Permite publicar o código antes de os
     * planos estarem ativos no Play Console.
     */
    const val PAYWALL_MULTI_PLANS_ENABLED = "paywall_multi_plans_enabled"

    // ============================================================================
    // CATEGORY 2: Authentication & Identity
    // ============================================================================

    // --- Login Providers (Google and E-mail default to true at call site for Play reviewer) ---
    /** Google Sign-In via Credential Manager. Default on. */
    const val AUTH_GOOGLE_ENABLED = "auth_google_enabled"

    /** E-mail/Password authentication. Default on. */
    const val AUTH_EMAIL_ENABLED = "auth_email_enabled"

    /** Phone number authentication. Default off. */
    const val AUTH_PHONE_ENABLED = "auth_phone_enabled"

    /** Apple Sign-In. Default off. */
    const val AUTH_APPLE_ENABLED = "auth_apple_enabled"

    /** Microsoft account authentication. Default off. */
    const val AUTH_MICROSOFT_ENABLED = "auth_microsoft_enabled"

    /** Facebook authentication. Default off. */
    const val AUTH_FACEBOOK_ENABLED = "auth_facebook_enabled"

    /** GitHub authentication. Default off. */
    const val AUTH_GITHUB_ENABLED = "auth_github_enabled"

    // --- Platform-Specific Auth ---
    /** Pairing (and signing in) a Wear OS watch from the phone. Default on. */
    const val WEAR_LOGIN_ENABLED = "wear_login_enabled"

    // ============================================================================
    // CATEGORY 3: Shopping List Management & Automation (AppFunctions)
    // ============================================================================

    // --- Base AppFunctions (CRUD) ---
    /** Base AppFunction: create, read, list shopping lists. Default on. */
    const val APP_FUNCTIONS_ENABLED = "feat_app_functions"

    /** Alias for APP_FUNCTIONS_ENABLED (legacy compatibility). */
    const val APP_FUNCTIONS_BASE_ENABLED = APP_FUNCTIONS_ENABLED

    // --- Extended AppFunctions (Mutations) ---
    /** Mark product as purchased. Default on. */
    const val APP_FUNCTIONS_MARK_PURCHASED_ENABLED = "feat_app_functions_mark_purchased"

    /** Remove product from list. Default on. */
    const val APP_FUNCTIONS_REMOVE_PRODUCT_ENABLED = "feat_app_functions_remove_product"

    /** Update shopping list budget. Default on. */
    const val APP_FUNCTIONS_UPDATE_BUDGET_ENABLED = "feat_app_functions_update_budget"

    /** Get all products in list. Default on. */
    const val APP_FUNCTIONS_GET_PRODUCTS_ENABLED = "feat_app_functions_get_products"

    // --- Tier 1 AppFunctions (Search & Bulk) ---
    /** Search products by name or category. Default on. */
    const val APP_FUNCTIONS_SEARCH_PRODUCT_ENABLED = "feat_app_functions_search_product"

    /** Add multiple products at once (CSV format). Default on. */
    const val APP_FUNCTIONS_ADD_MULTIPLE_ENABLED = "feat_app_functions_add_multiple"

    /** List all shopping lists with budget info. Default on. */
    const val APP_FUNCTIONS_LIST_ALL_ENABLED = "feat_app_functions_list_all"

    /** Get complete shopping list details (budget, spent, items, members). Default on. */
    const val APP_FUNCTIONS_GET_DETAILS_ENABLED = "feat_app_functions_get_details"

    /** Clear all purchased items from list. Default on. */
    const val APP_FUNCTIONS_CLEAR_PURCHASED_ENABLED = "feat_app_functions_clear_purchased"

    // --- Tier 2 AppFunctions (Analytics & Advanced) ---
    /** Duplicate shopping list with optional custom title. Default on. */
    const val APP_FUNCTIONS_DUPLICATE_LIST_ENABLED = "feat_app_functions_duplicate_list"

    /** Add recipe ingredients to list. Default on. */
    const val APP_FUNCTIONS_RECIPE_TO_LIST_ENABLED = "feat_app_functions_recipe_to_list"

    /** Get total spending for current month. Default on. */
    const val APP_FUNCTIONS_MONTHLY_SPENDING_ENABLED = "feat_app_functions_monthly_spending"

    /** Get spending history for last N months. Default on. */
    const val APP_FUNCTIONS_SPENDING_HISTORY_ENABLED = "feat_app_functions_spending_history"

    /** Rename shopping list. Default on. */
    const val APP_FUNCTIONS_RENAME_LIST_ENABLED = "feat_app_functions_rename_list"

    // --- Tier 3 AppFunctions (Advanced Operations) ---
    /** Update product price. Default on. */
    const val APP_FUNCTIONS_UPDATE_PRODUCT_PRICE_ENABLED = "feat_app_functions_update_price"

    /** Duplicate product with new quantity. Default on. */
    const val APP_FUNCTIONS_DUPLICATE_PRODUCT_ENABLED = "feat_app_functions_duplicate_product"

    /** Reopen closed/finished shopping list. Default on. */
    const val APP_FUNCTIONS_REOPEN_LIST_ENABLED = "feat_app_functions_reopen_list"

    /** List all members and their roles in shopping list. Default on. */
    const val APP_FUNCTIONS_LIST_MEMBERS_ENABLED = "feat_app_functions_list_members"

    /** Generate or retrieve share code for list. Default on. */
    const val APP_FUNCTIONS_GENERATE_SHARE_CODE_ENABLED = "feat_app_functions_generate_share_code"

    // --- Shopping List Features ---
    /** Budget alerts: monthly target card, budget line, spending warnings. Default on. */
    const val BUDGET_ALERTS_ENABLED = "feat_budget_alerts_enabled"

    // ============================================================================
    // CATEGORY 4: User Feedback & Analytics
    // ============================================================================

    // --- Feedback Collection ---
    /** NPS (Net Promoter Score) surveys. Default on. */
    const val FEEDBACK_NPS_ENABLED = "feat_feedback_nps"

    /** CSAT (Customer Satisfaction) surveys per feature. Default on. */
    const val FEEDBACK_CSAT_ENABLED = "feat_feedback_csat"

    /** Thumb up/down sentiment in chat. Default on. */
    const val FEEDBACK_SENTIMENT_ENABLED = "feat_feedback_sentiment"

    /** In-App Review prompt (Play Store rating request). Default on. */
    const val FEEDBACK_STORE_REVIEW_ENABLED = "feedback_store_review_enabled"

    /** Legacy alias for all feedback features (NPS + CSAT + sentiment). Maps to NPS toggle. */
    const val FEEDBACK_ENABLED = FEEDBACK_NPS_ENABLED

    // --- Analytics Events ---
    /** Track UI action events (clicks, taps, interactions). Default on. */
    const val ANALYTICS_UI_ACTIONS_ENABLED = "feat_analytics_ui_actions"

    /** Track feature usage and funnel events. Default on. */
    const val ANALYTICS_FEATURE_USAGE_ENABLED = "feat_analytics_feature_usage"

    /** Track performance metrics (latency, errors). Default on. */
    const val ANALYTICS_PERFORMANCE_ENABLED = "feat_analytics_performance"

    // ============================================================================
    // CATEGORY 5: Platform Integration
    // ============================================================================

    /** Wear OS complication and widget integration. Default on. */
    const val WEAR_WIDGETS_ENABLED = "feat_wear_widgets"

    /** Wear OS notifications and alerts. Default on. */
    const val WEAR_NOTIFICATIONS_ENABLED = "feat_wear_notifications"

    /** Transições de slide entre telas (sdd-045); desligada, o `NavHost` volta ao fade padrão. Default on. */
    const val FLUID_TRANSITIONS_ENABLED = "feat_fluid_transitions"

    // ============================================================================
    // CATEGORY 6: API & Network Configuration (Timeouts, Retries, Resilience)
    // ============================================================================

    /** Fallback to Gemini Vision API for photo OCR (disable if API broken). Default on. */
    const val FEAT_AI_VISION_FALLBACK_ENABLED = "feat_ai_vision_fallback_enabled"

    /** Skip onboarding screen on first launch. Default off. */
    const val FEAT_ONBOARDING_SKIP_ENABLED = "feat_onboarding_skip_enabled"

    /** Show tips during onboarding. Default on. */
    const val FEAT_ONBOARDING_SHOW_TIPS = "feat_onboarding_show_tips"

    /** Aggressive CTA (call-to-action) in empty state to drive activation. Default off. */
    const val FEAT_EMPTY_STATE_CTA_AGGRESSIVE = "feat_empty_state_cta_aggressive"

    /** Auto-add sample products when creating first list to reduce friction. Default off. */
    const val FEAT_AUTO_ADD_SAMPLE_PRODUCTS = "feat_auto_add_sample_products"

    /** Show notification badge on AI Chat FAB. Default on. */
    const val AI_CHAT_FAB_SHOW_BADGE = "ai_chat_fab_show_badge"

    /** Show first message suggestions in AI Chat (to improve engagement). Default on. */
    const val FEAT_AI_CHAT_FIRST_MESSAGE_SUGGESTION = "feat_ai_chat_first_message_suggestion"

    /** Enable animations in shopping lists (disable on slow devices). Default on. */
    const val FEAT_ENABLE_LIST_ANIMATIONS = "feat_enable_list_animations"

    /** Enable offline vision mode (cache vision results for later). Default off. */
    const val FEAT_AI_VISION_OFFLINE_MODE_ENABLED = "feat_ai_vision_offline_mode_enabled"
}

/**
 * Nomes das variáveis remotas conhecidas no Firebase Remote Config, centralizados para evitar
 * strings soltas espalhadas pelos módulos que consultam [br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService].
 *
 * **Organização:** por funcionalidade semântica.
 * **Tipo:** strings ou números, não booleanos (use FeatureFlagKeys para toggles).
 */
object RemoteVariableKeys {

    // ============================================================================
    // AI Assistant Configuration
    // ============================================================================

    /** OpenRouter API key. Fetched on demand. */
    const val OPEN_ROUTER_API_KEY = "open_router_api_key"

    /** OpenRouter model identifier (e.g., "anthropic/claude-3.5-sonnet"). */
    const val OPEN_ROUTER_MODEL = "open_router_model"

    /** Gemini API key. Fetched on demand. */
    const val GEMINI_API_KEY = "gemini_api_key"

    /** Gemini model for agent chat (e.g., "gemini-2.0-flash"). */
    const val GEMINI_AGENT_MODEL = "gemini_agent_model"

    /**
     * The assistant's system prompt (behavior guidelines, scope, guardrails). Remote so it can
     * be tightened or corrected — e.g. a missed guardrail case — without waiting on a store
     * rollout; [br.com.brunocarvalhs.howmuch.feature.ai_agent.data.service.SystemPrompts] is the
     * compiled-in fallback for when Remote Config hasn't been fetched yet.
     */
    const val AI_SYSTEM_PROMPT = "ai_system_prompt"

    /** Maximum tokens for AI response generation. Default: 1024. */
    const val AI_MAX_TOKENS = "ai_max_tokens"

    /** Temperature for AI model (0.0-1.0). Controls creativity. Default: 0.7. */
    const val AI_TEMPERATURE = "ai_temperature"

    // ============================================================================
    // Feedback & Analytics Configuration
    // ============================================================================

    /** Minimum days between two NPS surveys for same user. Default: 90. */
    const val FEEDBACK_NPS_COOLDOWN_DAYS = "feedback_nps_cooldown_days"

    /** Minimum days between two CSAT surveys for same feature. Default: 30. */
    const val FEEDBACK_CSAT_COOLDOWN_DAYS = "feedback_csat_cooldown_days"

    /** NPS target threshold (0-10) to trigger follow-up. Default: 9 (Promoter). */
    const val FEEDBACK_NPS_TARGET_THRESHOLD = "feedback_nps_target_threshold"

    /** Maximum events per session before throttling analytics. Default: 50. */
    const val ANALYTICS_MAX_EVENTS_PER_SESSION = "analytics_max_events_per_session"

    /** Sampling rate for performance events (0.0-1.0). Default: 0.1 (10%). */
    const val ANALYTICS_PERFORMANCE_SAMPLING_RATE = "analytics_performance_sampling_rate"

    // ============================================================================
    // Feature Behavior Configuration
    // ============================================================================

    /** Maximum products per shopping list before performance warning. Default: 100. */
    const val SHOPPING_MAX_PRODUCTS_PER_LIST = "shopping_max_products_per_list"

    /** Budget alert threshold (% of budget spent). Default: 80. */
    const val BUDGET_ALERT_THRESHOLD_PERCENT = "budget_alert_threshold_percent"

    /** Share code expiration in days. Default: 30. */
    const val SHARE_CODE_EXPIRATION_DAYS = "share_code_expiration_days"

    /** Wear OS sync interval in minutes. Default: 15. */
    const val WEAR_SYNC_INTERVAL_MINUTES = "wear_sync_interval_minutes"

    // ============================================================================
    // API & Network Configuration
    // ============================================================================

    /** Timeout for API requests in seconds. Default: 30. */
    const val API_REQUEST_TIMEOUT_SECONDS = "api_request_timeout_seconds"

    /**
     * Versão mínima (versionCode) exigida pelo force update via Play In-App Updates. Abaixo dela o
     * app é atualizado antes de continuar. Default: 0 (sem force update). Só aumente depois que a
     * versão nova estiver publicada na Play, para não bloquear quem não tem como atualizar.
     */
    const val APP_MIN_VERSION_CODE = "app_min_version_code"

    /** Maximum retry attempts for failed API calls. Default: 3. */
    const val API_RETRY_MAX_ATTEMPTS = "api_retry_max_attempts"

    /** Exponential backoff multiplier for retries (e.g., 1.5 → 1s, 1.5s, 2.25s, ...). Default: "1.5". */
    const val API_RETRY_BACKOFF_MULTIPLIER = "api_retry_backoff_multiplier"

    /** Timeout specific to Gemini Vision API calls in seconds. Default: 10. */
    const val AI_VISION_TIMEOUT_SECONDS = "ai_vision_timeout_seconds"

    /** Number of retries for photo OCR scan. Default: 2. */
    const val PHOTO_SCAN_RETRY_COUNT = "photo_scan_retry_count"

    // ============================================================================
    // UX & Onboarding Configuration
    // ============================================================================

    /** Number of sample products to auto-add on first list creation. Default: 3. */
    const val SAMPLE_PRODUCTS_COUNT = "sample_products_count"

    /** Examples for AI Chat first message suggestions (comma-separated). Default: "add,search,recipe". */
    const val AI_CHAT_SUGGESTION_EXAMPLES = "ai_chat_suggestion_examples"

    /** Duration of list animations in milliseconds. Default: 300. */
    const val ANIMATION_DURATION_MS = "animation_duration_ms"

    // ============================================================================
    // Data & Sync Configuration (Firestore, Cache, Wear OS)
    // ============================================================================

    /** Batch size for Firestore queries. Default: 25. */
    const val FIRESTORE_BATCH_SIZE = "firestore_batch_size"

    /** TTL (time to live) for local cache in minutes. Default: 60. */
    const val LOCAL_CACHE_TTL_MINUTES = "local_cache_ttl_minutes"

    /** Maximum pending changes allowed in offline mode before error. Default: 100. */
    const val OFFLINE_MODE_MAX_PENDING_CHANGES = "offline_mode_max_pending_changes"

    /** Full sync interval for Wear OS in minutes (heavy operation). Default: 15. */
    const val WEAR_FULL_SYNC_INTERVAL_MINUTES = "wear_full_sync_interval_minutes"

    /** Delta (incremental) sync interval for Wear OS in seconds (lightweight). Default: 30. */
    const val WEAR_DELTA_SYNC_INTERVAL_SECONDS = "wear_delta_sync_interval_seconds"

    /** Conflict resolution strategy for shared lists. Options: last_write_wins, merge. Default: last_write_wins. */
    const val SHARED_LIST_CONFLICT_RESOLUTION = "shared_list_conflict_resolution"
}
