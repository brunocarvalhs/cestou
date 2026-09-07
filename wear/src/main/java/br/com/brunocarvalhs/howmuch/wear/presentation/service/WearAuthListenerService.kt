package br.com.brunocarvalhs.howmuch.wear.presentation.service

import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingCodeHolder
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearablePaths
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class WearAuthListenerService : WearableListenerService() {

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var pairingCodeHolder: PairingCodeHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WearablePaths.AUTH_PAIR -> {
                val userId = String(messageEvent.data)
                Timber.tag("WearAuth").d("Received pairing message for user: $userId")
                scope.launch {
                    authService.updateUserId(userId)
                }
            }

            WearablePaths.PAIRING_VERIFY -> {
                val receivedCode = String(messageEvent.data)
                if (receivedCode.isNotEmpty() && receivedCode == pairingCodeHolder.currentCode) {
                    Timber.tag("WearAuth").d("Pairing code verified, confirming to node: ${messageEvent.sourceNodeId}")
                    Wearable.getMessageClient(this)
                        .sendMessage(messageEvent.sourceNodeId, WearablePaths.PAIRING_CONFIRM, ByteArray(0))
                } else {
                    Timber.tag("WearAuth").d("Pairing code mismatch from node: ${messageEvent.sourceNodeId}")
                }
            }
        }
    }
}
