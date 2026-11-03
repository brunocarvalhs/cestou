package br.com.brunocarvalhs.howmuch.core.analytics.model

/**
 * Catálogo de telas reportadas ao GA4. [id] vira `screen_name` (mantido igual ao que as
 * ViewModels enviavam antes, para preservar o histórico das Explorations) e [screenClass] vira
 * `screen_class`, o nome do Composable da tela - nunca o de uma ViewModel.
 */
enum class AnalyticsScreen(val id: String, val screenClass: String) {
    WELCOME("welcome", "WelcomeScreen"),
    EMAIL_SIGN_IN("email_sign_in", "EmailSignInScreen"),
    EMAIL_SIGN_UP("email_sign_up", "EmailSignUpScreen"),
    PASSWORD_RESET("password_reset", "PasswordResetScreen"),
    COMPLETE_NAME("complete_name", "CompleteNameScreen"),
    LINK_WEAR_DEVICE("link_wear_device", "LinkWearDeviceScreen"),

    SHOPPING_LIST("shopping_list", "ShoppingScreen"),
    SHOPPING_EDIT("shopping_edit", "EditShoppingContent"),
    SHOPPING_JOIN("shopping_join", "JoinListContent"),
    SHOPPING_JOIN_SCANNER("shopping_join_scanner", "QrCodeScanner"),
    SHOPPING_QR_CODE("shopping_qr_code", "QrCodeContent"),
    NOTIFICATIONS("notifications", "NotificationsScreen"),
    HISTORY("history", "HistoryScreen"),
    SPENDING_INSIGHTS("spending_insights", "SpendingInsightsScreen"),

    CART("cart", "CartScreen"),
    CART_FINISH_PURCHASE("cart_finish_purchase", "FinishPurchaseContent"),
    CART_CONFIRM_ITEM("cart_confirm_item", "ConfirmItemContent"),
    CART_EDIT_ITEM("cart_edit_item", "EditItemContent"),
    CART_PRODUCT_HISTORY("cart_product_history", "ProductHistoryContent"),
    CART_SHARE_OPTIONS("cart_share_options", "ShareOptionsContent"),

    PRODUCT_PICKER("product_picker", "ProductScreen"),
    PRODUCT_PHOTO_SCAN("product_photo_scan", "ProductPhotoContent"),

    AI_CHAT("ai_chat", "AiChatScreen"),
    AI_CHAT_HISTORY("ai_chat_history", "AiHistoryScreen"),

    PROFILE("profile", "ProfileScreen"),

    FEEDBACK_SURVEY("feedback_survey", "FeedbackSurveyScreen"),
    STORE_REVIEW("store_review", "StoreReviewHost"),

    PAYWALL("paywall", "PaywallScreen"),
    MANAGE_SUBSCRIPTION("manage_subscription", "ManageSubscriptionScreen"),

    SETTINGS_THEME("settings_theme", "ThemeSettingsScreen"),
    SETTINGS_LANGUAGE("settings_language", "PlaceholderSettingsScreen"),
    SETTINGS_CURRENCY("settings_currency", "PlaceholderSettingsScreen"),
    SETTINGS_AI("settings_ai", "AiSettingsScreen"),
    SETTINGS_SHOPPING("settings_shopping", "ShoppingSettingsScreen"),
    SETTINGS_DATA("settings_data", "DataSettingsScreen"),
    SETTINGS_NOTIFICATIONS("settings_notifications", "NotificationSettingsScreen"),
    SUPPORT_CONTACT("support_contact", "LegalContentScreen"),
    SUPPORT_BUG_REPORT("support_bug_report", "LegalContentScreen"),
    SUPPORT_FEEDBACK("support_feedback", "LegalContentScreen"),
    APP_RATE("app_rate", "LegalContentScreen"),
    TERMS_OF_USE("terms_of_use", "LegalContentScreen"),
    PRIVACY_POLICY("privacy_policy", "LegalContentScreen"),
    OPEN_SOURCE_LICENSES("open_source_licenses", "OpenSourceLicensesScreen"),
    RELEASE_NOTES("release_notes", "ReleaseNotesScreen")
}
