package br.com.brunocarvalhs.howmuch.core.remoteconfig.model

/**
 * Compiled-in default values for all Firebase Remote Config parameters.
 * Used as fallback when:
 * - Network is offline
 * - Firebase RCC fetch fails or times out (>5s)
 * - App is running in airplane mode
 *
 * These defaults are NOT overridden by Remote Config; they're merged.
 * Remote Config values take precedence if available (non-null).
 *
 * Strategy: All defaults are "safe" (features ON by default for best UX).
 * If a feature causes issues, it's disabled via Firebase Remote Config (not code).
 */
object RemoteConfigDefaults {

    // ============================================================================
    // CATEGORY 1: AI Assistant & LLM Integration
    // ============================================================================

    const val AI_OPENROUTER_ENABLED_DEFAULT = true
    const val AI_GEMINI_ENABLED_DEFAULT = true
    const val PROACTIVE_AI_SUGGESTIONS_ENABLED_DEFAULT = true
    const val PROACTIVE_AI_ACTIONS_ENABLED_DEFAULT = true

    const val AI_MAX_TOKENS_DEFAULT = 1024
    const val AI_TEMPERATURE_DEFAULT = "0.7"

    // ============================================================================
    // CATEGORY 2: Authentication & Identity
    // ============================================================================

    const val AUTH_GOOGLE_ENABLED_DEFAULT = true
    const val AUTH_EMAIL_ENABLED_DEFAULT = true
    const val AUTH_PHONE_ENABLED_DEFAULT = false
    const val AUTH_APPLE_ENABLED_DEFAULT = false
    const val AUTH_MICROSOFT_ENABLED_DEFAULT = false
    const val AUTH_FACEBOOK_ENABLED_DEFAULT = false
    const val AUTH_GITHUB_ENABLED_DEFAULT = false
    const val WEAR_LOGIN_ENABLED_DEFAULT = true

    // ============================================================================
    // CATEGORY 3: Shopping List Management & Automation (AppFunctions)
    // ============================================================================

    // Base CRUD
    const val APP_FUNCTIONS_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_BASE_ENABLED_DEFAULT = true

    // Extended (Mutations)
    const val APP_FUNCTIONS_MARK_PURCHASED_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_REMOVE_PRODUCT_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_UPDATE_BUDGET_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_GET_PRODUCTS_ENABLED_DEFAULT = true

    // Tier 1 (Search & Bulk)
    const val APP_FUNCTIONS_SEARCH_PRODUCT_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_ADD_MULTIPLE_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_LIST_ALL_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_GET_DETAILS_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_CLEAR_PURCHASED_ENABLED_DEFAULT = true

    // Tier 2 (Analytics & Advanced)
    const val APP_FUNCTIONS_DUPLICATE_LIST_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_RECIPE_TO_LIST_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_MONTHLY_SPENDING_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_SPENDING_HISTORY_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_RENAME_LIST_ENABLED_DEFAULT = true

    // Tier 3 (Advanced Operations)
    const val APP_FUNCTIONS_UPDATE_PRODUCT_PRICE_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_DUPLICATE_PRODUCT_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_REOPEN_LIST_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_LIST_MEMBERS_ENABLED_DEFAULT = true
    const val APP_FUNCTIONS_GENERATE_SHARE_CODE_ENABLED_DEFAULT = true

    // Budget Management
    const val BUDGET_ALERTS_ENABLED_DEFAULT = true
    const val BUDGET_ALERT_THRESHOLD_PERCENT_DEFAULT = "80"

    // ============================================================================
    // CATEGORY 4: User Feedback & Analytics
    // ============================================================================

    // Feedback Collection
    const val FEEDBACK_NPS_ENABLED_DEFAULT = true
    const val FEEDBACK_CSAT_ENABLED_DEFAULT = true
    const val FEEDBACK_SENTIMENT_ENABLED_DEFAULT = true
    const val FEEDBACK_STORE_REVIEW_ENABLED_DEFAULT = true
    const val FEEDBACK_ENABLED_DEFAULT = true // Legacy alias

