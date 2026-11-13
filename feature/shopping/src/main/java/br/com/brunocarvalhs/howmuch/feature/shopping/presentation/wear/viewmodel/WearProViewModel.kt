package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearSubscriptionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The watch's PRO gate. Emits true only when the phone has published a PRO account; false and null
 * (not published yet) both keep the content hidden.
 */
@HiltViewModel
class WearProViewModel @Inject constructor(
    store: WearSubscriptionStore,
    private val wearableSyncService: WearableSyncService,
    private val analytics: AnalyticsTracker
) : ViewModel() {

    val isPro: StateFlow<Boolean?> = store.isPro.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = null
    )

    /** Opens the phone's paywall, where the subscription is bought. Nothing happens if no phone is reachable. */
    fun openPhone() {
        analytics.trackEvent(AnalyticsEvents.WEAR_PRO_OPEN_PHONE_CLICKED)
        viewModelScope.launch { wearableSyncService.openPhoneApp(PATH_PHONE_SUBSCRIPTION) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val PATH_PHONE_SUBSCRIPTION = "subscription"
    }
}
