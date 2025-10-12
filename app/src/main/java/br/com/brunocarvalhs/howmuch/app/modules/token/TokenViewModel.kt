package br.com.brunocarvalhs.howmuch.app.modules.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.domain.useCases.EnterShoppingCartWithTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TokenViewModel @Inject constructor(
    private val enterTokenWithTokenUseCase: EnterShoppingCartWithTokenUseCase
): ViewModel() {

    private val _uiState = MutableStateFlow(TokenUiState())
    val uiState: StateFlow<TokenUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<TokenUiEffect>()
    val uiEffect: SharedFlow<TokenUiEffect> = _uiEffect.asSharedFlow()

    fun onIntent(intent: TokenUiIntent) {
        when (intent) {
            is TokenUiIntent.SearchByToken -> searchByToken(token = intent.token)
        }
    }

    private fun searchByToken(token: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        val result = enterTokenWithTokenUseCase(token).getOrNull()
        if (result != null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                cartId = result.id,
                token = result.token
            )
        } else {
            _uiEffect.emit(TokenUiEffect.ShowError("Invalid token"))
        }
    }
}