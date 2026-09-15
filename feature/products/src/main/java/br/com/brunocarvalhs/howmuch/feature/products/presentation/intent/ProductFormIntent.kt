package br.com.brunocarvalhs.howmuch.feature.products.presentation.intent

internal data class ProductFormIntent(
    val onNameChange: (String) -> Unit = {},
    val onQuantityChange: (String) -> Unit = {},
    val onPriceChange: (String) -> Unit = {},
    val onCategorySelected: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onSaveErrorShown: () -> Unit = {},
    val onDuplicateWarningShown: () -> Unit = {}
)
