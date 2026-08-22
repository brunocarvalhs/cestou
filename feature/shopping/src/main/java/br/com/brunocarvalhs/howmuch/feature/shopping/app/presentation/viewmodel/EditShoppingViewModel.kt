package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingUpdateUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.commons.navigation.EditShopping
import br.com.brunocarvalhs.howmuch.feature.shopping.commons.navigation.QrCode
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.intent.EditShoppingIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state.EditShoppingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class EditShoppingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShoppingRepository,
    private val shoppingUpdateUseCase: ShoppingUpdateUseCase
) : ViewModel() {

    private val initialShopping = savedStateHandle.toRoute<EditShopping>(EditShopping.typeMap).shopping
    private var _navigator: Navigator? = null

    private val _uiState = MutableStateFlow(EditShoppingUiState(shopping = initialShopping))
    val uiState: StateFlow<EditShoppingUiState> = _uiState.asStateFlow()

    val intent = EditShoppingIntent(
        onUpdate = { shopping -> update(shopping) },
        onShareToken = { id -> shareToken(id) },
        onCancel = { _navigator?.goBack() }
    )

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    private fun update(shopping: Shopping) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            shoppingUpdateUseCase(shopping.id, shopping)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigator?.goBack()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun shareToken(shoppingId: String) {
        viewModelScope.launch {
            val shopping = repository.getById(shoppingId)
            val token = shopping?.shortCode ?: shoppingId
            _navigator?.navigate(QrCode(token))
        }
    }
}
