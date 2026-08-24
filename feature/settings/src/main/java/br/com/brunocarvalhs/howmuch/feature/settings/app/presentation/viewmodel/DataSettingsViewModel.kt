package br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.ClearCacheUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.DeleteAllDataUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.intent.DataSettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.app.presentation.state.DataSettingsUiState
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
    private val deleteAllDataUseCase: DeleteAllDataUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataSettingsUiState())
    val uiState: StateFlow<DataSettingsUiState> = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    val intent = DataSettingsIntent(
        onClearCache = { clearCache() },
        onDeleteAllData = { deleteAllData() },
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

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }
}
