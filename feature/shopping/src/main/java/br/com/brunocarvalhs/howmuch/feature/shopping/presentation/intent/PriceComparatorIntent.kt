package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent

internal data class PriceComparatorIntent(
    val onSaveLocation: (state: String, city: String) -> Unit = { _, _ -> },
    val onChangeLocation: () -> Unit = {}
)
