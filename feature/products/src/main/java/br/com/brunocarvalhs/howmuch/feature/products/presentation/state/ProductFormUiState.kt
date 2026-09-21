package br.com.brunocarvalhs.howmuch.feature.products.presentation.state

import androidx.compose.runtime.Stable

private const val CURRENCY_DIVISOR = 100.0

@Stable
internal data class ProductFormUiState(
    val name: String = "",
    val quantity: String = "1",
    val price: String = "",
    val category: String = "",
    val isCategoryManuallySet: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val duplicateWarning: String? = null
) {
    val quantityValue: Double? get() = quantity.toDoubleOrNull()
    val priceValue: Double? get() = price.toDoubleOrNull()?.div(CURRENCY_DIVISOR)
    val isValid: Boolean get() = name.isNotBlank() && (quantityValue ?: 0.0) > 0
}
