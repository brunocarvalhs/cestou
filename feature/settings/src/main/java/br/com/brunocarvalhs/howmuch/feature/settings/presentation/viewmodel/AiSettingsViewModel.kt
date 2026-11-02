package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.common.BuildConfig
import br.com.brunocarvalhs.howmuch.core.domain.model.AiConfigTier
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiModelRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Paywall
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateAiSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.AiSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.AiSettingsUiState
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
    private val settingsRepository: SettingsRepository,
    private val aiModelRepository: AiModelRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val updateAiSettingsUseCase: UpdateAiSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiSettingsUiState())
    val uiState: StateFlow<AiSettingsUiState> = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    val intent = AiSettingsIntent(
        onUpdateAiSettings = { tier, model, creativity, customApiKey ->
            updateAiSettings(tier, model, creativity, customApiKey)
        },
        onOpenPaywall = {
            _navigator?.navigate(Paywall(source = "ai_settings"))
        },
        onBack = { _navigator?.goBack() }
    )

    init {
        observeSettings()
        observeSubscription()
        loadModels()
    }

    private fun observeSettings() {
        settingsRepository.getSettings()
            .onEach { settings ->
                _uiState.update { state ->
                    val modelToUse = if (settings.aiModel.isBlank() && state.availableModels.isNotEmpty()) {
                        state.availableModels.firstOrNull()?.id ?: BuildConfig.OPEN_ROUTER_MODEL
                    } else {
                        settings.aiModel
                    }
                    state.copy(
                        aiConfigTier = settings.aiConfigTier,
                        aiModel = modelToUse,
                        customApiKey = settings.customApiKey ?: "",
                        creativityLevel = settings.creativityLevel
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSubscription() {
        subscriptionRepository.observeStatus()
            .onEach { status ->
                _uiState.update { it.copy(subscriptionStatus = status) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModels = true) }
            val models = aiModelRepository.getAvailableModels()
            val freeModels = models.filter { it.isFree }
            _uiState.update { state ->
                val currentModel = state.aiModel
                val modelToUse = if (currentModel.isBlank() && freeModels.isNotEmpty()) {
                    freeModels.first().id
                } else {
                    currentModel.ifBlank { BuildConfig.OPEN_ROUTER_MODEL }
                }
                state.copy(
                    availableModels = freeModels,
                    aiModel = modelToUse,
                    isLoadingModels = false
                )
            }
        }
    }

    private fun updateAiSettings(
        tier: AiConfigTier,
        model: String,
        creativity: Float,
        customApiKey: String?
    ) {
        viewModelScope.launch {
            updateAiSettingsUseCase(tier, model, creativity, customApiKey)
        }
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }
}
