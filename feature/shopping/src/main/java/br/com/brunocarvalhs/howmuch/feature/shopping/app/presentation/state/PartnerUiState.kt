package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state

import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile

internal data class PartnerUiState(
    val partner: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
