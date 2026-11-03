package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.extensions.trackAction
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsElements
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.exception.RecentLoginRequiredException
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.ClearCacheUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.DeleteAccountUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.DeleteAllDataUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateAnalyticsEnabledUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.DataSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.DataSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class DataSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val authService: AuthService,
    private val settingsRepository: SettingsRepository,
    private val updateAnalyticsEnabledUseCase: UpdateAnalyticsEnabledUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataSettingsUiState())
    val uiState: StateFlow<DataSettingsUiState> = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    val intent = DataSettingsIntent(
        onClearCache = { clearCache() },
        onDeleteAllData = { deleteAllData() },
        onDeleteAccount = { deleteAccount() },
        onToggleAnalytics = { enabled -> updateAnalyticsEnabled(enabled) },
        onReauthConfirmed = { signOutToReauthenticate() },
        onReauthDismissed = { _uiState.update { it.copy(reauthRequired = false) } },
        onBack = { _navigator?.goBack() },
        onMessageShown = { _uiState.update { it.copy(message = null) } }
    )

    init {
        settingsRepository.getSettings()
            .onEach { settings -> _uiState.update { it.copy(analyticsEnabled = settings.analyticsEnabled) } }
            .launchIn(viewModelScope)
    }

    private fun updateAnalyticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            /** Desligando, o evento sai antes: depois do opt-out o tracker não envia mais nada. */
            if (!enabled) trackConsentChanged(enabled = false)
            updateAnalyticsEnabledUseCase(enabled)
                .onSuccess {
                    /** Ligando, a coleta volta antes do evento, senão ele seria descartado. */
                    analyticsTracker.setCollectionEnabled(enabled)
                    if (enabled) trackConsentChanged(enabled = true)
                }
                .onFailure { Timber.w(it, "Falha ao gravar a escolha de analytics") }
        }
    }

    private fun trackConsentChanged(enabled: Boolean) {
        analyticsTracker.trackEvent(
            AnalyticsEvents.ANALYTICS_CONSENT_CHANGED,
            mapOf(AnalyticsParams.ENABLED to enabled)
        )
    }

    private fun clearCache() {
        analyticsTracker.trackAction(AnalyticsElements.DATA_SETTINGS_CLEAR_CACHE)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val message = clearCacheUseCase()
                .fold(
                    onSuccess = { context.getString(R.string.settings_data_clear_cache_success) },
                    onFailure = { context.getString(R.string.settings_data_clear_cache_error) }
                )
            _uiState.update { it.copy(isLoading = false, message = message) }
        }
    }

    private fun deleteAllData() {
        analyticsTracker.trackEvent(AnalyticsEvents.DATA_CLEARED)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val message = deleteAllDataUseCase()
                .fold(
                    onSuccess = { context.getString(R.string.settings_data_delete_all_success) },
                    onFailure = { context.getString(R.string.settings_data_delete_all_error) }
                )
            _uiState.update { it.copy(isLoading = false, message = message) }
        }
    }

    private fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            deleteAccountUseCase()
                .onSuccess {
                    analyticsTracker.trackEvent(AnalyticsEvents.ACCOUNT_DELETED)
                    // This screen is torn down once the auth state flips to signed-out and the app
                    // lands on Welcome, so the confirmation has to outlive it.
                    showToast(R.string.settings_data_delete_account_success)
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        if (error is RecentLoginRequiredException) {
                            it.copy(isLoading = false, reauthRequired = true)
                        } else {
                            it.copy(
                                isLoading = false,
                                message = context.getString(R.string.settings_data_delete_account_error)
                            )
                        }
                    }
                }
        }
    }

    // Reauth fallback: sign out so the next login is fresh; the app then routes to Welcome.
    private fun signOutToReauthenticate() {
        viewModelScope.launch {
            _uiState.update { it.copy(reauthRequired = false) }
            authService.signOut()
            showToast(R.string.settings_data_delete_account_reauth_toast)
        }
    }

    private fun showToast(resId: Int) {
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_LONG).show()
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }
}
