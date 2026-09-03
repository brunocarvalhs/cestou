package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.JoinList
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingJoinUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.mobile.Scanner
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.JoinListIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.JoinListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class JoinListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val shoppingJoinUseCase: ShoppingJoinUseCase
) : ViewModel() {

    private var _navigator: Navigator? = null

    private val initialToken = savedStateHandle.toRoute<JoinList>().token

    private val _uiState = MutableStateFlow(JoinListUiState(initialToken = initialToken))
    val uiState: StateFlow<JoinListUiState> = _uiState.asStateFlow()

    val intent = JoinListIntent(
        onJoinByToken = { token -> joinByToken(token) },
        onScanQrCode = {
            _navigator?.goBack()
            _navigator?.navigate(Scanner)
        },
        onDismiss = { _navigator?.goBack() }
    )

    init {
        initialToken?.takeIf { it.isNotBlank() }?.let { joinByToken(it) }
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    private fun joinByToken(token: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            shoppingJoinUseCase(token)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigator?.goBack()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false, 
                        error = if (error.message?.contains("invalid", true) == true) {
                            UiText.StringResource(R.string.shopping_management_join_error_invalid)
                        } else {
                            val message = error.message
                            if (message != null) {
                                UiText.DynamicString(message)
                            } else {
                                UiText.StringResource(R.string.shopping_management_error_join)
                            }
                        }
                    ) }
                }
        }
    }
}
