package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
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
    private val wearableSyncService: WearableSyncService
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

    private fun observeAuthToken() {
        viewModelScope.launch {
            wearableSyncService.receivedAuthToken.collectLatest { token ->
                if (token.isNotEmpty()) {
                    _uiState.update { it.copy(isLinked = true) }
                }
            }
        }
    }
}
