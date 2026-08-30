package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping

@Stable
internal data class EditShoppingUiState(
    val shopping: Shopping? = null,
    val sharingToken: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
