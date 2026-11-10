package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val wearableSyncService: WearableSyncService
) : ViewModel() {

    val isPro: StateFlow<Boolean?> = store.isPro.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = null
    )

    /** Opens the phone app, where the subscription is bought. Nothing happens if no phone is reachable. */
    fun openPhone() {
        viewModelScope.launch { wearableSyncService.openPhoneApp(PATH_PHONE_HOME) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val PATH_PHONE_HOME = "home"
    }
}
