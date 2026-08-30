package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class FinishPurchaseViewModel @Inject constructor(
    private val repository: ShoppingRepository
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
            repository.update(updatedShopping)
            _navigator?.goBack()
        }
    }
}
