package br.com.brunocarvalhs.howmuch.core.ai.model

import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction

/**
 * Key in the request metadata (`AgentContext.toMetadata()`). A request that sets it to `true` asks
 * for every write action in [WRITE_ACTION_IDS] to be approved by the user first. Only the watch's
 * AI relay sets it, so the phone chat keeps its own rule.
 */
const val REQUIRE_WRITE_CONFIRMATION_KEY = "require_write_confirmation"

/**
 * Actions that change data. Read-only actions (`get_all_shopping_lists`, `get_shopping_list_details`,
 * `search_products`, `search_recipes`, `open_shopping_by_id`) are not listed, so they never ask for
 * approval.
 */
val WRITE_ACTION_IDS: Set<String> = setOf(
    "add_common_products_to_cart",
    "add_multiple_products_to_cart",
    "add_recipe_ingredients",
    "clear_purchased_products",
    "create_shopping_list",
    "delete_multi_shopping_list",
    "delete_product",
    "delete_shopping_list",
    "reopen_shopping_list",
    "save_product_to_cart",
    "update_shopping_list"
)

/**
 * Whether [action] needs the user's approval before it runs. Its own flag always counts. A request
 * that sets [REQUIRE_WRITE_CONFIRMATION_KEY] also needs approval for every write action in
 * [WRITE_ACTION_IDS]. Other requests keep the action's own flag only.
 */
fun requiresApproval(action: AgentAction<*>, meta: Map<String, Any?>): Boolean =
    action.requiresConfirmation ||
        (meta[REQUIRE_WRITE_CONFIRMATION_KEY] == true && action.id in WRITE_ACTION_IDS)
