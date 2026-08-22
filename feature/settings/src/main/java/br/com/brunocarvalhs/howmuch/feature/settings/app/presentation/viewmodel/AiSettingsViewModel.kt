package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.GetSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.UpdateAiSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent.AiSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state.AiSettingsUiState
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
internal class AiSettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateAiSettingsUseCase: UpdateAiSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiSettingsUiState())
    val uiState: StateFlow<AiSettingsUiState> = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    val intent = AiSettingsIntent(
        onUpdateAiSettings = { model, prompt, creativity -> updateAiSettings(model, prompt, creativity) },
        onBack = { _navigator?.goBack() }
    )

    init {
        observeSettings()
    }

    private fun observeSettings() {
        getSettingsUseCase()
            .onEach { settings ->
                _uiState.update { it.copy(
                    aiModel = settings.aiModel,
                    customPrompt = settings.customPrompt,
                    creativityLevel = settings.creativityLevel
                ) }
            }
            .launchIn(viewModelScope)
    }

    private fun updateAiSettings(model: String, prompt: String?, creativity: Float) {
        viewModelScope.launch {
            updateAiSettingsUseCase(model, prompt, creativity)
        }
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }
}
