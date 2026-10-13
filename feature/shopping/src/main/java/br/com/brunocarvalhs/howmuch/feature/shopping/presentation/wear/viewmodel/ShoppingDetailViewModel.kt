package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.withActivity
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.wear.ShoppingDetail
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearListStore
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearListSummary
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.ShoppingDetailIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.state.ShoppingDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * The watch's list screen: live list + items, tap to (un)mark, add by voice or keyboard.
 * Every change is also saved as the "last list" summary that the tile and the next launch use.
 */
@HiltViewModel
internal class ShoppingDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val authService: AuthService,
    private val wearListStore: WearListStore
) : ViewModel() {

    private val shoppingId = savedStateHandle.toRoute<ShoppingDetail>().shoppingId
    private var _navigator: Navigator? = null

    private val _uiState = MutableStateFlow(ShoppingDetailUiState(isLoading = true))
    val uiState: StateFlow<ShoppingDetailUiState> = _uiState.asStateFlow()

    val intent = ShoppingDetailIntent(
        onBack = { _navigator?.goBack() },
        onToggle = ::toggle,
        onAddItem = ::addItem
    )

    init {
        observeList()
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    private fun observeList() {
        viewModelScope.launch {
            // Listeners, not one-shot gets: they answer from Firestore's cache right away offline.
            combine(
                shoppingRepository.observeById(shoppingId),
                flow { emitAll(productsUseCase(shoppingId)) }
            ) { shopping, products -> shopping to products }
                .catch { emit(null to emptyList()) }
                .collect { (shopping, products) ->
                    if (shopping == null) {
                        forgetList()
                    } else {
                        _uiState.value = shopping.toUiState(products)
                        wearListStore.save(shopping.toSummary(products))
                    }
                }
        }
    }

    // Deleted, or the user left it (the rules then deny the read): stop reopening it.
    private suspend fun forgetList() {
        if (wearListStore.lastList()?.shoppingId == shoppingId) wearListStore.clear()
        _navigator?.goBack()
    }

    private fun toggle(product: Product) {
        val userId = authService.currentUser?.id
        val updated = product.copy(isPurchased = !product.isPurchased).let {
            if (it.isPurchased && userId != null) it.withActivity(ProductActivity.Action.PURCHASED, userId) else it
        }
        // ponytail: no price prompt on the watch; an unpriced item is just marked and adds 0 to the total.
        viewModelScope.launch { productsUseCase.update(updated, shoppingId) }
    }

    private fun addItem(text: String) {
        val name = text.trim().replaceFirstChar { it.uppercase() }
        if (name.isEmpty()) return
        val product = Product(id = UUID.randomUUID().toString(), name = name, quantity = 1.0).let { new ->
            authService.currentUser?.id?.let { new.withActivity(ProductActivity.Action.ADDED, it) } ?: new
        }
        viewModelScope.launch { productsUseCase.save(product, shoppingId) }
    }

    private fun Shopping.toUiState(products: List<Product>) = ShoppingDetailUiState(
        title = title,
        emoji = emoji,
        total = products.sumOf { it.total },
        budget = budget?.takeIf { it > 0.0 },
        pending = products.count { !it.isPurchased },
        items = StableList(products.sortedBy { it.isPurchased }),
        isLoading = false
    )

    private fun Shopping.toSummary(products: List<Product>) = WearListSummary(
        shoppingId = id,
        title = title,
        emoji = emoji,
        total = products.sumOf { it.total },
        budget = budget?.takeIf { it > 0.0 },
        pending = products.count { !it.isPurchased },
        itemCount = products.size
    )
}
