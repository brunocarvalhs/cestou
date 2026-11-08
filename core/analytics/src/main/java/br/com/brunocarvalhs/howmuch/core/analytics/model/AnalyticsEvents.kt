package br.com.brunocarvalhs.howmuch.core.analytics.model

/**
 * Nomes dos eventos conhecidos, centralizados para evitar strings soltas espalhadas pelas
 * ViewModels que consultam [br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker].
 * Segue a convenção snake_case exigida pelo Firebase Analytics.
 */
object AnalyticsEvents {
    const val APP_OPEN = "app_open"

    const val SHOPPING_LIST_CREATED = "shopping_list_created"
    const val SHOPPING_LIST_DELETED = "shopping_list_deleted"
    const val SHOPPING_LIST_SHARED = "shopping_list_shared"
    const val SHOPPING_LIST_JOINED = "shopping_list_joined"
    const val SHOPPING_LIST_JOIN_FAILED = "shopping_list_join_failed"
    const val SHOPPING_BUDGET_SET = "shopping_budget_set"
    const val MONTHLY_GOAL_SET = "monthly_goal_set"
    const val BUDGET_ALERT_SHOWN = "budget_alert_shown"

    const val CART_PRODUCT_DELETED = "cart_product_deleted"
    const val CART_FINISH_PURCHASE_STARTED = "cart_finish_purchase_started"
    const val CART_FINISH_PURCHASE_COMPLETED = "cart_finish_purchase_completed"

    const val PRODUCT_SEARCH_PERFORMED = "product_search_performed"
    const val PRODUCT_SELECTED = "product_selected"

    // Fired for every "add to list" path that isn't a search pick (recipe, common/favorite
    // product, quick add free-text, AI photo scan) so the beta funnel can be broken down by
    // AnalyticsParams.SOURCE without multiplying event names per entry point.
    const val PRODUCT_ADDED = "product_added"
    const val PRODUCT_PHOTO_SCAN_PERFORMED = "product_photo_scan_performed"
    const val PRODUCT_PHOTO_SCAN_FAILED = "product_photo_scan_failed"

    // Mantém o nome histórico (sem prefixo "ai_message_") para não quebrar as Explorations do GA.
    const val AI_CHAT_MESSAGE_SENT = "ai_chat_message_sent"

    const val NOTIFICATIONS_OPENED = "notifications_opened"
    const val NOTIFICATION_CLICKED = "notification_clicked"
    const val NOTIFICATIONS_MARK_ALL_READ = "notifications_mark_all_read"

    const val SETTINGS_LANGUAGE_CHANGED = "settings_language_changed"
    const val SETTINGS_CURRENCY_CHANGED = "settings_currency_changed"
    const val SETTINGS_SECTION_TOGGLED = "settings_section_toggled"

    const val AUTH_SIGN_IN_FAILED = "auth_sign_in_failed"
    const val AUTH_SIGN_UP_FAILED = "auth_sign_up_failed"

    // REASON carries only the rule category (length/characters/contact/blocked), never the typed
    // name - it measures false positives of the anti-fake-name rules (spec EPA-13 item 16).
    const val AUTH_NAME_VALIDATION_FAILED = "auth_name_validation_failed"

    const val PROFILE_SIGN_OUT = "profile_sign_out"

    const val PAYWALL_SUBSCRIBE_CLICKED = "paywall_subscribe_clicked"
    const val PAYWALL_PURCHASE_FAILED = "paywall_purchase_failed"
    const val MANAGE_SUBSCRIPTION_PLAY_STORE_OPENED = "manage_subscription_play_store_opened"

    // History and spending insights never carry amounts, establishments or category names.
    const val HISTORY_VIEWED = "history_viewed"
    const val HISTORY_MONTH_SELECTED = "history_month_selected"
    const val HISTORY_PURCHASE_OPENED = "history_purchase_opened"
    const val SPENDING_INSIGHTS_VIEWED = "spending_insights_viewed"
    const val SPENDING_INSIGHTS_MONTH_SELECTED = "spending_insights_month_selected"
    const val SPENDING_INSIGHTS_LIST_OPENED = "spending_insights_list_opened"

