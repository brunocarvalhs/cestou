package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingJoinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ScannerViewModel @Inject constructor(
    private val shoppingJoinUseCase: ShoppingJoinUseCase
) : ViewModel() {

    private var _navigator: Navigator? = null

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    fun onTokenScanned(token: String) {
        viewModelScope.launch {
            shoppingJoinUseCase(token)
                .onSuccess {
                    _navigator?.goBack()
                }
        }
    }
}
