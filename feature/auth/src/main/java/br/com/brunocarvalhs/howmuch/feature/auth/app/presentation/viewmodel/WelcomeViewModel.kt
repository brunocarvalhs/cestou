package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import br.com.brunocarvalhs.howmuch.feature.auth.app.domain.usecase.AuthConfigUseCase
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.state.WelcomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
internal class WelcomeViewModel @Inject constructor(
    val authConfig: AuthConfigUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState = _uiState.asStateFlow()

    fun onSignInFailure(exception: Exception) {

    }
}
