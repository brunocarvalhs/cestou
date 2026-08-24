package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.intent

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping

internal data class EditShoppingIntent(
    val onUpdate: (Shopping) -> Unit = {},
    val onShareToken: (String) -> Unit = {},
    val onCancel: () -> Unit = {},
    val onErrorShown: () -> Unit = {}
)
