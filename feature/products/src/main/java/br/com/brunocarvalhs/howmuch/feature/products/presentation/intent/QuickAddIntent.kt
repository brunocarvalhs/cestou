package br.com.brunocarvalhs.howmuch.feature.products.presentation.intent

internal data class QuickAddIntent(
    val onNewItemNameChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onDuplicateWarningShown: () -> Unit = {},
    val onSaveErrorShown: () -> Unit = {}
)
