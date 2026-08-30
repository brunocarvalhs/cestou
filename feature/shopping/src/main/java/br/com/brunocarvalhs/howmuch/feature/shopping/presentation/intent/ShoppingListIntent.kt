package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.ShoppingFilter

internal data class ShoppingListIntent(
    val onFetchAll: () -> Unit = {},
    val onCreate: () -> Unit = {},
    val onOpen: (String) -> Unit = {},
    val onFilter: (ShoppingFilter) -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onSearch: (String) -> Unit = {},
    val onToggleFavorite: (Shopping) -> Unit = {},
    val onDuplicate: (Shopping) -> Unit = {},
    val onShare: (Shopping) -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onEdit: (Shopping) -> Unit = {},
    val onUpdate: (Shopping) -> Unit = {},
    val onReopen: (Shopping) -> Unit = {},
    val onShowJoinDialog: () -> Unit = {},
    val onMove: (Int, Int) -> Unit = { _, _ -> },
    val onShowCreateSheet: (Boolean) -> Unit = {},
    val onCreateConfirmed: (String, String) -> Unit = { _, _ -> }
)
