package br.com.brunocarvalhs.howmuch.wear

import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingInbox
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingMessage
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearablePaths
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives the watch's answer during a pairing (sdd-055): the "Parear" tap, sent on
 * `/pairing/wear-confirm`. It does nothing else. It only forwards the message to [WearPairingInbox],
 * so the pairing screen can decide, and it never blocks: a message nobody is waiting for is dropped.
 *
 * Declared in the app manifest with the `/pairing` prefix, so the system starts it when a
 * message arrives, even if the phone app is closed.
 */
@AndroidEntryPoint
class WearPairingListenerService : WearableListenerService() {

    @Inject
    lateinit var pairingInbox: WearPairingInbox

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearablePaths.PAIRING_WEAR_CONFIRM) return
        val sessionId = WearablePaths.decodeWearConfirm(messageEvent.data) ?: return
        pairingInbox.emit(WearPairingMessage.WatchConfirmed(sessionId, messageEvent.sourceNodeId))
    }
}
