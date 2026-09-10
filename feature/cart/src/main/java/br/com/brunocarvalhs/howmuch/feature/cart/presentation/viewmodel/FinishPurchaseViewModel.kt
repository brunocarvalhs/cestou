package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private var _navigator: Navigator? = null

    init {
        analyticsTracker.trackScreenView(screenName = "cart_finish_purchase", screenClass = "FinishPurchaseViewModel")
    }

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }

    fun onFinishPurchase(shopping: Shopping, price: Double, establishment: String) {
        viewModelScope.launch {
            val updatedShopping = shopping.copy(
                price = price,
                description = establishment,
                status = Shopping.Status.FINISH
            )
            repository.update(updatedShopping)
            analyticsTracker.trackEvent(
                AnalyticsEvents.CART_FINISH_PURCHASE_COMPLETED,
                mapOf(
                    AnalyticsParams.SHOPPING_ID to shopping.id,
                    AnalyticsParams.AMOUNT to price
                )
            )
            notifyOtherMembers(updatedShopping)
            _navigator?.goBack()
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
            notificationRepository.notify(memberId, title, message, TYPE_LIST_FINISHED)
        }
    }
}
