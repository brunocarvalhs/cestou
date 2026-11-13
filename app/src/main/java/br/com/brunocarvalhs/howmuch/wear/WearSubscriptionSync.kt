package br.com.brunocarvalhs.howmuch.wear

import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes the account's PRO state to the watch, which shows its content only for PRO. Only
 * changes are sent, so a repeated emission does not rewrite the DataItem.
 */
@Singleton
class WearSubscriptionSync @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val wearableSyncService: WearableSyncService
) {

    /** Starts publishing in [scope]; the app's view model owns that scope. */
    fun start(scope: CoroutineScope) {
        scope.launch {
            subscriptionRepository.observeStatus()
                .map { it == SubscriptionStatus.PRO }
                .distinctUntilChanged()
                .collect { isPro -> wearableSyncService.syncSubscription(isPro) }
        }
    }
}
