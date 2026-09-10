package br.com.brunocarvalhs.howmuch.feature.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
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
        val localUser = authService.currentUser ?: return
        _uiState.update { it.copy(user = localUser) }
        viewModelScope.launch {
            userRepository.getUserProfile(localUser.id).collect { profile ->
                _uiState.update { state -> state.copy(user = state.user.mergeWith(profile)) }
            }
        }
    }

    /**
     * Reconciles the locally cached [AuthenticatedUser] (Firebase Auth) with the Firestore
     * [UserProfile] emission. The Firestore document is the editable, multi-device source of
     * truth for name/email/photo (it's what settings screens write to), so its non-null fields
     * win; `phoneNumber`/`id` only exist on the auth user and are always preserved from it.
     */
    private fun AuthenticatedUser?.mergeWith(profile: UserProfile?): AuthenticatedUser? {
        val base = this ?: authService.currentUser ?: return null
        if (profile == null) return base
        return base.copy(
            displayName = profile.name ?: base.displayName,
            email = profile.email ?: base.email,
            photoUrl = profile.photoUrl ?: base.photoUrl
        )
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
