package br.com.brunocarvalhs.howmuch.app.modules.finalizePurchase

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.domain.usecases.cart.FinalizePurchaseUseCase
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.FinalizePurchaseRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FinalizePurchaseViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val finalizePurchaseUseCase: FinalizePurchaseUseCase,
) : ViewModel() {

    private val args = savedStateHandle.toRoute<FinalizePurchaseRoute>()

    private val _uiState = MutableStateFlow<FinalizePurchaseUiState>(FinalizePurchaseUiState.Idle)
    val uiState: StateFlow<FinalizePurchaseUiState> = _uiState.asStateFlow()

    fun onIntent(intent: FinalizePurchaseUiIntent) = when (intent) {
        is FinalizePurchaseUiIntent.FinalizePurchase -> finalizePurchase(
            cartId = args.cartId,
            market = intent.name,
            totalPrice = intent.price
        )
    }

    private fun finalizePurchase(
        cartId: String?,
        market: String,
        totalPrice: Long
    ) = viewModelScope.launch {
        _uiState.emit(FinalizePurchaseUiState.Loading)
        cartId?.let { id ->
            finalizePurchaseUseCase(id, market = market, price = totalPrice)
                .onSuccess {
                    Toast.makeText(context, "Compra finalizada com sucesso!", Toast.LENGTH_SHORT)
                        .show()
                    _uiState.emit(FinalizePurchaseUiState.Success("Compra finalizada com sucesso!"))
                }.onFailure {
                    Toast.makeText(context, "Falha ao finalizar a compra", Toast.LENGTH_SHORT)
                        .show()
                    _uiState.emit(FinalizePurchaseUiState.Error("Falha ao finalizar a compra"))
                }
        } ?: run {
            _uiState.emit(FinalizePurchaseUiState.Error("Carrinho não encontrado"))
            Toast.makeText(context, "Carrinho não encontrado", Toast.LENGTH_SHORT).show()
        }
    }
}
