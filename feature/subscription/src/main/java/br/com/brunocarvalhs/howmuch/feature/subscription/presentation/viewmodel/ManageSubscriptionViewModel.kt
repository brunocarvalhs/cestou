package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.billing.BillingConstants
import br.com.brunocarvalhs.howmuch.core.billing.PlayBillingSubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.common.extensions.openBrowser
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.intent.ManageSubscriptionIntent
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state.ManageSubscriptionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the screen an already-PRO user reaches from Profile (never FREE users - [Paywall]
 * handles those). Play Billing subscriptions can only be cancelled or changed through Google
 * Play itself (store policy), so this screen's only real action is a deep link there - there is
 * no in-app cancel/upgrade flow to build (AD-010: this app never gets a purchase token or expiry
 * date from Play Billing client-side, so it cannot show a renewal date either).
 */
@HiltViewModel
class ManageSubscriptionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val billingRepository: PlayBillingSubscriptionRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageSubscriptionUiState())
    val uiState = _uiState.asStateFlow()

    val intent = ManageSubscriptionIntent(
        onManageOnPlayStore = { openPlayStoreSubscriptions() }
    )

    init {
        loadPrice()
    }

    private fun loadPrice() {
        viewModelScope.launch {
            // A tela de gerenciamento mostra o preço mensal (manage_subscription_price, "/mês").
            val price = billingRepository.queryPlans()
                .firstOrNull { it.basePlanId == BillingConstants.BASE_PLAN_MONTHLY }
                ?.formattedPrice
            _uiState.update { it.copy(formattedPrice = price, isLoadingPrice = false) }
        }
    }

    private fun openPlayStoreSubscriptions() {
        analyticsTracker.trackEvent(AnalyticsEvents.MANAGE_SUBSCRIPTION_PLAY_STORE_OPENED)
        context.openBrowser(
            "https://play.google.com/store/account/subscriptions" +
                "?sku=${BillingConstants.PRO_MONTHLY}&package=${context.packageName}"
        )
    }
}
