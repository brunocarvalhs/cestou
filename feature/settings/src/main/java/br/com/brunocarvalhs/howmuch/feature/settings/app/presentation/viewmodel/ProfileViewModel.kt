package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent.ProfileIntent
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state.PartnerInfo
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    private val authService: AuthService,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeProfile()
    }

    val intent = ProfileIntent(
        onSignOut = { signOut() },
        onDisconnectPartner = { disconnectPartner() }
    )

    private fun observeProfile() {
        val currentId = authService.currentUser?.id ?: return
        viewModelScope.launch {
            userRepository.getUserProfile(currentId).collect { profile ->
                _uiState.update { 
                    it.copy(
                        user = authService.currentUser, // Keeps base info
                        partner = profile?.partnerId?.let { partnerId ->
                             PartnerInfo("Parceiro ($partnerId)") // Placeholder name
                        }
                    )
                }
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            authService.signOut()
        }
    }

    private fun disconnectPartner() {
        // TODO: Implementar lógica real de desvincular parceiro
        _uiState.update { it.copy(partner = null) }
    }
}