    /** Quais controles o cliente realmente usa em cada tela (ver `trackAction`). */
    const val UI_ACTION = "ui_action"

    /** Onde o cliente hesita ou desiste: voltou, dispensou dialog ou sheet sem concluir. */
    const val BACK_PRESSED = "back_pressed"
    const val DIALOG_DISMISSED = "dialog_dismissed"
    const val TAB_SELECTED = "tab_selected"

    /** Onde as operações falham para o cliente sem ele reclamar. */
    const val OPERATION_FAILED = "operation_failed"

    /** Ciclo de vida: frequência e duração das sessões. */
    const val APP_FOREGROUND = "app_foreground"
    const val APP_BACKGROUND = "app_background"
    const val DEEP_LINK_OPENED = "deep_link_opened"

    /** Retenção: em quantos dias o cliente volta. */
    const val RETURNED_AFTER_DAYS = "returned_after_days"

    /** Ativação: quanto tempo leva até o primeiro valor. */
    const val FIRST_LIST_CREATED = "first_list_created"
    const val FIRST_PRODUCT_ADDED = "first_product_added"
    const val FIRST_PURCHASE_COMPLETED = "first_purchase_completed"

    /** Descoberta: quais recursos o cliente chegou a usar pelo menos uma vez. */
    const val FEATURE_FIRST_USED = "feature_first_used"

    /** O que o cliente muda na lista e nos produtos depois de criar. */
    const val SHOPPING_LIST_UPDATED = "shopping_list_updated"
    const val SHOPPING_LIST_OPENED = "shopping_list_opened"

    /** Abriu o seletor de produto e saiu sem adicionar nada: onde a ativação trava. */
    const val PRODUCT_ADD_ABANDONED = "product_add_abandoned"

    const val PRODUCT_UPDATED = "product_updated"
    const val PRODUCT_PURCHASED_TOGGLED = "product_purchased_toggled"
    const val PRODUCT_MOVED = "product_moved"
    const val PRODUCT_QUANTITY_CHANGED = "product_quantity_changed"
    const val CART_PURCHASED_CLEARED = "cart_purchased_cleared"
    const val CART_SHARE_OPTIONS_OPENED = "cart_share_options_opened"
    const val CART_PRODUCT_HISTORY_OPENED = "cart_product_history_opened"

    /** Contas: método usado para entrar ou criar a conta. */
    const val LOGIN = "login"
    const val SIGN_UP = "sign_up"
    const val PASSWORD_RESET_REQUESTED = "password_reset_requested"
    const val PROFILE_NAME_COMPLETED = "profile_name_completed"
    const val ACCOUNT_DELETED = "account_deleted"
    const val DATA_CLEARED = "data_cleared"

    /** Busca: o que o cliente procura. */
    const val SEARCH = "search"

    /** Funil de conversão em assinatura; `paywall_dismissed` leva `duration_ms` (leu ou fugiu na hora). */
    const val PAYWALL_VIEWED = "paywall_viewed"
    const val PAYWALL_DISMISSED = "paywall_dismissed"
    const val PLAN_CHANGED = "plan_changed"
    const val SUBSCRIPTION_STARTED = "subscription_started"

    /** Configurações e privacidade. */
    const val SETTINGS_THEME_CHANGED = "settings_theme_changed"
    const val SETTINGS_AI_CHANGED = "settings_ai_changed"
    const val SETTINGS_NOTIFICATIONS_TOGGLED = "settings_notifications_toggled"
    const val SETTINGS_SHOPPING_CHANGED = "settings_shopping_changed"
    const val ANALYTICS_CONSENT_CHANGED = "analytics_consent_changed"

    /** Uso fora do app: quais funções os assistentes do sistema chamam (`action_id` = nome da função). */
    const val APP_FUNCTION_INVOKED = "app_function_invoked"

