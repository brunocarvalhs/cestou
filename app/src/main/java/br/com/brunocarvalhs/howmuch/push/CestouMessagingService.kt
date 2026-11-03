package br.com.brunocarvalhs.howmuch.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import javax.inject.Inject

/**
 * Receives campaign pushes while the app is in the foreground and stores them in the notifications
 * feed. No token is stored: campaigns are sent from the Firebase console by app or segment.
 */
@AndroidEntryPoint
internal class CestouMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushNotificationHandler: PushNotificationHandler

    /**
     * Runs on a background thread with roughly 20 s before the system may stop the process, so the
     * save blocks here instead of launching and returning. A timeout does not lose the push: the
     * Firestore write is already queued in the local cache and goes out when the network is back.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val payload = PushPayloadExtractor.from(message)
        val outcome = runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(SAVE_TIMEOUT_MS) { pushNotificationHandler.handle(payload, PushSource.MESSAGE) }
        }
        if (outcome == null) Timber.w("Saving a received push timed out; the write stays queued offline")
    }

    private companion object {
        const val SAVE_TIMEOUT_MS = 10_000L
    }
}
