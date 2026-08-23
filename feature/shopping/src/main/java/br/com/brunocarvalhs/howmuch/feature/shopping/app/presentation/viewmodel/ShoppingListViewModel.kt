package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.User
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.extensions.toMonthYearString
import br.com.brunocarvalhs.howmuch.core.navigation.JoinList
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.cart.navigation.CartFlow
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.exception.OwnershipRequiredException
import br.com.brunocarvalhs.howmuch.feature.settings.app.domain.usecase.GetSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShareShoppingUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingCreateUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingDeleteUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingDuplicateUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingGetAllUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingGetByIdUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingReopenUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingUpdateUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.commons.navigation.EditShopping
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.intent.ShoppingListIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state.ShoppingFilter
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.state.ShoppingListUiState
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
    private val shoppingReopenUseCase: ShoppingReopenUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val authService: AuthService,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    private var closeConfirmation = false
    private var _navigator: Navigator? = null

    val intent = ShoppingListIntent(
        onFetchAll = { fetchAll() },
        onCreate = { _uiState.update { it.copy(isCreateSheetVisible = true) } },
        onOpen = { id ->
            viewModelScope.launch {
                shoppingGetByIdUseCase.invoke(id).onSuccess { navigateToProducts(it) }
            }
        },
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
        onToggleFavorite = { shopping ->
            handleShoppingAction(ShoppingAction.ToggleFavorite(shopping))
        },
        onDuplicate = { shopping ->
            handleShoppingAction(ShoppingAction.Duplicate(shopping))
        },
        onShare = { shopping ->
            handleShoppingAction(ShoppingAction.Share(shopping))
        },
        onDelete = { id -> handleShoppingAction(ShoppingAction.Delete(id)) },
        onEdit = { shopping ->
            val userId = authService.currentUser?.id
            if (shopping.roles[userId] == User.Role.OWNER.name) {
                _navigator?.navigate(EditShopping(shopping))
            } else {
                _uiState.update {
                    it.copy(
                        error = UiText.StringResource(
                            R.string.shopping_management_error_only_owner_edit
                        )
                    )
                }
            }
        },
        onUpdate = { shopping ->
            handleShoppingAction(ShoppingAction.Update(shopping))
        },
        onReopen = { shopping ->
            handleShoppingAction(ShoppingAction.Reopen(shopping))
        },
        onShowJoinDialog = { _navigator?.navigate(JoinList) },
        onMove = { from, to -> moveShopping(from, to) },
        onShowCreateSheet = { visible -> _uiState.update { it.copy(isCreateSheetVisible = visible) } },
        onCreateConfirmed = { title, description ->
            _uiState.update { it.copy(isCreateSheetVisible = false) }
            viewModelScope.launch {
                shoppingCreateUseCase.invoke(title = title, description = description)
                    .onSuccess {
                        analyticsTracker.trackEvent(
                            AnalyticsEvents.SHOPPING_LIST_CREATED,
                            mapOf(AnalyticsParams.SHOPPING_ID to it.id)
                        )
                        navigateToProducts(it)
                    }
            }
        }
    )

    init {
        analyticsTracker.trackScreenView(screenName = "shopping_list", screenClass = "ShoppingListViewModel")
        observeData()
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    private fun observeData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            repository.observeAll().collect { list ->
                _uiState.update { it.copy(list = StableList(list), isLoading = false) }
                applyFilters()
            }
        }
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _uiState.update { it.copy(sortingMode = settings.sortingMode) }
                applyFilters()
            }
        }
    }

    private fun handleShoppingAction(action: ShoppingAction) {
        viewModelScope.launch {
            val result = when (action) {
                is ShoppingAction.Delete -> {
                    val shopping = repository.getById(action.id)
                    val userId = authService.currentUser?.id
                    if (shopping?.roles?.get(userId) == User.Role.OWNER.name) {
                        shoppingDeleteUseCase(action.id).onSuccess {
                            analyticsTracker.trackEvent(
                                AnalyticsEvents.SHOPPING_LIST_DELETED,
                                mapOf(AnalyticsParams.SHOPPING_ID to action.id)
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                error = UiText.StringResource(
                                    R.string.shopping_management_error_only_owner_delete
                                )
                            )
                        }
                        Result.failure(OwnershipRequiredException("delete"))
                    }
                }
                is ShoppingAction.Share -> {
                    shareShoppingUseCase(action.shopping)
                    analyticsTracker.trackEvent(
                        AnalyticsEvents.SHOPPING_LIST_SHARED,
                        mapOf(AnalyticsParams.SHOPPING_ID to action.shopping.id)
                    )
                    Result.success(Unit)
                }
                is ShoppingAction.Duplicate -> shoppingDuplicateUseCase(action.shopping)
                is ShoppingAction.ToggleFavorite -> {
                    val shopping = action.shopping
                    val updated = shopping.copy(isFavorite = !shopping.isFavorite)
                    shoppingUpdateUseCase(shopping.id, updated)
                }
                is ShoppingAction.Update -> shoppingUpdateUseCase(action.shopping.id, action.shopping)
                is ShoppingAction.Reopen -> shoppingReopenUseCase(action.shopping)
            }
            result.onSuccess {
                if (action !is ShoppingAction.Share) fetchAll()
            }
        }
    }

    private fun applyFilters() {
        val query = _uiState.value.searchQuery.lowercase()
        val filter = _uiState.value.selectedFilter

        val filtered = _uiState.value.list.items.filter { shopping ->
            val matchesQuery = shopping.title.lowercase().contains(query) ||
                shopping.description.lowercase().contains(query)

            val matchesFilter = when (filter) {
                ShoppingFilter.SHOPPING -> shopping.status != Shopping.Status.FINISH
                ShoppingFilter.FAVORITES -> shopping.isFavorite
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

    private fun navigateToProducts(shopping: Shopping) {
        _navigator?.navigate(route = CartFlow(shopping))
    }

    private fun moveShopping(fromIndex: Int, toIndex: Int) {
        val list = _uiState.value.filteredList.items.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return

        val movedItem = list.removeAt(fromIndex)
        list.add(toIndex, movedItem)

        val updatedList = list.mapIndexed { index, shopping ->
            shopping.copy(position = index)
        }
        _uiState.update { it.copy(filteredList = StableList(updatedList)) }

        viewModelScope.launch {
            repository.updatePositions(updatedList)
        }
    }

    private sealed class ShoppingAction {
        data class Delete(val id: String) : ShoppingAction()
        data class Share(val shopping: Shopping) : ShoppingAction()
        data class Duplicate(val shopping: Shopping) : ShoppingAction()
        data class ToggleFavorite(val shopping: Shopping) : ShoppingAction()
        data class Update(val shopping: Shopping) : ShoppingAction()
        data class Reopen(val shopping: Shopping) : ShoppingAction()
    }
}
