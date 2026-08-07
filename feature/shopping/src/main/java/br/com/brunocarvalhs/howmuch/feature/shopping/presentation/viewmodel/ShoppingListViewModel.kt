package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.ChatMessage
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.CartAssistantUseCase
import br.com.brunocarvalhs.howmuch.feature.products.navigation.ProductsFlow
import br.com.brunocarvalhs.howmuch.feature.products.presentation.state.AiDockState
import br.com.brunocarvalhs.howmuch.core.domain.service.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.entity.User
import br.com.brunocarvalhs.howmuch.core.extensions.toMonthYearString
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.GetSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.*
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.*
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.ShoppingListIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.ShoppingListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ShoppingListViewModel @Inject constructor(
    private val repository: ShoppingRepository,
    private val shoppingGetAllUseCase: ShoppingGetAllUseCase,
    private val shoppingGetByIdUseCase: ShoppingGetByIdUseCase,
    private val shoppingCreateUseCase: ShoppingCreateUseCase,
    private val shoppingUpdateUseCase: ShoppingUpdateUseCase,
    private val shoppingDuplicateUseCase: ShoppingDuplicateUseCase,
    private val shoppingDeleteUseCase: ShoppingDeleteUseCase,
    private val shareShoppingUseCase: ShareShoppingUseCase,
    private val assistantUseCase: CartAssistantUseCase,
    private val shoppingReopenUseCase: ShoppingReopenUseCase,
    private val getShoppingSuggestionsUseCase: GetShoppingSuggestionsUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    private var closeConfirmation = false
    private var _navigator: Navigator? = null

    val intent = ShoppingListIntent(
        onFetchAll = { fetchAll() },
        onCreate = { create() },
        onOpen = { id -> open(id) },
        onPromptChanged = { value -> _uiState.update { it.copy(prompt = value) } },
        onFilter = { value -> 
            _uiState.update { it.copy(selectedFilter = value) }
            applyFilters()
        },
        onQueryChange = { value -> 
            _uiState.update { it.copy(searchQuery = value) }
            applyFilters()
        },
        onSearch = { value -> 
            _uiState.update { it.copy(searchQuery = value) }
            applyFilters()
        },
        onSendPrompt = { sendPrompt() },
        onToggleAi = { toggleAi() },
        onOpenAi = { 
            _uiState.update { it.copy(aiDockState = AiDockState.EXPANDED) }
            loadAiSuggestions()
        },
        onCloseAi = { _uiState.update { it.copy(aiDockState = AiDockState.COLLAPSED) } },
        onToggleFavorite = { shopping -> toggleFavorite(shopping) },
        onDuplicate = { shopping -> duplicate(shopping) },
        onShare = { shopping -> share(shopping) },
        onDelete = { id -> 
            viewModelScope.launch {
                val shopping = repository.getById(id)
                if (isOwner(shopping)) {
                    delete(id)
                } else {
                    _uiState.update { it.copy(error = UiText.StringResource(R.string.shopping_management_error_only_owner_delete)) }
                }
            }
        },
        onEdit = { shopping -> 
            if (isOwner(shopping)) {
                _navigator?.navigate(EditShopping(shopping))
            } else {
                _uiState.update { it.copy(error = UiText.StringResource(R.string.shopping_management_error_only_owner_edit)) }
            }
        },
        onUpdate = { shopping -> update(shopping) },
        onReopen = { shopping -> reopen(shopping) },
        onShowJoinDialog = { _navigator?.navigate(JoinList) },
        onMove = { from, to -> moveShopping(from, to) },
        onShowCreateSheet = { visible -> _uiState.update { it.copy(isCreateSheetVisible = visible) } },
        onCreateConfirmed = { title, description -> createConfirmed(title, description) },
        onSuggestionClick = { suggestion -> 
            _uiState.update { it.copy(prompt = suggestion) }
            sendPrompt()
        }
    )

    init {
        observeAll()
        observeSettings()
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    private fun observeSettings() {
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(sortingMode = settings.sortingMode) }
                applyFilters()
            }
        }
    }

    private fun observeAll() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.observeAll().collect { list ->
                _uiState.update { it.copy(list = StableList(list), isLoading = false) }
                applyFilters()
            }
        }
    }

    private fun isOwner(shopping: Shopping?): Boolean {
        val userId = authService.currentUser?.id ?: return false
        return shopping?.roles?.get(userId) == User.Role.OWNER.name
    }

    private fun loadAiSuggestions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiSuggestionsLoading = true) }
            getShoppingSuggestionsUseCase().collect { suggestions ->
                _uiState.update { 
                    it.copy(
                        aiSuggestions = StableList(suggestions),
                        isAiSuggestionsLoading = false
                    ) 
                }
            }
        }
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            shoppingDeleteUseCase(id).onSuccess {
                fetchAll()
            }
        }
    }

    private fun share(shopping: Shopping) {
        viewModelScope.launch {
            shareShoppingUseCase(shopping)
        }
    }

    private fun duplicate(shopping: Shopping) {
        viewModelScope.launch {
            shoppingDuplicateUseCase(shopping).onSuccess {
                fetchAll()
            }
        }
    }

    private fun toggleFavorite(shopping: Shopping) {
        viewModelScope.launch {
            val updated = shopping.copy(isFavorite = !shopping.isFavorite)
            shoppingUpdateUseCase(shopping.id, updated).onSuccess {
                fetchAll()
            }
        }
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.lowercase()
        val filter = _uiState.value.selectedFilter

        val filtered = _uiState.value.list.filter { shopping ->
            val matchesQuery = shopping.title.lowercase().contains(query) ||
                    shopping.description.lowercase().contains(query)

            val matchesFilter = when (filter) {
                "Compras" -> shopping.status != Shopping.Status.FINISH
                "Favoritos" -> shopping.isFavorite
                else -> true
            }

            matchesQuery && matchesFilter
        }.sortedWith(
            if (_uiState.value.sortingMode == "NAME") {
                compareBy { it.title.lowercase() }
            } else {
                compareByDescending { it.createdAt }
            }
        )

        val grouped = filtered.groupBy { it.createdAt.toMonthYearString() }
            .mapValues { StableList(it.value) }

        _uiState.update { it.copy(filteredList = StableList(filtered), groupedList = grouped) }
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
            } catch (_: kotlinx.coroutines.CancellationException) {
                _uiState.update { it.copy(isAiLoading = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isAiLoading = false) }
            }
        }
    }

    private fun toggleAi() {
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
                it.copy(
                    aiDockState = AiDockState.EXPANDED
                )
            }
            return
        }
        closeConfirmation = false
        _uiState.update {
            it.copy(
                aiDockState = AiDockState.COLLAPSED
            )
        }
    }

    private fun fetchAll() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            shoppingGetAllUseCase.invoke()
                .onSuccess { list ->
                    _uiState.update { it.copy(list = StableList(list), isLoading = false) }
                    applyFilters()
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    private fun create() {
        _uiState.update { it.copy(isCreateSheetVisible = true) }
    }

    private fun createConfirmed(title: String, description: String) {
        _uiState.update { it.copy(isCreateSheetVisible = false) }
        viewModelScope.launch {
            shoppingCreateUseCase.invoke(title = title, description = description)
                .onSuccess { data ->
                    _navigator?.navigate(route = ProductsFlow(data))
                }.onFailure {

                }
        }
    }

    private fun open(id: String) {
        viewModelScope.launch {
            shoppingGetByIdUseCase.invoke(id)
                .onSuccess {
                    _navigator?.navigate(route = ProductsFlow(it))
                }.onFailure {

                }
        }
    }

    private fun update(shopping: Shopping) {
        viewModelScope.launch {
            shoppingUpdateUseCase(shopping.id, shopping).onSuccess {
                fetchAll()
            }
        }
    }

    private fun reopen(shopping: Shopping) {
        viewModelScope.launch {
            shoppingReopenUseCase(shopping).onSuccess {
                fetchAll()
            }
        }
    }

    private fun moveShopping(fromIndex: Int, toIndex: Int) {
        val list = _uiState.value.filteredList.items.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return

        val movedItem = list.removeAt(fromIndex)
        list.add(toIndex, movedItem)

        // Update positions locally first for immediate feedback
        val updatedList = list.mapIndexed { index, shopping ->
            shopping.copy(position = index)
        }
        _uiState.update { it.copy(filteredList = StableList(updatedList)) }

        // Persist to repository
        viewModelScope.launch {
            repository.updatePositions(updatedList)
        }
    }
}
