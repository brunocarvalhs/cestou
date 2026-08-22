package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.GetSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.UpdateNotificationSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent.NotificationSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state.NotificationSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class NotificationSettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    val intent = NotificationSettingsIntent(
        onUpdateNotificationSettings = { enabled, time -> updateNotificationSettings(enabled, time) },
        onBack = { _navigator?.goBack() }
    )

    init {
        observeSettings()
    }

    private fun observeSettings() {
        getSettingsUseCase()
            .onEach { settings ->
                _uiState.update { it.copy(
                    notificationsEnabled = settings.notificationsEnabled,
                    reminderTime = settings.reminderTime
                ) }
            }
            .launchIn(viewModelScope)
    }

    private fun updateNotificationSettings(enabled: Boolean, reminderTime: String) {
        viewModelScope.launch {
            updateNotificationSettingsUseCase(enabled, reminderTime)
        }
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }
}
