package br.com.brunocarvalhs.howmuch.feature.cart.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText

@Stable
internal data class EditItemUiState(
    val error: UiText? = null,
    val isLoading: Boolean = false
)
