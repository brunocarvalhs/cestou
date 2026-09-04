package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.withActivity
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.EditItemRoute
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class EditItemViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val useCase: ProductsUseCase,
    private val authService: AuthService
) : ViewModel() {

    private val shoppingId = savedStateHandle.toRoute<EditItemRoute>(EditItemRoute.typeMap).shoppingId
    private var _navigator: Navigator? = null

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    fun onSaveEdit(product: Product, price: Double?, quantity: Double) {
        viewModelScope.launch {
            val userId = authService.getOrCreateUserId().id
            useCase.update(
                shoppingId = shoppingId,
                product = product.copy(price = price, quantity = quantity)
                    .withActivity(ProductActivity.Action.EDITED, userId),
            ).onSuccess {
                _navigator?.goBack()
            }
        }
    }
}
