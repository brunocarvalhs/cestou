package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.auth.app.domain.usecase.AuthConfigUseCase
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.intent.LoginIntent
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.state.LoginUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
internal class LoginViewModel @Inject constructor(
    private val authService: AuthService,
    val authConfig: AuthConfigUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    val intent = LoginIntent(
        onGoogleLogin = { /* Handled in AuthGraph */ },
        onAppleLogin = { /* Handled in AuthGraph */ },
        onBack = { }
    )

    fun onSignInFailure(exception: Exception) {
        _uiState.update { it.copy(error = exception.message, isLoading = false) }
    }

    fun onSignInStarted() {
        _uiState.update { it.copy(isLoading = true, error = null) }
    }
}
