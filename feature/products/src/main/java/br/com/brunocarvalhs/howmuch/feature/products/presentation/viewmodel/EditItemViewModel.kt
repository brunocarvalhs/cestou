package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.entity.Product
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.navigation.EditItemRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class EditItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val useCase: ProductsUseCase
) : ViewModel() {

    private val shoppingId = savedStateHandle.toRoute<EditItemRoute>(EditItemRoute.typeMap).shoppingId
    private var _navigator: Navigator? = null

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    fun onSaveEdit(product: Product, price: Double, quantity: Double, unit: String) {
        viewModelScope.launch {
            useCase.update(product.copy(price = price, quantity = quantity, unit = unit), shoppingId).onSuccess {
                _navigator?.goBack()
            }
        }
    }
}
