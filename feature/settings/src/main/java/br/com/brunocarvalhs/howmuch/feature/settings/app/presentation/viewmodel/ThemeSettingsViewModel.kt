package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.GetSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.UpdateThemeUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent.ThemeSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state.ThemeSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            }.catch { e -> throw e }.launchIn(viewModelScope)
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
