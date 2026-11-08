package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.extensions.trackAction
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsElements
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.budget.MonthlySpendingCalculator
import br.com.brunocarvalhs.howmuch.core.domain.extensions.orEmpty
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.repository.MonthlyGoalRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ShoppingClearPurchasedUseCase
import br.com.brunocarvalhs.howmuch.core.domain.usecase.SortProductsUseCase
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.CartFlow
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.PriceComparator
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.ConfirmItemRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.EditItemRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.FinishPurchaseRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.ProductHistoryRoute
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.ShareOptionsRoute
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.helper.BudgetAlertTracker
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.helper.BudgetInputs
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.helper.computeCartBudget
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.helper.computeCartTotals
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.intent.CartIntent
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.CartUiState
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ProductPickerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class CartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShoppingRepository,
    private val useCase: ProductsUseCase,
    private val clearPurchasedUseCase: ShoppingClearPurchasedUseCase,
    private val settingsRepository: SettingsRepository,
    private val sortProductsUseCase: SortProductsUseCase,
    private val userRepository: UserRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val monthlyGoalRepository: MonthlyGoalRepository,
    private val monthlySpendingCalculator: MonthlySpendingCalculator,
    private val featureFlags: FeatureFlagService
) : ViewModel() {

    private val shopping = savedStateHandle.toRoute<CartFlow>(CartFlow.typeMap).shopping

    private val _uiState = MutableStateFlow(
        CartUiState(
            shopping = shopping,
            realtimeCartEnabled = featureFlags.isEnabled(FeatureFlagKeys.REALTIME_CART_ENABLED, true),
            priceComparatorEnabled = featureFlags.isEnabled(FeatureFlagKeys.PRICE_COMPARATOR_ENABLED, true)
        )
    )
    val uiState = _uiState.asStateFlow()
    private var _navigator: Navigator? = null

    // Tracks which member ids already have a profile resolution in flight/done, so a re-emission
    // of `shopping`/`products` (e.g. a price edit) never re-resolves a member already cached in
    // CartUiState.memberProfiles (Tech Lead performance requirement, design.md).
    private val resolvedMemberIds = mutableSetOf<String>()

    /** Decide quando avisar o gasto; vive com esta abertura da lista, então reabrir recomeça a contagem. */
    private val budgetAlertTracker = BudgetAlertTracker()

    // Só avisa depois de produtos, outras listas e meta terem chegado: antes disso os totais estão
    // incompletos e o aviso seria falso. Setados antes da atualização do estado que reavalia.
    private var productsLoaded = false
    private var shoppingsLoaded = false
    private var goalLoaded = false

    val intent = CartIntent(
        onRefresh = { /* Handled by observe */ },
        onToggleProductPicker = {
            analyticsTracker.trackAction(AnalyticsElements.CART_ADD_PRODUCT, shoppingParams())
            _uiState.value.shopping?.let {
                _navigator?.navigate(ProductPickerRoute(it))
            }
        },
        onShareShopping = {
            analyticsTracker.trackAction(AnalyticsElements.CART_SHARE, shoppingParams())
            _navigator?.navigate(ShareOptionsRoute)
        },
        onDeleteProduct = { product -> deleteProduct(product) },
        onEditProduct = { product ->
            analyticsTracker.trackAction(AnalyticsElements.CART_EDIT_PRODUCT, shoppingParams())
            _navigator?.navigate(EditItemRoute(product, shopping.id))
        },
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
        onOpenPriceComparator = {
            analyticsTracker.trackAction(AnalyticsElements.CART_PRICE_COMPARATOR, shoppingParams())
            _navigator?.navigate(PriceComparator(shopping))
        },
        onBudgetAlertShown = {
            _uiState.value.budgetAlert?.let { alert ->
                analyticsTracker.trackEvent(
                    AnalyticsEvents.BUDGET_ALERT_SHOWN,
                    mapOf(
                        AnalyticsParams.SCOPE to alert.scope.name.lowercase(),
                        AnalyticsParams.LEVEL to alert.level.name.lowercase()
                    )
                )
            }
            _uiState.update { it.copy(budgetAlert = null) }
        },
        onShowShareOptions = {
            analyticsTracker.trackEvent(AnalyticsEvents.CART_SHARE_OPTIONS_OPENED, shoppingParams())
            _navigator?.navigate(ShareOptionsRoute)
        },
        onMoveProduct = { product, targetId -> moveProduct(product, targetId) },
        onShowProductHistory = { product ->
            analyticsTracker.trackEvent(AnalyticsEvents.CART_PRODUCT_HISTORY_OPENED, shoppingParams())
            _navigator?.navigate(ProductHistoryRoute(product))
        }
    )

    init {
        observeData()
        observeBudget()
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    private fun observeData() {
        viewModelScope.launch {
            // `flatMapLatest` cancels the previous `useCase(shopping.id)` product collector as soon
            // as a newer settings emission arrives, so a settings write (theme, language, AI prefs,
            // sorting mode) anywhere in the app never leaves a stale collector running alongside the
            // new one (see G12 in .specs/MVP-ROADMAP.md).
            settingsRepository.getSettings()
                .flatMapLatest { settings ->
                    _uiState.update { it.copy(sortingMode = settings.sortingMode) }
                    useCase(shopping.id).map { products -> settings.sortingMode to products }
                }
                .collect { (sortingMode, products) ->
                    val sortedProducts = sortProductsUseCase(products, sortingMode)
                    productsLoaded = true
                    val stableProducts = StableList(sortedProducts)
                    val totals = computeCartTotals(stableProducts)
                    val totalsChanged = totals != _uiState.value.totals
                    // A flag é relida a cada emissão, então uma mudança remota vale sem reabrir a lista.
                    val realtimeEnabled = featureFlags.isEnabled(FeatureFlagKeys.REALTIME_CART_ENABLED, true)
                    val comparatorEnabled = featureFlags.isEnabled(FeatureFlagKeys.PRICE_COMPARATOR_ENABLED, true)
                    _uiState.update {
                        it.copy(
                            products = stableProducts,
                            realtimeCartEnabled = realtimeEnabled,
                            priceComparatorEnabled = comparatorEnabled
                        )
                    }
                    // Só reporta quando os totais mudam: reordenar ou editar o nome não os altera.
                    if (totalsChanged && realtimeEnabled && stableProducts.isNotEmpty()) {
                        analyticsTracker.trackEvent(
                            AnalyticsEvents.CART_TOTAL_CALCULATED,
                            shoppingParams() + (AnalyticsParams.ITEMS_COUNT to stableProducts.size) +
                                (AnalyticsParams.UNPRICED_COUNT to totals.unpricedCount)
                        )
                    }
                    resolveMemberProfiles()
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
                shoppingsLoaded = true
                _uiState.update { it.copy(allShoppings = StableList(lists.filter { it.id != shopping.id })) }
            }
        }
    }

    /**
     * Total da lista contra o orçamento dela e, com meta, a projeção do mês. Reaproveita o
     * `observeAll` que o ViewModel já mantém aberto (`allShoppings`): nenhuma leitura nova.
     */
    private fun observeBudget() {
        if (!featureFlags.isEnabled(FeatureFlagKeys.BUDGET_ALERTS_ENABLED, true)) return
        viewModelScope.launch {
            combine(
                _uiState.map { BudgetInputs(it.shopping, it.products, it.allShoppings) }.distinctUntilChanged(),
                monthlyGoalRepository.observeGoal().onEach { goalLoaded = true }
            ) { inputs, goal -> computeCartBudget(inputs, goal, monthlySpendingCalculator) }
                .distinctUntilChanged()
                .collect { snapshot ->
                    val alert = budgetAlertTracker.onUpdate(
                        ready = productsLoaded && shoppingsLoaded && goalLoaded,
                        list = snapshot.list,
                        month = snapshot.month
                    )
                    _uiState.update {
                        it.copy(
                            listBudgetProgress = snapshot.list,
                            monthProgress = snapshot.month,
                            budgetAlert = alert ?: it.budgetAlert
                        )
                    }
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

    private fun shoppingParams() = mapOf(AnalyticsParams.SHOPPING_ID to shopping.id)

    private fun clearPurchased() {
        viewModelScope.launch {
            clearPurchasedUseCase(shopping.id)
            analyticsTracker.trackEvent(AnalyticsEvents.CART_PURCHASED_CLEARED, shoppingParams())
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
                    trackPurchasedToggled(purchased = true)
                }
            }
        } else {
            viewModelScope.launch {
                useCase.update(product.copy(isPurchased = false), shopping.id)
                trackPurchasedToggled(purchased = false)
            }
        }
    }

    private fun trackPurchasedToggled(purchased: Boolean) {
        analyticsTracker.trackEvent(
            AnalyticsEvents.CART_ITEM_CHECKED,
            shoppingParams() + (AnalyticsParams.ENABLED to purchased)
        )
        analyticsTracker.trackEvent(
            AnalyticsEvents.PRODUCT_PURCHASED_TOGGLED,
            shoppingParams() + (AnalyticsParams.ENABLED to purchased)
        )
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
            analyticsTracker.trackEvent(AnalyticsEvents.PRODUCT_MOVED, shoppingParams())
        }
    }

    private fun updateQuantity(product: Product, quantity: Double) {
        if (quantity <= 0.0) {
            deleteProduct(product)
            return
        }
        viewModelScope.launch {
            useCase.update(product.copy(quantity = quantity), shopping.id)
            analyticsTracker.trackEvent(AnalyticsEvents.PRODUCT_QUANTITY_CHANGED, shoppingParams())
        }
    }
}
