package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.CommonProduct
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.CommonProductAddAllToShoppingUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.CommonProductAddUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.CommonProductGetAllUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.CommonProductRemoveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.presentation.intent.CommonProductIntent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.CommonProductUiState
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ProductPickerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
internal class CommonProductViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val getAllUseCase: CommonProductGetAllUseCase,
    private val addUseCase: CommonProductAddUseCase,
    private val removeUseCase: CommonProductRemoveUseCase,
    private val addAllToShoppingUseCase: CommonProductAddAllToShoppingUseCase,
    private val productSaveUseCase: ProductSaveUseCase
) : ViewModel() {
    private val shopping = savedStateHandle.toRoute<ProductPickerRoute>(ProductPickerRoute.typeMap).shopping

    private val _uiState = MutableStateFlow(CommonProductUiState())
    val uiState = _uiState.asStateFlow()

    val intent = CommonProductIntent(
        onNewItemNameChange = { name -> _uiState.update { it.copy(newItemName = name) } },
        onAddItem = { addItem() },
        onRemoveItem = { id -> removeItem(id) },
        onAddToShopping = { item -> addToShopping(item) },
        onAddAllToShopping = { addAllToShopping() },
        onMessageShown = { _uiState.update { it.copy(message = null) } }
    )

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            getAllUseCase().collect { items ->
                _uiState.update { it.copy(isLoading = false, items = items) }
            }
        }
    }

    private fun addItem() {
        val name = _uiState.value.newItemName.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            addUseCase(name = name)
            _uiState.update { it.copy(newItemName = "") }
        }
    }

    private fun removeItem(id: String) {
        viewModelScope.launch { removeUseCase(id) }
    }

    private fun addToShopping(item: CommonProduct) {
        viewModelScope.launch {
            productSaveUseCase(
                product = Product(
                    id = UUID.randomUUID().toString(),
                    name = item.name,
                    quantity = 1.0,
                    price = 0.0,
                    category = item.category,
                ),
                shoppingId = shopping.id
            )
            _uiState.update {
                it.copy(message = context.getString(R.string.common_products_item_added, item.name))
            }
        }
    }

    private fun addAllToShopping() {
        val items = _uiState.value.items
        if (items.isEmpty()) return

        viewModelScope.launch {
            addAllToShoppingUseCase(shopping.id)
            _uiState.update {
                it.copy(message = context.getString(R.string.common_products_all_added))
            }
        }
    }
}
