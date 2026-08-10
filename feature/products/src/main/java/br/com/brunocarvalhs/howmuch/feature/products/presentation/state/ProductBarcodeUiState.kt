package br.com.brunocarvalhs.howmuch.feature.products.presentation.state

internal data class ProductBarcodeUiState(
    val isFlashOn: Boolean = false,
    val lastScannedBarcode: String? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)
