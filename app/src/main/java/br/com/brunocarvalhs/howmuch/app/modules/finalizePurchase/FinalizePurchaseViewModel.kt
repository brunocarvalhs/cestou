package br.com.brunocarvalhs.howmuch.app.modules.finalizePurchase

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.domain.services.ICartLocalStorage
import br.com.brunocarvalhs.domain.usecases.cart.FinalizePurchaseUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class FinalizePurchaseViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val finalizePurchaseUseCase: FinalizePurchaseUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FinalizePurchaseUiState())
    val uiState: StateFlow<FinalizePurchaseUiState> = _uiState.asStateFlow()

    fun onIntent(intent: FinalizePurchaseUiIntent) = when(intent) {
        is FinalizePurchaseUiIntent.FinalizePurchase -> finalizePurchase(
            cartId = intent.cartId,
            market = intent.name,
            totalPrice = intent.price
        )
    }

    private fun finalizePurchase(
        cartId: String?,
        market: String,
        totalPrice: Long
    ) = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(isLoading = true)
        cartId?.let { id ->
            finalizePurchaseUseCase(id, market = market, price = totalPrice)
                .onSuccess {
                    Toast.makeText(context, "Compra finalizada com sucesso!", Toast.LENGTH_SHORT)
                        .show()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }.onFailure {
                    Log.e(
                        this.javaClass.simpleName,
                        "Failed to finalize purchase",
                        it
                    )
                    Toast.makeText(context, "Falha ao finalizar a compra", Toast.LENGTH_SHORT)
                        .show()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
        } ?: run {
            _uiState.value = _uiState.value.copy(isLoading = false)
            Toast.makeText(context, "Carrinho não encontrado", Toast.LENGTH_SHORT).show()
        }
    }
}