package br.com.brunocarvalhs.howmuch.feature.subscription.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.billing.PlayBillingSubscriptionRepository
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.intent.PaywallIntent
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.state.PaywallUiState
import com.android.billingclient.api.BillingClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.TimeSource

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingRepository: PlayBillingSubscriptionRepository,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    private val openedAt = TimeSource.Monotonic.markNow()
    private var subscribed = false

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState = _uiState.asStateFlow()

    val intent = PaywallIntent(
        onSubscribeClick = { activity -> subscribe(activity) }
    )

    init {
        viewModelScope.launch {
            billingRepository.observeStatus().collect { status ->
                _uiState.update { it.copy(status = status) }
            }
        }
        viewModelScope.launch {
            billingRepository.refresh()
        }
        loadPrice()
    }

    fun setSource(source: String) {
        if (_uiState.value.source == source) return
        analyticsTracker.trackEvent(AnalyticsEvents.PAYWALL_VIEWED, mapOf(AnalyticsParams.SOURCE to source))
        _uiState.update { it.copy(source = source) }
    }

    private fun loadPrice() {
        viewModelScope.launch {
            val price = billingRepository.queryFormattedPrice()
            _uiState.update { it.copy(formattedPrice = price, isLoadingPrice = false) }
        }
    }

    private fun subscribe(activity: Activity) {
        if (_uiState.value.isPurchasing) return
        _uiState.update { it.copy(isPurchasing = true, errorMessage = null) }
        analyticsTracker.trackEvent(
            AnalyticsEvents.PAYWALL_SUBSCRIBE_CLICKED,
            mapOf(AnalyticsParams.SOURCE to _uiState.value.source)
        )

        viewModelScope.launch {
            val result = billingRepository.purchase(activity)
            val isUserCanceled = result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                subscribed = true
                analyticsTracker.trackEvent(
                    AnalyticsEvents.SUBSCRIPTION_STARTED,
                    mapOf(AnalyticsParams.SOURCE to _uiState.value.source)
                )
            }
            val failureMessage = if (result.responseCode != BillingClient.BillingResponseCode.OK && !isUserCanceled) {
                analyticsTracker.trackEvent(
                    AnalyticsEvents.PAYWALL_PURCHASE_FAILED,
                    mapOf(AnalyticsParams.REASON to result.responseCode.toString())
                )
                result.debugMessage
            } else {
                null
            }
            _uiState.update { it.copy(isPurchasing = false, errorMessage = failureMessage) }
        }
    }

    override fun onCleared() {
        reportDismissal()
    }

    /** Saída sem assinar (voltar, gesto ou sistema): é aqui que o funil de conversão vaza. */
    internal fun reportDismissal() {
        if (subscribed || _uiState.value.status == SubscriptionStatus.PRO) return
        analyticsTracker.trackEvent(
            AnalyticsEvents.PAYWALL_DISMISSED,
            mapOf(
                AnalyticsParams.SOURCE to _uiState.value.source,
                AnalyticsParams.DURATION_MS to openedAt.elapsedNow().inWholeMilliseconds
            )
        )
    }
}
