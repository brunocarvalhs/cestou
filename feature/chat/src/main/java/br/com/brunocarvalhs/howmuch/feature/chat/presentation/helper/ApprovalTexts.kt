package br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper

import androidx.annotation.StringRes
import br.com.brunocarvalhs.howmuch.feature.chat.R

/**
 * Título do card de aprovação por ação: diz em linguagem simples o que a IA quer fazer. Ação
 * desconhecida cai num texto genérico, que continua verdadeiro para qualquer ação irreversível.
 */
@StringRes
internal fun approvalTitleFor(actionId: String): Int = when (actionId) {
    "delete_shopping_list",
    "delete_multi_shopping_list" -> R.string.ai_approval_title_delete_list
    "clear_purchased_products" -> R.string.ai_approval_title_clear_purchased
    else -> R.string.ai_approval_title_generic
}
