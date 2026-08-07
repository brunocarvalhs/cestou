package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText

@Stable
internal data class JoinListUiState(
    val isLoading: Boolean = false,
    val error: UiText? = null
)
