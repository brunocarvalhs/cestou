package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.JoinList
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

private const val JOIN_METHOD_DEEP_LINK = "deep_link"
private const val JOIN_METHOD_MANUAL_TOKEN = "manual_token"

@HiltViewModel
internal class JoinListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val shoppingJoinUseCase: ShoppingJoinUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private var _navigator: Navigator? = null

    private val initialToken = savedStateHandle.toRoute<JoinList>().token

    private val _uiState = MutableStateFlow(JoinListUiState(initialToken = initialToken))
    val uiState: StateFlow<JoinListUiState> = _uiState.asStateFlow()

    val intent = JoinListIntent(
        onJoinByToken = { token -> joinByToken(token, JOIN_METHOD_MANUAL_TOKEN) },
        onScanQrCode = {
            _navigator?.goBack()
            _navigator?.navigate(Scanner)
        },
        onDismiss = { _navigator?.goBack() }
    )

    init {
        analyticsTracker.trackScreenView(screenName = "shopping_join", screenClass = "JoinListViewModel")
        initialToken?.takeIf { it.isNotBlank() }?.let { joinByToken(it, JOIN_METHOD_DEEP_LINK) }
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    private fun joinByToken(token: String, method: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            shoppingJoinUseCase(token)
                .onSuccess {
                    analyticsTracker.trackEvent(
                        AnalyticsEvents.SHOPPING_LIST_JOINED,
                        mapOf(AnalyticsParams.JOIN_METHOD to method)
                    )
                    _uiState.update { it.copy(isLoading = false) }
                    _navigator?.goBack()
                }
                .onFailure { error ->
                    analyticsTracker.trackEvent(
                        AnalyticsEvents.SHOPPING_LIST_JOIN_FAILED,
                        mapOf(
                            AnalyticsParams.JOIN_METHOD to method,
                            AnalyticsParams.REASON to (error.message ?: error::class.simpleName.orEmpty())
                        )
                    )
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
