package br.com.brunocarvalhs.howmuch.core.analytics.model

/**
 * Identificadores de controles para `ui_action`, no formato `<tela>.<elemento>`. Constantes
 * (nunca texto digitado) mantêm a dimensão `element_id` com cardinalidade baixa no GA4.
 */
object AnalyticsElements {
    const val SHOPPING_LIST_CREATE = "shopping_list.create"
    const val SHOPPING_LIST_OPEN = "shopping_list.open"
    const val SHOPPING_LIST_FILTER = "shopping_list.filter"
    const val SHOPPING_LIST_SEARCH = "shopping_list.search"
    const val SHOPPING_LIST_AI_FAB = "shopping_list.ai_fab"
    const val SHOPPING_LIST_FAVORITE = "shopping_list.favorite"
    const val SHOPPING_LIST_DUPLICATE = "shopping_list.duplicate"
    const val SHOPPING_LIST_EDIT = "shopping_list.edit"
    const val SHOPPING_LIST_FINISH = "shopping_list.finish"
    const val SHOPPING_LIST_REOPEN = "shopping_list.reopen"
    const val SHOPPING_LIST_JOIN = "shopping_list.join"
    const val SHOPPING_LIST_MOVE = "shopping_list.move"

    const val CART_ADD_PRODUCT = "cart.add_product"
    const val CART_SHARE = "cart.share"
    const val CART_EDIT_PRODUCT = "cart.edit_product"
    const val CART_PRODUCT_HISTORY = "cart.product_history"

    const val PRODUCT_PICKER_PHOTO_SCAN_OPENED = "product_picker.photo_scan_opened"
    const val PRODUCT_PHOTO_RETAKE = "product_photo.retake"
    const val PRODUCT_PHOTO_ANALYZE = "product_photo.analyze"
    const val PRODUCT_PHOTO_CONFIRM_ALL = "product_photo.confirm_all"

    const val NOTIFICATIONS_FILTER = "notifications.filter"
    const val HISTORY_SEE_ANALYSIS = "history.see_analysis"
    const val JOIN_SCAN_QR = "join_list.scan_qr"
    const val EDIT_SHOPPING_SHARE_TOKEN = "edit_shopping.share_token"

    const val PROFILE_LINK_WEAR = "profile.link_wear"
    const val PROFILE_LINK_MOBILE = "profile.link_mobile"
    const val PROFILE_MANAGE_SUBSCRIPTION = "profile.manage_subscription"
    const val AI_SETTINGS_OPEN_PAYWALL = "ai_settings.open_paywall"
    const val SETTINGS_OPEN_URL = "settings.open_url"
    const val SETTINGS_SEND_EMAIL = "settings.send_email"
    const val SETTINGS_NAVIGATE = "settings.navigate"
    const val DATA_SETTINGS_CLEAR_CACHE = "data_settings.clear_cache"
    const val DATA_SETTINGS_DELETE_ALL = "data_settings.delete_all"
    const val DATA_SETTINGS_DELETE_ACCOUNT = "data_settings.delete_account"

    const val AI_CHAT_SETTINGS = "ai_chat.settings"
    const val AI_CHAT_TOGGLE_HISTORY = "ai_chat.toggle_history"
    const val AI_CHAT_CLOSE = "ai_chat.close"
    const val AI_CHAT_SELECT_HISTORY = "ai_chat.select_history"
    const val AI_HISTORY_DELETE_SESSION = "ai_history.delete_session"
    const val WELCOME_LANGUAGE = "welcome.language"

    const val WEAR_LINK_CODE_SUBMIT = "link_wear.submit"
}
