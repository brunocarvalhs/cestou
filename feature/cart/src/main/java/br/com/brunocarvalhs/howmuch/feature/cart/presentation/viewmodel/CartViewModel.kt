package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.extensions.orEmpty
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.CartFlow
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ShoppingClearPurchasedUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.SortProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ProductPickerRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.ConfirmItemRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.EditItemRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.FinishPurchaseRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.ProductHistoryRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.ShareOptionsRoute
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.intent.CartIntent
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.CartUiState
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.GetSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class CartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShoppingRepository,
    private val useCase: ProductsUseCase,
    private val clearPurchasedUseCase: ShoppingClearPurchasedUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val sortProductsUseCase: SortProductsUseCase,
    private val userRepository: UserRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val shopping = savedStateHandle.toRoute<CartFlow>(CartFlow.typeMap).shopping

    private val _uiState = MutableStateFlow(
        CartUiState(
            shopping = shopping
        )
    )
    val uiState = _uiState.asStateFlow()
    private var _navigator: Navigator? = null

    // Tracks which member ids already have a profile resolution in flight/done, so a re-emission
    // of `shopping`/`products` (e.g. a price edit) never re-resolves a member already cached in
    // CartUiState.memberProfiles (Tech Lead performance requirement, design.md).
    private val resolvedMemberIds = mutableSetOf<String>()

    val intent = CartIntent(
        onRefresh = { /* Handled by observe */ },
        onToggleProductPicker = {
            _uiState.value.shopping?.let {
                _navigator?.navigate(ProductPickerRoute(it))
            }
        },
        onShareShopping = { _navigator?.navigate(ShareOptionsRoute) },
        onDeleteProduct = { product -> deleteProduct(product) },
        onEditProduct = { product -> _navigator?.navigate(EditItemRoute(product, shopping.id)) },
        onUpdateQuantity = { product, quantity -> updateQuantity(product, quantity) },
        onTogglePurchased = { product, isPurchased -> togglePurchased(product, isPurchased) },
        onToggleFinishPurchaseSheet = {
            analyticsTracker.trackEvent(
                AnalyticsEvents.CART_FINISH_PURCHASE_STARTED,
                mapOf(AnalyticsParams.SHOPPING_ID to shopping.id)
            )
            _navigator?.navigate(FinishPurchaseRoute)
        },
        onClearPurchased = { clearPurchased() },
        onShowShareOptions = { _navigator?.navigate(ShareOptionsRoute) },
        onMoveProduct = { product, targetId -> moveProduct(product, targetId) },
        onShowProductHistory = { product -> _navigator?.navigate(ProductHistoryRoute(product)) }
    )

    init {
        analyticsTracker.trackScreenView(screenName = "cart", screenClass = "CartViewModel")
        observeData()
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    private fun observeData() {
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(sortingMode = settings.sortingMode) }
                observeProducts()
            }
        }
        viewModelScope.launch {
            repository.observeById(shopping.id).collect { updatedShopping ->
                updatedShopping?.let { shopping ->
                    _uiState.update { it.copy(shopping = shopping) }
                    resolveMemberProfiles()
                }
            }
        }
        viewModelScope.launch {
            repository.observeAll().collect { lists ->
                _uiState.update { it.copy(allShoppings = StableList(lists.filter { it.id != shopping.id })) }
            }
        }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            useCase(shopping.id).collect { products ->
                val sortedProducts = sortProductsUseCase(products, _uiState.value.sortingMode)
                _uiState.update { it.copy(products = StableList(sortedProducts)) }
                resolveMemberProfiles()
            }
        }
    }

    /**
     * Resolves, once per distinct id, every member id referenced by the current list: the
     * shopping's `users` plus every `Product.history` entry's `userId` across all products.
     * Never called per-row — `ProductListItem`/`CartProductItem` only read the resulting
     * `CartUiState.memberProfiles` map. Ids already resolved (or already in flight) are skipped
     * on subsequent calls, so a products/shopping re-emission doesn't re-fetch known members.
     */
    private fun resolveMemberProfiles() {
        val state = _uiState.value
        val memberIds = buildSet {
            state.shopping?.users?.let(::addAll)
            state.products.forEach { product ->
                product.history.forEach { add(it.userId) }
            }
        }
        val newIds = memberIds - resolvedMemberIds
        if (newIds.isEmpty()) return
        resolvedMemberIds += newIds

        newIds.forEach { id ->
            viewModelScope.launch {
                val profile = userRepository.getUserProfile(id).firstOrNull()
                if (profile != null) {
                    _uiState.update { it.copy(memberProfiles = it.memberProfiles + (id to profile)) }
                }
            }
        }
    }

    private fun clearPurchased() {
        viewModelScope.launch {
            clearPurchasedUseCase(shopping.id)
        }
    }

    private fun togglePurchased(
        product: Product,
        isPurchased: Boolean
    ) {
        if (isPurchased) {
            if (product.price.orEmpty() <= 0) {
                _navigator?.navigate(ConfirmItemRoute(product, shopping.id))
            } else {
                viewModelScope.launch {
                    useCase.update(product.copy(isPurchased = true), shopping.id)
                }
            }
        } else {
            viewModelScope.launch {
                useCase.update(product.copy(isPurchased = false), shopping.id)
            }
        }
    }

    private fun deleteProduct(product: Product) {
        viewModelScope.launch {
            useCase.delete(product.id, shopping.id)
            analyticsTracker.trackEvent(
                AnalyticsEvents.CART_PRODUCT_DELETED,
                mapOf(AnalyticsParams.SHOPPING_ID to shopping.id, AnalyticsParams.PRODUCT_ID to product.id)
            )
        }
    }

    private fun moveProduct(product: Product, targetId: String) {
        viewModelScope.launch {
            useCase.move(product, shopping.id, targetId)
        }
    }

    private fun updateQuantity(product: Product, quantity: Double) {
        if (quantity <= 0.0) {
            deleteProduct(product)
            return
        }
        viewModelScope.launch {
            useCase.update(product.copy(quantity = quantity), shopping.id)
        }
    }
}
