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

    const val AI_CHAT_MESSAGE_SENT = "ai_chat_message_sent"

    const val SETTINGS_LANGUAGE_CHANGED = "settings_language_changed"
    const val SETTINGS_CURRENCY_CHANGED = "settings_currency_changed"

    const val AUTH_SIGN_IN_FAILED = "auth_sign_in_failed"

    const val PROFILE_SIGN_OUT = "profile_sign_out"
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
    const val AMOUNT = "amount"
    const val ITEMS_COUNT = "items_count"
    const val SOURCE = "source"
}
