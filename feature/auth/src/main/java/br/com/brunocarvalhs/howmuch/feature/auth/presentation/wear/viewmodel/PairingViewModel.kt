package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.state.PairingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PairingViewModel @Inject constructor(
    private val wearableSyncService: WearableSyncService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        generateCode()
        observeAuthToken()
    }

    private fun generateCode() {
        val code = wearableSyncService.generatePairingCode()
        _uiState.update { it.copy(code = code) }
    }

    // Linked only once the watch's own Firebase sign-in succeeded (WearAuthListenerService),
    // not when the pairing message arrives: the rules need a real session (AD-009).
    private fun observeAuthToken() {
        viewModelScope.launch {
            authService.authState.collectLatest { user ->
                _uiState.update { it.copy(isLinked = user != null) }
            }
        }
    }
}
