package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.intent

internal data class ProductBarcodeIntent(
    val onBarcodeScanner: (String) -> Unit = {},
    val onFlashToggle: () -> Unit = {}
)
