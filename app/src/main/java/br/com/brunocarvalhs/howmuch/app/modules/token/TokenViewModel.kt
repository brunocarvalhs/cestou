package br.com.brunocarvalhs.howmuch.app.modules.token

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.domain.useCases.EnterShoppingCartWithTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TokenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val enterTokenWithTokenUseCase: EnterShoppingCartWithTokenUseCase
): ViewModel() {

    private val _uiState = MutableStateFlow(TokenUiState())
    val uiState: StateFlow<TokenUiState> = _uiState.asStateFlow()

    fun onIntent(intent: TokenUiIntent) {
        when (intent) {
            is TokenUiIntent.SearchByToken -> searchByToken(token = intent.token)
        }
    }

    private fun searchByToken(token: String) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        enterTokenWithTokenUseCase(token).onSuccess { result ->
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                cartId = result.id,
                token = result.token
            )
        }.onFailure {
            _uiState.value = _uiState.value.copy(isLoading = false)
            Toast.makeText(context, "Invalid token", Toast.LENGTH_SHORT).show()
        }
    }
}