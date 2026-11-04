package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.extensions.trackAction
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsElements
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.analytics.service.FirstTimeEventTracker
import br.com.brunocarvalhs.howmuch.core.common.extensions.toMonthYearString
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetProgress
import br.com.brunocarvalhs.howmuch.core.domain.budget.MonthlySpendingCalculator
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.User
import br.com.brunocarvalhs.howmuch.core.domain.repository.MonthlyGoalRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.ShareShoppingUseCase
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.CartFlow
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.JoinList
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Notifications
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.FeatureFlagService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.FeatureFlagKeys
import br.com.brunocarvalhs.howmuch.core.ui.utils.StableList
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.exception.OwnershipRequiredException
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ObserveUnreadNotificationsCountUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingCreateUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingDeleteUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingDuplicateUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingGetAllUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingGetByIdUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingReopenUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingUpdateUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.EditShopping
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.ShoppingListIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.ShoppingFilter
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.ShoppingListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val settingsRepository: SettingsRepository,
    private val observeUnreadNotificationsCount: ObserveUnreadNotificationsCountUseCase,
    private val authService: AuthService,
    private val analyticsTracker: AnalyticsTracker,
    private val firstTimeEventTracker: FirstTimeEventTracker,
    private val monthlyGoalRepository: MonthlyGoalRepository,
    private val monthlySpendingCalculator: MonthlySpendingCalculator,
    private val featureFlags: FeatureFlagService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShoppingListUiState())
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    private var closeConfirmation = false

    /** Muda a cada volta à Home para o gasto do mês ser recalculado se o mês virou com o app aberto. */
    private val monthTick = MutableStateFlow(0)
    private var _navigator: Navigator? = null

    val intent = ShoppingListIntent(
        onFetchAll = { fetchAll() },
        onCreate = {
            analyticsTracker.trackAction(AnalyticsElements.SHOPPING_LIST_CREATE)
            _uiState.update { it.copy(isCreateSheetVisible = true) }
        },
        onOpen = { id ->
            analyticsTracker.trackEvent(
                AnalyticsEvents.SHOPPING_LIST_OPENED,
                mapOf(AnalyticsParams.SHOPPING_ID to id)
            )
            viewModelScope.launch {
                shoppingGetByIdUseCase.invoke(id).onSuccess { navigateToProducts(it) }
            }
        },
        onFilter = { value ->
            analyticsTracker.trackAction(AnalyticsElements.SHOPPING_LIST_FILTER)
            _uiState.update { it.copy(selectedFilter = value) }
            applyFilters()
        },
        onQueryChange = { value ->
            _uiState.update { it.copy(searchQuery = value) }
            applyFilters()
        },
        onSearch = { value ->
            analyticsTracker.trackEvent(
                AnalyticsEvents.SEARCH,
                mapOf(AnalyticsParams.QUERY_LENGTH to value.length, AnalyticsParams.SOURCE to "shopping_list")
            )
            _uiState.update { it.copy(searchQuery = value) }
            applyFilters()
        },
        onOpenAiChat = {
            analyticsTracker.trackEvent(
                AnalyticsEvents.AI_CHAT_OPENED,
                mapOf(AnalyticsParams.SOURCE to "shopping_list_fab")
            )
            _navigator?.navigate(AiChat(shoppingId = ""))
        },
        onToggleFavorite = { shopping ->
            analyticsTracker.trackAction(AnalyticsElements.SHOPPING_LIST_FAVORITE)
            handleShoppingAction(ShoppingAction.ToggleFavorite(shopping))
        },
        onDuplicate = { shopping ->
            analyticsTracker.trackAction(AnalyticsElements.SHOPPING_LIST_DUPLICATE)
            handleShoppingAction(ShoppingAction.Duplicate(shopping))
        },
        onShare = { shopping ->
            handleShoppingAction(ShoppingAction.Share(shopping))
        },
        onDelete = { id -> handleShoppingAction(ShoppingAction.Delete(id)) },
        onEdit = { shopping ->
            analyticsTracker.trackAction(AnalyticsElements.SHOPPING_LIST_EDIT)
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
            analyticsTracker.trackAction(AnalyticsElements.SHOPPING_LIST_FINISH)
            handleShoppingAction(ShoppingAction.Update(shopping))
        },
        onReopen = { shopping ->
            analyticsTracker.trackAction(AnalyticsElements.SHOPPING_LIST_REOPEN)
            handleShoppingAction(ShoppingAction.Reopen(shopping))
        },
        onShowJoinDialog = {
            analyticsTracker.trackAction(AnalyticsElements.SHOPPING_LIST_JOIN)
            _navigator?.navigate(JoinList())
        },
        onNotificationsClick = {
            analyticsTracker.trackEvent(
                AnalyticsEvents.NOTIFICATIONS_OPENED,
                mapOf(AnalyticsParams.UNREAD_COUNT to _uiState.value.unreadNotifications)
            )
            _navigator?.navigate(Notifications)
        },
        onMove = { from, to -> moveShopping(from, to) },
        onShowCreateSheet = { visible -> _uiState.update { it.copy(isCreateSheetVisible = visible) } },
        onCreateConfirmed = { title, description, emoji ->
            _uiState.update { it.copy(isCreateSheetVisible = false) }
            viewModelScope.launch {
                shoppingCreateUseCase.invoke(title = title, description = description, emoji = emoji)
                    .onSuccess {
                        analyticsTracker.trackEvent(
                            AnalyticsEvents.SHOPPING_LIST_CREATED,
                            mapOf(AnalyticsParams.SHOPPING_ID to it.id)
                        )
                        firstTimeEventTracker.trackOnce(AnalyticsEvents.FIRST_LIST_CREATED)
                        navigateToProducts(it)
                    }
                    .onFailure {
                        _uiState.update {
                            it.copy(
                                error = UiText.StringResource(
                                    R.string.shopping_management_error_create
                                )
                            )
                        }
                    }
            }
        }
    )

    init {
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
        viewModelScope.launch { observeMonthlyGoalProgress() }
        viewModelScope.launch {
            observeUnreadNotificationsCount().collect { count ->
                _uiState.update { it.copy(unreadNotifications = count) }
            }
        }
        viewModelScope.launch {
            settingsRepository.getSettings().collect { settings ->
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
                    shoppingUpdateUseCase(shopping.id, mapOf("isFavorite" to !shopping.isFavorite))
                }
                // Only ever dispatched with status = FINISH (quick-finish from the list row, see
                // ShoppingScreen's onFinishClick), so only that field is written.
                is ShoppingAction.Update -> shoppingUpdateUseCase(
                    action.shopping.id,
                    mapOf("status" to action.shopping.status.name)
                )
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
                ShoppingFilter.FAVORITES -> shopping.isFavorite
                ShoppingFilter.OPEN -> shopping.status != Shopping.Status.FINISH
                ShoppingFilter.GROUP -> shopping.users.size > 1 || shopping.roles.size > 1
                ShoppingFilter.FINISHED -> shopping.status == Shopping.Status.FINISH
                ShoppingFilter.ALL -> true
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

    /**
     * Meta mensal contra as compras finalizadas do mês, pelo mesmo `observeAll` da Home, sem novo
     * listener. Com a flag desligada o estado fica sem progresso e nada aparece.
     */
    private suspend fun observeMonthlyGoalProgress() {
        if (!featureFlags.isEnabled(FeatureFlagKeys.BUDGET_ALERTS_ENABLED, true)) return
        combine(repository.observeAll(), monthlyGoalRepository.observeGoal(), monthTick) { lists, goal, _ ->
            goal?.let { BudgetProgress(limit = it, spent = monthlySpendingCalculator.finishedSpent(lists)) }
        }
            .distinctUntilChanged()
            .collect { progress -> _uiState.update { it.copy(monthlyGoalProgress = progress) } }
    }

    private fun fetchAll() {
        monthTick.update { it + 1 }
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
            analyticsTracker.trackAction(AnalyticsElements.SHOPPING_LIST_MOVE)
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
