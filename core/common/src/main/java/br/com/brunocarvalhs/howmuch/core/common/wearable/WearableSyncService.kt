package br.com.brunocarvalhs.howmuch.core.common.wearable

import kotlinx.coroutines.flow.Flow

interface WearableSyncService {
    suspend fun syncAuthStatus(isAuthenticated: Boolean, userId: String?)
    suspend fun syncShoppingLists(shoppingListsJson: String)
    suspend fun notifySignOut()

    /**
     * Watch: asks the phone to open the pairing deeplink. Returns false when the phone could not
     * be reached; the phone's answer (its key) arrives later as a [WearPairingMessage.PhoneKey].
     */
    suspend fun requestPhonePairing(sessionId: String, watchPublicKey: String): Boolean

    /** Watch: tells the phone the user tapped "Parear". Returns false when the send failed. */
    suspend fun sendWearConfirm(phoneNodeId: String, sessionId: String): Boolean

    /** Phone: sends its public key to the watch node that asked for the pairing. */
    suspend fun sendPhoneKey(watchNodeId: String, sessionId: String, phonePublicKey: String): Boolean

    /** Phone: sends the sign-in material. Only called after both sides confirmed. */
    suspend fun sendAuthPair(watchNodeId: String, sessionId: String, userId: String, googleIdToken: String): Boolean

    /** Either side: reports why the session stopped, so the other side can show it. */
    suspend fun sendPairingError(nodeId: String, sessionId: String, reason: String)

    /** Phone: emits once when the watch confirms [sessionId]. Unregisters its listener when collection stops. */
    fun observeWatchConfirm(sessionId: String): Flow<WearPairingMessage.WatchConfirmed>
}
