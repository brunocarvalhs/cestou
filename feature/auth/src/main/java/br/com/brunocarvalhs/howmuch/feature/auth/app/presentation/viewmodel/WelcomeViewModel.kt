package br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.intent.WelcomeIntent
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.state.WelcomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
internal class WelcomeViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState = _uiState.asStateFlow()

    var onNavigateToLogin: () -> Unit = {}

    val intent = WelcomeIntent(
        onStart = { onNavigateToLogin() }
    )
}
