package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.domain.entity.ThemeMode
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.GetSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateThemeUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.ThemeSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.ThemeSettingsUiState
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
internal class ThemeSettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateThemeUseCase: UpdateThemeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ThemeSettingsUiState())
    val uiState: StateFlow<ThemeSettingsUiState> = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    val intent = ThemeSettingsIntent(
        onUpdateTheme = { themeMode -> updateTheme(themeMode) },
        onBack = { _navigator?.goBack() }
    )

    init {
        observeSettings()
    }

    private fun observeSettings() {
        getSettingsUseCase()
            .onEach { settings ->
                _uiState.update { it.copy(themeMode = settings.themeMode) }
            }
            .launchIn(viewModelScope)
    }

    private fun updateTheme(themeMode: ThemeMode) {
        viewModelScope.launch {
            updateThemeUseCase(themeMode)
        }
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }
}
