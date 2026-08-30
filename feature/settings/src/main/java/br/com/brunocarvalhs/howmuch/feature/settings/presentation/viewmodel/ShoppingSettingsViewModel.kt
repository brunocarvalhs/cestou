package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.GetSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateShoppingPreferencesUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.ShoppingSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.ShoppingSettingsUiState
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
internal class ShoppingSettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateShoppingPreferencesUseCase: UpdateShoppingPreferencesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingSettingsUiState())
    val uiState: StateFlow<ShoppingSettingsUiState> = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    val intent = ShoppingSettingsIntent(
        onUpdateShoppingPreferences = { defaultListId, sortingMode, remindersEnabled ->
            updateShoppingPreferences(defaultListId, sortingMode, remindersEnabled)
        },
        onBack = { _navigator?.goBack() }
    )

    init {
        observeSettings()
    }

    private fun observeSettings() {
        getSettingsUseCase()
            .onEach { settings ->
                _uiState.update { it.copy(
                    defaultListId = settings.defaultListId,
                    sortingMode = settings.sortingMode,
                    remindersEnabled = settings.remindersEnabled
                ) }
            }
            .launchIn(viewModelScope)
    }

    private fun updateShoppingPreferences(defaultListId: String?, sortingMode: String, remindersEnabled: Boolean) {
        viewModelScope.launch {
            updateShoppingPreferencesUseCase(defaultListId, sortingMode, remindersEnabled)
        }
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }
}
