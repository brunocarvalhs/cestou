package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.extensions.trackError
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.analytics.service.FirstTimeEventTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationOrigin
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.cart.R
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.FinishPurchaseUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

private const val TYPE_LIST_FINISHED = "list_finished"

@HiltViewModel
internal class FinishPurchaseViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ShoppingRepository,
    private val authService: AuthService,
    private val notificationRepository: NotificationRepository,
    private val analyticsTracker: AnalyticsTracker,
    private val firstTimeEventTracker: FirstTimeEventTracker
) : ViewModel() {

    private var _navigator: Navigator? = null

    private val _uiState = MutableStateFlow(FinishPurchaseUiState())
    val uiState: StateFlow<FinishPurchaseUiState> = _uiState.asStateFlow()

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun onFinishPurchase(shopping: Shopping, price: Double, establishment: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // Partial write: status/price/establishment/finishedAt only. `description` is never touched here
            // (it used to be overwritten with the establishment name) and no other member's
            // concurrent edit to this list is clobbered.
            repository.updateFields(
                shopping.id,
                mapOf(
                    "status" to Shopping.Status.FINISH.name,
                    "price" to price,
                    "establishment" to establishment,
                    "finishedAt" to System.currentTimeMillis()
                )
            ).onSuccess {
                analyticsTracker.trackEvent(
                    AnalyticsEvents.CART_FINISH_PURCHASE_COMPLETED,
                    mapOf(
                        AnalyticsParams.SHOPPING_ID to shopping.id,
                        AnalyticsParams.AMOUNT to price
                    )
                )
                firstTimeEventTracker.trackOnce(AnalyticsEvents.FIRST_PURCHASE_COMPLETED)
                notifyOtherMembers(shopping)
                _uiState.update { it.copy(isLoading = false) }
                _navigator?.goBack()
            }.onFailure { error ->
                analyticsTracker.trackError("finish_purchase", error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.StringResource(R.string.cart_error_finish_purchase)
                    )
                }
            }
        }
    }

    private suspend fun notifyOtherMembers(shopping: Shopping) {
        val actorId = authService.currentUser?.id ?: return
        val actorName = authService.currentUser?.displayName
            ?: context.getString(CoreR.string.notification_someone)
        val title = context.getString(CoreR.string.notification_list_finished_title)
        val message = context.getString(
            CoreR.string.notification_list_finished_message,
            actorName,
            shopping.title
        )
        shopping.users.filter { it != actorId }.forEach { memberId ->
            notificationRepository.notify(
                userId = memberId,
                title = title,
                message = message,
                type = TYPE_LIST_FINISHED,
                origin = NotificationOrigin(shoppingId = shopping.id, senderId = actorId)
            )
        }
    }
}
