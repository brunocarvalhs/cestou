package br.com.brunocarvalhs.howmuch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val authService: AuthService,
    private val userRepository: UserRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    init {
        analyticsTracker.trackEvent(AnalyticsEvents.APP_OPEN)
        syncUserProfile()
    }

    /**
     * Keeps users/{uid} (name + photo other list members see) in step with the signed-in account,
     * for every sign-in method. Also backfills accounts created before the profile was ever written.
     */
    private fun syncUserProfile() {
        viewModelScope.launch {
            authService.authState
                .map { user ->
                    user?.takeUnless { it.displayName.isNullOrBlank() }
                        ?.let { UserProfile(id = it.id, name = it.displayName, photoUrl = it.photoUrl) }
                }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { profile ->
                    userRepository.saveProfile(profile)
                        .onFailure { Timber.w(it, "Falha ao sincronizar users/%s", profile.id) }
                }
        }
    }

    /** Signed in but without a name: must go through the required-name step (spec EPA-06). */
    val requiresName: StateFlow<Boolean> = authService.authState
        .map { it.requiresName() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authService.currentUser.requiresName()
        )

    /** Synchronous read for navigation decisions, so a name saved a moment ago is never missed. */
    fun requiresNameNow(): Boolean = authService.currentUser.requiresName()

    private fun AuthenticatedUser?.requiresName() = this != null && displayName.isNullOrBlank()

    val isAuthenticated: StateFlow<Boolean> = authService.authState
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authService.currentUser != null
        )
    private val settings = settingsRepository.getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    val themeMode: StateFlow<ThemeMode> = settings
        .map { it.themeMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val language: StateFlow<String> = settings
        .map { it.language }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "pt-BR"
        )

    val photoUrl: StateFlow<String?> = authService.authState
        .map { it?.photoUrl }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authService.currentUser?.photoUrl
        )
}
