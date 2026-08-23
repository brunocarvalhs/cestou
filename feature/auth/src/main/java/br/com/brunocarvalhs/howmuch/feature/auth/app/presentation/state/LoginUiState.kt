package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.state

import androidx.compose.runtime.Stable

@Stable
internal data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
