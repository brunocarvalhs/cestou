package br.com.brunocarvalhs.howmuch.app.modules.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.domain.usecases.cart.EnterShoppingCartWithTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TokenViewModel @Inject constructor(
    private val enterTokenWithTokenUseCase: EnterShoppingCartWithTokenUseCase
): ViewModel() {

    private val _uiState = MutableStateFlow<TokenUiState>(TokenUiState.Idle)
    val uiState: StateFlow<TokenUiState> = _uiState.asStateFlow()

    fun onIntent(intent: TokenUiIntent) {
        when (intent) {
            is TokenUiIntent.SearchByToken -> searchByToken(token = intent.token)
        }
    }

    private fun searchByToken(token: String) = viewModelScope.launch {
        _uiState.emit(TokenUiState.Loading)
        enterTokenWithTokenUseCase(token).onSuccess { result ->
            _uiState.emit(TokenUiState.Success(cartId = result.id))
        }.onFailure {
            _uiState.emit(TokenUiState.Error(message = "Error"))
        }
    }
}