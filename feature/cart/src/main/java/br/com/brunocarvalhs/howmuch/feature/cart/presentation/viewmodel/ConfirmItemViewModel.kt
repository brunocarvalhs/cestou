package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.ConfirmItemRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ConfirmItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val useCase: ProductsUseCase
) : ViewModel() {

    private val shoppingId = savedStateHandle.toRoute<ConfirmItemRoute>(ConfirmItemRoute.typeMap).shoppingId
    private var _navigator: Navigator? = null

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    fun onConfirmPurchased(product: Product, price: Double, quantity: Double) {
        viewModelScope.launch {
            useCase.update(product.copy(isPurchased = true, price = price, quantity = quantity), shoppingId).onSuccess {
                _navigator?.goBack()
            }
        }
    }
}
