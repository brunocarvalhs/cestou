package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ShareShoppingUseCase
import br.com.brunocarvalhs.howmuch.feature.products.navigation.QrCodeProductsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ShareOptionsViewModel @Inject constructor(
    private val shareShoppingUseCase: ShareShoppingUseCase
) : ViewModel() {

    private var _navigator: Navigator? = null

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    fun onShareAsText(shopping: Shopping) {
        viewModelScope.launch {
            shareShoppingUseCase(shopping)
            _navigator?.goBack()
        }
    }

    fun onInviteMember(shopping: Shopping) {
        val token = shopping.shortCode ?: shopping.id
        _navigator?.goBack()
        _navigator?.navigate(QrCodeProductsRoute(token))
    }
}
