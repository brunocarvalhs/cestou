package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.ShareShoppingUseCase
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.QrCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ShareOptionsViewModel @Inject constructor(
    private val shareShoppingUseCase: ShareShoppingUseCase,
    private val shoppingRepository: ShoppingRepository
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
        viewModelScope.launch {
            // Before goBack(): popping this sheet clears the ViewModel and cancels the write.
            runCatching { shoppingRepository.ensureShortCodeIndex(shopping) }
            _navigator?.goBack()
            _navigator?.navigate(route = QrCode(token))
        }
    }
}
