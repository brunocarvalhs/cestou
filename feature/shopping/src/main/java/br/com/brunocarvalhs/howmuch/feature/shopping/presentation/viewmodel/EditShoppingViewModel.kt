package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.QrCode
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingUpdateUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.mobile.EditShopping
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.intent.EditShoppingIntent
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.EditShoppingUiState
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
    private val shoppingUpdateUseCase: ShoppingUpdateUseCase,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val initialShopping = savedStateHandle.toRoute<EditShopping>(EditShopping.typeMap).shopping
    private var _navigator: Navigator? = null

    private val _uiState = MutableStateFlow(EditShoppingUiState(shopping = initialShopping))
    val uiState: StateFlow<EditShoppingUiState> = _uiState.asStateFlow()

    val intent = EditShoppingIntent(
        onUpdate = { shopping -> update(shopping) },
        onShareToken = { id -> shareToken(id) },
        onCancel = { _navigator?.goBack() },
        onErrorShown = { _uiState.update { it.copy(error = null) } }
    )

    init {
        analyticsTracker.trackScreenView(screenName = "shopping_edit", screenClass = "EditShoppingViewModel")
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    private fun update(shopping: Shopping) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            shoppingUpdateUseCase(shopping.id, shopping)
                .onSuccess {
                    trackBudgetChangeIfNeeded(shopping)
                    _uiState.update { it.copy(isLoading = false) }
                    _navigator?.goBack()
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    // Beta needs to know how many lists actually get a spending limit (PM checklist item
    // "definir limite de gastos"). Only fires when the budget value actually changed on this
    // save, so re-saving the same edit form doesn't inflate the funnel.
    private fun trackBudgetChangeIfNeeded(shopping: Shopping) {
        val budget = shopping.budget
        if (budget == initialShopping.budget) return
        analyticsTracker.trackEvent(
            AnalyticsEvents.SHOPPING_BUDGET_SET,
            mapOf(
                AnalyticsParams.SHOPPING_ID to shopping.id,
                AnalyticsParams.HAS_BUDGET to (budget != null && budget > 0.0)
            )
        )
    }

    private fun shareToken(shoppingId: String) {
        viewModelScope.launch {
            val shopping = repository.getById(shoppingId)
            val token = shopping?.shortCode ?: shoppingId
            _navigator?.navigate(QrCode(token))
        }
    }
}
