package br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.intent.LinkWearIntent
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.state.LinkWearUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class LinkWearViewModel @Inject constructor(
    private val wearableSyncService: WearableSyncService,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LinkWearUiState())
    val uiState = _uiState.asStateFlow()

    val intent = LinkWearIntent(
        onCodeChange = { code ->
            _uiState.update { it.copy(code = code, error = null) }
        },
        onLinkClick = { linkDevice() }
    )

    private fun linkDevice() {
        val code = _uiState.value.code
        if (code.length != 6) {
            _uiState.update { it.copy(error = "Código inválido (6 dígitos)") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val nodeId = wearableSyncService.findNodeByPairingCode(code)
            if (nodeId != null) {
                val userId = authService.currentUser?.id ?: ""
                wearableSyncService.sendAuthTokenToNode(nodeId, userId)
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Dispositivo não encontrado") }
            }
        }
    }
}
