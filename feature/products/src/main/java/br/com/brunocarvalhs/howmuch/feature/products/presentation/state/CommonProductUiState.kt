package br.com.brunocarvalhs.howmuch.feature.products.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.CommonProduct

@Stable
internal data class CommonProductUiState(
    val isLoading: Boolean = false,
    val items: List<CommonProduct> = emptyList(),
    val newItemName: String = "",
    val message: String? = null
)
