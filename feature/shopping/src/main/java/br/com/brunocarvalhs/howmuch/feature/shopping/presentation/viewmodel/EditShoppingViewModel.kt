package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.extensions.trackAction
import br.com.brunocarvalhs.howmuch.core.analytics.extensions.trackError
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsElements
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.QrCode
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingUpdateUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.EditShopping
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
        onShareToken = { id ->
            analyticsTracker.trackAction(AnalyticsElements.EDIT_SHOPPING_SHARE_TOKEN)
            shareToken(id)
        },
        onCancel = {
            analyticsTracker.trackEvent(
                AnalyticsEvents.DIALOG_DISMISSED,
                mapOf(AnalyticsParams.ELEMENT_ID to "edit_shopping.cancel")
            )
            _navigator?.goBack()
        },
        onErrorShown = { _uiState.update { it.copy(error = null) } }
    )

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    private fun update(shopping: Shopping) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Only the fields this screen actually edits (see EditShoppingContent): never
            // users/roles/status/position/shortCode/createdAt, which other flows own.
            val fields = mapOf(
                "title" to shopping.title,
                "description" to shopping.description,
                "isCategorized" to shopping.isCategorized,
                "price" to shopping.price,
                "budget" to (shopping.budget ?: NetworkService.FieldOp.Delete)
            )
            shoppingUpdateUseCase(shopping.id, fields)
                .onSuccess {
                    trackBudgetChangeIfNeeded(shopping)
                    analyticsTracker.trackEvent(
                        AnalyticsEvents.SHOPPING_LIST_UPDATED,
                        mapOf(AnalyticsParams.SHOPPING_ID to shopping.id)
                    )
                    _uiState.update { it.copy(isLoading = false) }
                    _navigator?.goBack()
                }
                .onFailure { error ->
                    analyticsTracker.trackError("update_shopping", error)
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
            shopping?.let { runCatching { repository.ensureShortCodeIndex(it) } }
            val token = shopping?.shortCode ?: shoppingId
            _navigator?.navigate(QrCode(token))
        }
    }
}
