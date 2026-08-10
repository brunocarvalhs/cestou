package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ShoppingUpdateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class FinishPurchaseViewModel @Inject constructor(
    private val shoppingUpdateUseCase: ShoppingUpdateUseCase
) : ViewModel() {

    private var _navigator: Navigator? = null

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    fun onFinishPurchase(shopping: Shopping, price: Double, establishment: String) {
        viewModelScope.launch {
            val updatedShopping = shopping.copy(
                price = price,
                description = establishment,
                status = Shopping.Status.FINISH
            )
            shoppingUpdateUseCase(shopping.id, updatedShopping).onSuccess {
                _navigator?.goBack()
            }
        }
    }
}
