package br.com.brunocarvalhs.howmuch.app.modules.historyDetail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.domain.entities.ShoppingCart
import br.com.brunocarvalhs.domain.usecases.history.CreateShoppingListFromHistoryUseCase
import br.com.brunocarvalhs.domain.usecases.history.DeleteOneCartOfHistoryUseCase
import br.com.brunocarvalhs.domain.usecases.history.GetCartByIdUseCase
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.DateFormat
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.shareText
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.toCurrencyString
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.toFormatDate
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.HistoryDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCartByIdUseCase: GetCartByIdUseCase,
    private val createShoppingListFromHistoryUseCase: CreateShoppingListFromHistoryUseCase,
    private val deleteCartUseCase: DeleteOneCartOfHistoryUseCase
) : ViewModel() {

    private val args = savedStateHandle.toRoute<HistoryDetailRoute>()

    private val _uiState = MutableStateFlow(HistoryDetailUiState())
    val uiState: StateFlow<HistoryDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            fetchCart(args.cartId)
        }
    }

    fun onIntent(intent: HistoryDetailUiIntent) {
        when (intent) {
            is HistoryDetailUiIntent.FetchCart -> fetchCart(args.cartId)
            is HistoryDetailUiIntent.CreateListFromHistory -> createListFromHistory(intent.cart)
            is HistoryDetailUiIntent.ShareCart -> sharedCart(intent.context, intent.cart)
            is HistoryDetailUiIntent.Delete -> deleteCart(intent.cart)
        }
    }

    private fun fetchCart(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            getCartByIdUseCase(id).onSuccess {
                _uiState.value = _uiState.value.copy(isLoading = false, cart = it)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
            }
        }
    }

    private fun createListFromHistory(cart: ShoppingCart) {
        viewModelScope.launch {
            createShoppingListFromHistoryUseCase(cart).onSuccess {

            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message)
            }
        }
    }

    private fun sharedCart(context: Context, cart: ShoppingCart) {
        val shareText = StringBuilder().apply {
            append("Shopping Cart: ${cart.market}\n")
            append("Date: ${cart.purchaseDate?.toFormatDate(DateFormat.DAY_MONTH_YEAR)}\n")
            append("Total: R$ ${cart.totalPrice.toCurrencyString()}\n")
            append("\nItems:\n")
            cart.products.forEach { item ->
                append("- ${item.name}: ${item.quantity}")
                append(" x R$ ${item.price?.toCurrencyString()} ")
                append("= R$ ${(item.quantity * (item.price ?: 0)).toCurrencyString()}\n")
            }
        }.toString()
        context.shareText(shareText, "Shopping Cart from ${cart.market}")
    }

    private fun deleteCart(cart: ShoppingCart) {
        viewModelScope.launch {
            deleteCartUseCase.invoke(cart).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isDeleting = true,
                    error = null,
                    cart = null
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(error = it.message)
            }
        }
    }
}
