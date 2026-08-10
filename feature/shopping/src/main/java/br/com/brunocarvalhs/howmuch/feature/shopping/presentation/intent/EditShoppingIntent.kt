package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent

import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping

internal data class EditShoppingIntent(
    val onUpdate: (Shopping) -> Unit = {},
    val onShareToken: (String) -> Unit = {},
    val onCancel: () -> Unit = {}
)
