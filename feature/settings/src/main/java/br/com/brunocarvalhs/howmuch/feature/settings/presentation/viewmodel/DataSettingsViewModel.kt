package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.exception.RecentLoginRequiredException
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.ClearCacheUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.DeleteAccountUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.DeleteAllDataUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.DataSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.DataSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DataSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val deleteAllDataUseCase: DeleteAllDataUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataSettingsUiState())
    val uiState: StateFlow<DataSettingsUiState> = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    val intent = DataSettingsIntent(
        onClearCache = { clearCache() },
        onDeleteAllData = { deleteAllData() },
        onDeleteAccount = { deleteAccount() },
        onReauthConfirmed = { signOutToReauthenticate() },
        onReauthDismissed = { _uiState.update { it.copy(reauthRequired = false) } },
        onBack = { _navigator?.goBack() },
        onMessageShown = { _uiState.update { it.copy(message = null) } }
    )

    private fun clearCache() {
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
