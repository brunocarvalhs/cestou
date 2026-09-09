package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingJoinUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.ScannerIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ScannerViewModel @Inject constructor(
    private val shoppingJoinUseCase: ShoppingJoinUseCase
) : ViewModel() {

    private var _navigator: Navigator? = null

    // NOTE: BarcodeAnalyzer (feature/products, internal) fires onBarcodeScanned on every analyzed
    // camera frame with no throttle of its own — the debounce guard below is what keeps holding a
    // QR code in frame from re-triggering ShoppingJoinUseCase once per frame (see MVP-ROADMAP G13).
    // feature/shopping depending on that internal class is a separate cross-module coupling issue
    // (G10) and is intentionally not addressed here.
    private var isJoining = false

    val intent = ScannerIntent(
        onTokenScanned = { token -> onTokenScanned(token) },
        onDismiss = { _navigator?.goBack() }
    )

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    private fun onTokenScanned(token: String) {
        if (isJoining) return
        isJoining = true
        viewModelScope.launch {
            shoppingJoinUseCase(token)
                .onSuccess {
                    // Leave isJoining = true: the screen is navigating away, so no further scans
                    // should be processed for the lifetime of this ViewModel.
                    _navigator?.goBack()
                }
                .onFailure {
                    // Allow retrying with a different/re-aligned code after a failed join.
                    isJoining = false
                }
        }
    }
}
