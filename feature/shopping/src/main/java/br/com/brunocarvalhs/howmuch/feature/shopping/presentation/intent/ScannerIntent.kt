package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent

internal data class ScannerIntent(
    val onTokenScanned: (String) -> Unit = {},
    val onDismiss: () -> Unit = {}
)
