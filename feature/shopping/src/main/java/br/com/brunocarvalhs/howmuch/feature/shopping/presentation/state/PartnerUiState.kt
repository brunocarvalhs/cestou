package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state

import br.com.brunocarvalhs.howmuch.core.domain.entity.UserProfile

internal data class PartnerUiState(
    val partner: UserProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
