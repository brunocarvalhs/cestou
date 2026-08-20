package br.com.brunocarvalhs.howmuch.feature.settings.presentation.state

import br.com.brunocarvalhs.howmuch.core.domain.entity.AuthenticatedUser

internal data class ProfileUiState(
    val user: AuthenticatedUser? = null,
    val partner: PartnerInfo? = null,
    val isLoading: Boolean = false
)

internal data class PartnerInfo(
    val name: String,
    val photoUrl: String? = null
)
