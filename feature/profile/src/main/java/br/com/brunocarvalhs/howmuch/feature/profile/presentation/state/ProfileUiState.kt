package br.com.brunocarvalhs.howmuch.feature.profile.presentation.state

import androidx.compose.runtime.Stable
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser

@Stable
internal data class ProfileUiState(
    val user: AuthenticatedUser? = null,
    val isLoading: Boolean = false
)
