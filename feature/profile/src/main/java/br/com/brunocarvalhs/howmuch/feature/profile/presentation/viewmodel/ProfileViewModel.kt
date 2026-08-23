package br.com.brunocarvalhs.howmuch.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.intent.ProfileIntent
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.state.ProfileUiState
import br.com.brunocarvalhs.howmuch.feature.settings.commons.navigation.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    private val authService: AuthService,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    init {
        observeProfile()
    }

    val intent = ProfileIntent(
        // Any menu option opens the main Settings screen; sub-routes are internal to feature:settings.
        onNavigate = { _navigator?.navigate(Settings) },
        onSignOut = { signOut() }
    )

    private fun observeProfile() {
        val currentId = authService.currentUser?.id ?: return
        viewModelScope.launch {
            userRepository.getUserProfile(currentId).collect {
                _uiState.update { it.copy(user = authService.currentUser) }
            }
        }
    }

    private fun signOut() {
        viewModelScope.launch {
            authService.signOut()
        }
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }
}
