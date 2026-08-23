package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.state

import androidx.compose.runtime.Stable

@Stable
internal data class ProductBarcodeUiState(
    val isFlashOn: Boolean = false,
    val lastScannedBarcode: String? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)
