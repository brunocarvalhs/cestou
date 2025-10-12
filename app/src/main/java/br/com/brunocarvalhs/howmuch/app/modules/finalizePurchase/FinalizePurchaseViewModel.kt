package br.com.brunocarvalhs.howmuch.app.modules.finalizePurchase

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class FinalizePurchaseViewModel @Inject constructor(): ViewModel() {
    private val _uiState = MutableStateFlow(FinalizePurchaseUiState())
    val uiState: StateFlow<FinalizePurchaseUiState> = _uiState.asStateFlow()

    fun onIntent(intent: FinalizePurchaseUiIntent) {

    }

}