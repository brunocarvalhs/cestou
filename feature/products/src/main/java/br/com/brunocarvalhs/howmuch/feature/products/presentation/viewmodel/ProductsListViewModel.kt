package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.CartAssistantUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.GetQuestionSuggestionsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ShoppingClearPurchasedUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.SortProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ConfirmItemRoute
import br.com.brunocarvalhs.howmuch.feature.products.navigation.EditItemRoute
import br.com.brunocarvalhs.howmuch.feature.products.navigation.FinishPurchaseRoute
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ProductPickerRoute
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ProductsFlow
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ShareOptionsRoute
import br.com.brunocarvalhs.howmuch.feature.products.presentation.intent.ProductsListIntent
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.AiDockState
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.ProductsListUiState
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.GetSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ProductsListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ShoppingRepository,
    private val useCase: ProductsUseCase,
    private val assistantUseCase: CartAssistantUseCase,
    private val getQuestionSuggestionsUseCase: GetQuestionSuggestionsUseCase,
    private val clearPurchasedUseCase: ShoppingClearPurchasedUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val sortProductsUseCase: SortProductsUseCase
) : ViewModel() {

    private val shopping = savedStateHandle.toRoute<ProductsFlow>(ProductsFlow.typeMap).shopping

    private val _uiState = MutableStateFlow(
        ProductsListUiState(
            shopping = shopping
        )
    )
    val uiState = _uiState.asStateFlow()
    private var closeConfirmation = false
    private var _navigator: Navigator? = null

    val intent = ProductsListIntent(
        onRefresh = { /* Handled by observe */ },
        onToggleProductPicker = {
            _uiState.value.shopping?.let {
                _navigator?.navigate(ProductPickerRoute(it))
            }
        },
        onShareShopping = { _navigator?.navigate(ShareOptionsRoute) },
        onPromptChanged = { value -> _uiState.update { it.copy(prompt = value) } },
        onOpenAi = {
            _uiState.update { it.copy(aiDockState = AiDockState.EXPANDED) }
            loadAiSuggestions()
        },
        onCloseAi = { _uiState.update { it.copy(aiDockState = AiDockState.COLLAPSED) } },
        onSendPrompt = { sendPrompt() },
        onToggleAi = { toggleAi() },
        onDeleteProduct = { product -> deleteProduct(product) },
        onEditProduct = { product -> _navigator?.navigate(EditItemRoute(product, shopping.id)) },
        onUpdateQuantity = { product, quantity -> updateQuantity(product, quantity) },
        onTogglePurchased = { product, isPurchased -> togglePurchased(product, isPurchased) },
        onToggleFinishPurchaseSheet = { _navigator?.navigate(FinishPurchaseRoute) },
        onClearPurchased = { clearPurchased() },
        onShowShareOptions = { _navigator?.navigate(ShareOptionsRoute) },
        onSuggestionClick = { question ->
            _uiState.update { it.copy(prompt = question) }
            sendPrompt()
        },
        onMoveProduct = { product, targetId -> moveProduct(product, targetId) }
    )

    init {
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
            }
        }
    }

    private fun clearPurchased() {
        viewModelScope.launch {
            clearPurchasedUseCase(shopping.id)
        }
    }

    private fun togglePurchased(product: br.com.brunocarvalhs.howmuch.core.domain.entity.Product, isPurchased: Boolean) {
        if (isPurchased) {
            if (product.price <= 0) {
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

    private fun deleteProduct(product: br.com.brunocarvalhs.howmuch.core.domain.entity.Product) {
        viewModelScope.launch {
            useCase.delete(product.id, shopping.id)
        }
    }

    private fun moveProduct(product: br.com.brunocarvalhs.howmuch.core.domain.entity.Product, targetId: String) {
        viewModelScope.launch {
            useCase.move(product, shopping.id, targetId)
        }
    }

    private fun updateQuantity(product: br.com.brunocarvalhs.howmuch.core.domain.entity.Product, quantity: Double) {
        if (quantity <= 0.0) {
            deleteProduct(product)
            return
        }
        viewModelScope.launch {
            useCase.update(product.copy(quantity = quantity), shopping.id)
        }
    }

    private fun sendPrompt() {
        val text = _uiState.value.prompt
        if (text.isBlank()) return

        _uiState.update {
            it.copy(
                prompt = "",
                aiDockState = AiDockState.CHAT,
                aiMessages = StableList(it.aiMessages + ChatMessage(
                    text = text,
                    sender = ChatMessage.Sender.USER
                )),
                isAiLoading = true
            )
        }

        viewModelScope.launch {
            try {
                assistantUseCase(text, _uiState.value).collect { response ->
                    _uiState.update {
                        it.copy(
                            aiMessages = StableList(it.aiMessages + ChatMessage(
                                text = response,
                                sender = ChatMessage.Sender.ASSISTANT
                            )),
                            isAiLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update { it.copy(isAiLoading = false) }
            }
        }
    }

    private fun toggleAi() {
        if (_uiState.value.aiDockState == AiDockState.COLLAPSED) {
            loadAiSuggestions()
        }

        if (_uiState.value.aiMessages.isEmpty()) {
            _uiState.update {
                it.copy(
                    aiDockState =
                    if (it.aiDockState == AiDockState.COLLAPSED)
                        AiDockState.EXPANDED
                    else
                        AiDockState.COLLAPSED
                )
            }
            return
        }

        if (!closeConfirmation) {
            closeConfirmation = true
            _uiState.update {
                it.copy(aiDockState = AiDockState.EXPANDED)
            }
            return
        }
        closeConfirmation = false
        _uiState.update {
            it.copy(aiDockState = AiDockState.COLLAPSED)
        }
    }

    private fun loadAiSuggestions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiSuggestionsLoading = true) }
            getQuestionSuggestionsUseCase(shopping.id).collect { questions ->
                _uiState.update {
                    it.copy(
                        aiSuggestions = StableList(questions),
                        isAiSuggestionsLoading = false
                    )
                }
            }
        }
    }
}
