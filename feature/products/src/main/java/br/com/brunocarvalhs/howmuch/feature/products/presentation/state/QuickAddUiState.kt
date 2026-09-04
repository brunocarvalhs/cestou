package br.com.brunocarvalhs.howmuch.feature.products.presentation.state

import androidx.compose.runtime.Stable

@Stable
internal data class QuickAddUiState(
    val newItemName: String = "",
    val totalAmount: Double = 0.0,
    val budget: Double? = null,
    val duplicateWarning: String? = null
) {
    val isOverBudget: Boolean get() = budget != null && totalAmount > budget
}
