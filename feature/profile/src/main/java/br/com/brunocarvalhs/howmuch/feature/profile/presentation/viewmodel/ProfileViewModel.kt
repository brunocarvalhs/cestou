package br.com.brunocarvalhs.howmuch.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.LinkWearDevice
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.PairingCode
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.intent.ProfileIntent
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.state.ProfileUiState
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    init {
        analyticsTracker.trackScreenView(screenName = "profile", screenClass = "ProfileViewModel")
        observeProfile()
    }

    val intent = ProfileIntent(
        // Any menu option opens the main Settings screen; sub-routes are internal to feature:settings.
        onNavigate = { _navigator?.navigate(Settings) },
        onSignOut = { signOut() },
        onLinkWearDevice = { _navigator?.navigate(LinkWearDevice) },
        onLinkMobileDevice = { _navigator?.navigate(PairingCode) }
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
            analyticsTracker.trackEvent(AnalyticsEvents.PROFILE_SIGN_OUT)
        }
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }
}
