package br.com.brunocarvalhs.howmuch.feature.auth.presentation.state

internal data class LinkWearUiState(
    val code: String = "",
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