    /** IA: de abrir o chat à resposta, voz, sessões e execução de ferramentas. */
    const val AI_CHAT_OPENED = "ai_chat_opened"
    const val AI_RESPONSE_RECEIVED = "ai_response_received"
    const val AI_RESPONSE_FAILED = "ai_response_failed"
    const val AI_TOOL_EXECUTED = "ai_tool_executed"
    const val AI_TOOL_BLOCKED_PRO = "ai_tool_blocked_pro"
    const val AI_TOOL_CONFIRMATION_REQUESTED = "ai_tool_confirmation_requested"
    const val AI_TOOL_CONFIRMATION_RESOLVED = "ai_tool_confirmation_resolved"
    const val AI_PROVIDER_FALLBACK = "ai_provider_fallback"
    const val AI_VOICE_INPUT_STARTED = "ai_chat_voice_input_started"
    const val AI_VOICE_INPUT_FAILED = "ai_voice_input_failed"
    const val AI_SESSION_CREATED = "ai_chat_session_created"
    const val AI_SESSION_SELECTED = "ai_session_selected"
    const val AI_TRIAL_MESSAGE_USED = "ai_trial_message_used"

    /** Chat proativo (sdd-040): toque num chip ou no "+" de um item, e adição em lote. Só contagens. */
    const val AI_SUGGESTION_CLICKED = "ai_suggestion_clicked"
    const val AI_BATCH_ITEMS_ADDED = "ai_batch_items_added"

    /** Satisfação (sdd-034): só nota e enums, nunca texto digitado nem conteúdo de mensagem da IA. */
    const val FEEDBACK_PROMPT_SHOWN = "feedback_prompt_shown"
    const val FEEDBACK_PROMPT_DISMISSED = "feedback_prompt_dismissed"
    const val NPS_SUBMITTED = "nps_submitted"
    const val CSAT_SUBMITTED = "csat_submitted"
    const val AI_MESSAGE_FEEDBACK = "ai_message_feedback"
    const val STORE_REVIEW_REQUESTED = "store_review_requested"
    const val FEEDBACK_CONTACT_OPENED = "feedback_contact_opened"
}

/**
 * Nomes de parâmetros reutilizados entre eventos, também centralizados por consistência.
 */
object AnalyticsParams {
    const val SHOPPING_ID = "shopping_id"
    const val PRODUCT_ID = "product_id"
    const val SEARCH_MODE = "search_mode"
    const val QUERY_LENGTH = "query_length"
    const val RESULT_COUNT = "result_count"
    const val LANGUAGE = "language"
    const val CURRENCY = "currency"
    const val REASON = "reason"
    const val JOIN_METHOD = "join_method"
    const val HAS_BUDGET = "has_budget"
    const val HAS_GOAL = "has_goal"
    const val GOAL_BUCKET = "goal_bucket"
    const val SCOPE = "scope"
    const val LEVEL = "level"
    const val AMOUNT = "amount"
    const val ITEMS_COUNT = "items_count"
    const val UNREAD_COUNT = "unread_count"
    const val NOTIFICATION_TYPE = "notification_type"
    const val SOURCE = "source"
    const val SECTION_ID = "section_id"
    const val EXPANDED = "expanded"

    /** Month as `yyyy-MM`. */
    const val MONTH = "month"

    const val ELEMENT_ID = "element_id"
    const val ENTITY = "entity"
    const val METHOD = "method"
    const val OPERATION = "operation"
    const val ERROR_TYPE = "error_type"
    const val FEATURE = "feature"
    const val PROVIDER = "provider"
    const val ACTION_ID = "action_id"
    const val DECISION = "decision"
    const val INPUT_METHOD = "input_method"
    const val LATENCY_MS = "latency_ms"
    const val DURATION_MS = "duration_ms"
    const val SUCCESS = "success"
    const val LENGTH_BUCKET = "length_bucket"
    const val TOOL_CALLS = "tool_calls"
    const val FALLBACK_USED = "fallback_used"
    const val SESSION_SECONDS = "session_seconds"
    const val DAYS = "days"
    const val TRIGGER = "trigger"
    const val FROM = "from"
    const val TO = "to"
    const val ENABLED = "enabled"
    const val VALUE = "value"
    const val HAS_CUSTOM_KEY = "has_custom_key"
    const val SECONDS_SINCE_SIGNUP = "seconds_since_signup"

    /** Nota da pesquisa: 0-10 no NPS, 1-5 no CSAT. */
    const val SCORE = "score"

    /** Tipo da pesquisa: `nps`, `csat` ou `store_review`. */
    const val KIND = "kind"
}
