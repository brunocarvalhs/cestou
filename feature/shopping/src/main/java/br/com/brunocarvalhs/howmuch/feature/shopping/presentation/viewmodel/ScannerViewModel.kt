package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingJoinUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.ScannerIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val JOIN_METHOD_QR_SCAN = "qr_scan"

@HiltViewModel
internal class ScannerViewModel @Inject constructor(
    private val shoppingJoinUseCase: ShoppingJoinUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private var _navigator: Navigator? = null

    val intent = ScannerIntent(
        onTokenScanned = { token -> onTokenScanned(token) },
        onDismiss = { _navigator?.goBack() }
    )

    init {
        analyticsTracker.trackScreenView(screenName = "shopping_join_scanner", screenClass = "ScannerViewModel")
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    private fun onTokenScanned(token: String) {
        viewModelScope.launch {
            shoppingJoinUseCase(token)
                .onSuccess {
                    analyticsTracker.trackEvent(
                        AnalyticsEvents.SHOPPING_LIST_JOINED,
                        mapOf(AnalyticsParams.JOIN_METHOD to JOIN_METHOD_QR_SCAN)
                    )
                    _navigator?.goBack()
                }
                .onFailure { error ->
                    analyticsTracker.trackEvent(
                        AnalyticsEvents.SHOPPING_LIST_JOIN_FAILED,
                        mapOf(
                            AnalyticsParams.JOIN_METHOD to JOIN_METHOD_QR_SCAN,
                            AnalyticsParams.REASON to (error.message ?: error::class.simpleName.orEmpty())
                        )
                    )
                }
        }
    }
}
