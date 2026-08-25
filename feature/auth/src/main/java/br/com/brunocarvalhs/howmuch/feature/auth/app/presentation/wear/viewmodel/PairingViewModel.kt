package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.wear.state.PairingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PairingViewModel @Inject constructor(
    private val wearableSyncService: WearableSyncService,
    private val authService: AuthService
) : ViewModel() {

    private val _code = MutableStateFlow("")

    val uiState: StateFlow<PairingUiState> = combine(
        _code,
        authService.authState
    ) { code, authUser ->
        PairingUiState(
            code = code,
            isLinked = authUser != null && authUser.id != "guest"
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PairingUiState()
    )

    init {
        generateCode()
    }

    private fun generateCode() {
        val code = (100000..999999).random().toString()
        _code.value = code
        viewModelScope.launch {
            wearableSyncService.updatePairingCode(code)
        }
    }

    fun openPhoneApp() {
        viewModelScope.launch {
            wearableSyncService.openPhoneApp()
        }
    }
}
