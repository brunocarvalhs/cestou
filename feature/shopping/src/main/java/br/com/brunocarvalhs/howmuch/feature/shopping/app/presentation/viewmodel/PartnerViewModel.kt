package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.intent.PartnerIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state.PartnerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PartnerViewModel @Inject constructor(
    private val authService: AuthService,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PartnerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observePartner()
    }

    val intent = PartnerIntent(
        onLinkPartner = { linkPartner(it) },
        onUnlinkPartner = { unlinkPartner() }
    )

    private fun observePartner() {
        val userId = authService.currentUser?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            userRepository.getUserProfile(userId).collect { profile ->
                val partnerId = profile?.partnerId
                if (partnerId != null) {
                    userRepository.getUserProfile(partnerId).collect { partnerProfile ->
                        _uiState.update { it.copy(partner = partnerProfile, isLoading = false) }
                    }
                } else {
                    _uiState.update { it.copy(partner = null, isLoading = false) }
                }
            }
        }
    }

    private fun linkPartner(partnerId: String) {
        viewModelScope.launch {
            userRepository.linkPartner(partnerId)
        }
    }

    private fun unlinkPartner() {
        viewModelScope.launch {
            userRepository.unlinkPartner()
        }
    }
}
