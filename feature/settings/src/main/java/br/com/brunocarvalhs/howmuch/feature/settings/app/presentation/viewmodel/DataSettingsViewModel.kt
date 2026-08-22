package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.ClearCacheUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.DeleteAllDataUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent.DataSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state.DataSettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DataSettingsViewModel @Inject constructor(
    private val clearCacheUseCase: ClearCacheUseCase,
    private val deleteAllDataUseCase: DeleteAllDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataSettingsUiState())
    val uiState: StateFlow<DataSettingsUiState> = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    val intent = DataSettingsIntent(
        onClearCache = { clearCache() },
        onDeleteAllData = { deleteAllData() },
        onBack = { _navigator?.goBack() }
    )

    private fun clearCache() {
        viewModelScope.launch {
            clearCacheUseCase()
        }
    }

    private fun deleteAllData() {
        viewModelScope.launch {
            deleteAllDataUseCase()
        }
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }
}