    // Analytics Events
    const val ANALYTICS_UI_ACTIONS_ENABLED_DEFAULT = true
    const val ANALYTICS_FEATURE_USAGE_ENABLED_DEFAULT = true
    const val ANALYTICS_PERFORMANCE_ENABLED_DEFAULT = true

    const val ANALYTICS_MAX_EVENTS_PER_SESSION_DEFAULT = 50
    const val ANALYTICS_PERFORMANCE_SAMPLING_RATE_DEFAULT = "0.1"

    // Feedback Cooldown (days)
    const val FEEDBACK_NPS_COOLDOWN_DAYS_DEFAULT = 90
    const val FEEDBACK_CSAT_COOLDOWN_DAYS_DEFAULT = 30
    const val FEEDBACK_NPS_TARGET_THRESHOLD_DEFAULT = 9

    // ============================================================================
    // CATEGORY 5: Platform Integration
    // ============================================================================

    const val WEAR_WIDGETS_ENABLED_DEFAULT = true
    const val WEAR_NOTIFICATIONS_ENABLED_DEFAULT = true
    const val WEAR_SYNC_INTERVAL_MINUTES_DEFAULT = 15

    // ============================================================================
    // Feature Behavior Configuration
    // ============================================================================

    const val SHOPPING_MAX_PRODUCTS_PER_LIST_DEFAULT = 100
    const val SHARE_CODE_EXPIRATION_DAYS_DEFAULT = 30

    // ============================================================================
    // CATEGORY 6: API & Network Configuration
    // ============================================================================

    const val API_REQUEST_TIMEOUT_SECONDS_DEFAULT = 30
    const val APP_MIN_VERSION_CODE_DEFAULT = 0L
    const val API_RETRY_MAX_ATTEMPTS_DEFAULT = 3
    const val API_RETRY_BACKOFF_MULTIPLIER_DEFAULT = "1.5"
    const val FEAT_AI_VISION_FALLBACK_ENABLED_DEFAULT = true
    const val AI_VISION_TIMEOUT_SECONDS_DEFAULT = 10
    const val PHOTO_SCAN_RETRY_COUNT_DEFAULT = 2
    const val FEAT_AI_VISION_OFFLINE_MODE_ENABLED_DEFAULT = false

    // ============================================================================
    // CATEGORY 7: UX & Onboarding
    // ============================================================================

    const val FEAT_ONBOARDING_SKIP_ENABLED_DEFAULT = false
    const val FEAT_ONBOARDING_SHOW_TIPS_DEFAULT = true
    const val FEAT_EMPTY_STATE_CTA_AGGRESSIVE_DEFAULT = false
    const val FEAT_AUTO_ADD_SAMPLE_PRODUCTS_DEFAULT = false
    const val SAMPLE_PRODUCTS_COUNT_DEFAULT = 3
    const val AI_CHAT_FAB_SHOW_BADGE_DEFAULT = true
    const val FEAT_AI_CHAT_FIRST_MESSAGE_SUGGESTION_DEFAULT = true
    const val AI_CHAT_SUGGESTION_EXAMPLES_DEFAULT = "add,search,recipe"
    const val FEAT_ENABLE_LIST_ANIMATIONS_DEFAULT = true
    const val ANIMATION_DURATION_MS_DEFAULT = 300

    // ============================================================================
    // CATEGORY 8: Data & Sync Configuration
    // ============================================================================

    const val FIRESTORE_BATCH_SIZE_DEFAULT = 25
    const val LOCAL_CACHE_TTL_MINUTES_DEFAULT = 60
    const val OFFLINE_MODE_MAX_PENDING_CHANGES_DEFAULT = 100
    const val WEAR_FULL_SYNC_INTERVAL_MINUTES_DEFAULT = 15
    const val WEAR_DELTA_SYNC_INTERVAL_SECONDS_DEFAULT = 30
    const val SHARED_LIST_CONFLICT_RESOLUTION_DEFAULT = "last_write_wins"

    // ============================================================================
    // CATEGORY 9: Paywall & Subscription
    // ============================================================================

    const val PAYWALL_MULTI_PLANS_ENABLED_DEFAULT = false
}
