package br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper

import androidx.annotation.StringRes
import br.com.brunocarvalhs.howmuch.feature.chat.R

/**
 * Texto de andamento de cada ação da IA, agrupado por intenção: o usuário não precisa saber o nome
 * técnico da ação, só o que está acontecendo. Ação desconhecida ou nenhuma cai em "pensando".
 */
@StringRes
internal fun activityLabelFor(actionId: String?): Int = when (actionId) {
    "create_shopping_list" -> R.string.ai_chat_activity_creating_list
    "save_product_to_cart",
    "add_multiple_products_to_cart",
    "add_common_products_to_cart",
    "add_recipe_ingredients" -> R.string.ai_chat_activity_adding_items
    "search_products",
    "search_recipes" -> R.string.ai_chat_activity_searching
    "get_all_shopping_lists",
    "get_shopping_list_details",
    "open_shopping_by_id" -> R.string.ai_chat_activity_checking_lists
    "update_shopping_list",
    "reopen_shopping_list" -> R.string.ai_chat_activity_updating_list
    "delete_shopping_list",
    "delete_multi_shopping_list",
    "clear_purchased_products" -> R.string.ai_chat_activity_removing
    else -> R.string.ai_chat_activity_thinking
}
