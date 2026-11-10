package br.com.brunocarvalhs.howmuch.core.data.wearable

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingInbox
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearPairingMessage
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearablePaths
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

internal class WearableSyncServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wearPairingInbox: WearPairingInbox
) : WearableSyncService {

    override suspend fun syncAuthStatus(isAuthenticated: Boolean, userId: String?) {
        val request = PutDataMapRequest.create("/auth").apply {
            dataMap.putBoolean("authenticated", isAuthenticated)
            dataMap.putString("userId", userId.orEmpty())
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        try {
            Wearable.getDataClient(context).putDataItem(request).await()
            Timber.tag("WearableSync").d("Auth status synced: $isAuthenticated")
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.API_NOT_CONNECTED) {
                Timber.tag("WearableSync").w("Error syncing auth status: Wearable API not connected (Code 17)")
            } else {
                Timber.tag("WearableSync").e(e, "Error syncing auth status")
            }
        } catch (e: Exception) {
            Timber.tag("WearableSync").e(e, "Error syncing auth status")
        }
    }

    override suspend fun syncShoppingLists(shoppingListsJson: String) {
        val request = PutDataMapRequest.create("/shopping_lists").apply {
            dataMap.putString("json", shoppingListsJson)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        try {
            Wearable.getDataClient(context).putDataItem(request).await()
            Timber.tag("WearableSync").d("Shopping lists synced")
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.API_NOT_CONNECTED) {
                Timber.tag("WearableSync").w("Error syncing shopping lists: Wearable API not connected (Code 17)")
            } else {
                Timber.tag("WearableSync").e(e, "Error syncing shopping lists")
            }
        } catch (e: Exception) {
            Timber.tag("WearableSync").e(e, "Error syncing shopping lists")
        }
    }

    override suspend fun notifySignOut() {
        try {
            Wearable.getCapabilityClient(context)
                .getCapability(
                    WearablePaths.WEAR_CAPABILITY,
                    com.google.android.gms.wearable.CapabilityClient.FILTER_REACHABLE
                )
                .await()
                .nodes
                .forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, WearablePaths.AUTH_SIGN_OUT, ByteArray(0))
                        .await()
                }
        } catch (e: Exception) {
            // An unreachable watch keeps its session until it next pairs.
            Timber.tag("WearableSync").w(e, "Could not notify the watch of the sign-out")
        }
    }

    /**
     * Opens the phone app on the nearest phone with a deeplink that carries the session:
     * `cestou://wear/pair?sid=...&k=...&n=...`. [RemoteActivityHelper] resolves the intent on the
     * phone; the phone answers later with its key over MessageClient.
     */
    override suspend fun requestPhonePairing(sessionId: String, watchPublicKey: String): Boolean = try {
        val nodeClient = Wearable.getNodeClient(context)
        val phoneNode = nodeClient.connectedNodes.await().let { nodes ->
            nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()
        }
        if (phoneNode == null) {
            Timber.tag("WearableSync").w("No connected phone node for pairing")
            false
        } else {
            val watchNodeId = nodeClient.localNode.await().id
            val intent = buildPhonePairingIntent(sessionId, watchPublicKey, watchNodeId)
            RemoteActivityHelper(context, context.mainExecutor).startRemoteActivity(intent, phoneNode.id).await()
            true
        }
    } catch (e: ApiException) {
        Timber.tag("WearableSync").e(e, "Error requesting the phone pairing")
        false
    } catch (e: Exception) {
        Timber.tag("WearableSync").e(e, "Unexpected error requesting the phone pairing")
        false
    }

    override suspend fun sendWearConfirm(phoneNodeId: String, sessionId: String): Boolean =
        sendMessage(phoneNodeId, WearablePaths.PAIRING_WEAR_CONFIRM, WearablePaths.encodeWearConfirm(sessionId))

    override suspend fun sendPhoneKey(watchNodeId: String, sessionId: String, phonePublicKey: String): Boolean =
        sendMessage(
            watchNodeId,
            WearablePaths.PAIRING_PHONE_KEY,
            WearablePaths.encodePhoneKey(sessionId, phonePublicKey)
        )

    override suspend fun sendAuthPair(
        watchNodeId: String,
        sessionId: String,
        userId: String,
        googleIdToken: String
    ): Boolean = sendMessage(
        watchNodeId,
        WearablePaths.AUTH_PAIR,
        WearablePaths.encodeAuthPair(sessionId, userId, googleIdToken)
    )

    override suspend fun sendPairingError(nodeId: String, sessionId: String, reason: String) {
        sendMessage(nodeId, WearablePaths.PAIRING_ERROR, WearablePaths.encodeError(sessionId, reason))
    }

    override fun observeWatchConfirm(sessionId: String): Flow<WearPairingMessage.WatchConfirmed> =
        wearPairingInbox.messages
            .filterIsInstance<WearPairingMessage.WatchConfirmed>()
            .filter { it.sessionId == sessionId }
            .take(1)

    /** One attempt, no retry: the pairing session has its own timeouts, so a failed send ends it. */
    private suspend fun sendMessage(nodeId: String, path: String, payload: ByteArray): Boolean {
        val messageClient: MessageClient = Wearable.getMessageClient(context)
        return try {
            messageClient.sendMessage(nodeId, path, payload).await()
            true
        } catch (e: ApiException) {
            Timber.tag("WearableSync").e(e, "Error sending $path (status ${e.statusCode})")
            false
        } catch (e: Exception) {
            Timber.tag("WearableSync").e(e, "Unexpected error sending $path")
            false
        }
    }
}

private const val WEAR_DEEPLINK_SCHEME = "cestou"
private const val WEAR_DEEPLINK_HOST = "wear"
private const val WEAR_DEEPLINK_PATH = "pair"
private const val PARAM_SESSION = "sid"
private const val PARAM_WATCH_KEY = "k"
private const val PARAM_WATCH_NODE = "n"

/**
 * The intent the watch sends to the phone for a pairing (sdd-055). It is `ACTION_VIEW` with
 * [Intent.CATEGORY_BROWSABLE], because `RemoteActivityHelper` rejects an `ACTION_VIEW` intent
 * without it. The phone's deeplink filter (`cestou://wear/pair`) declares the same category.
 */
internal fun buildPhonePairingIntent(sessionId: String, watchPublicKey: String, watchNodeId: String): Intent {
    val uri = Uri.Builder()
        .scheme(WEAR_DEEPLINK_SCHEME)
        .authority(WEAR_DEEPLINK_HOST)
        .path(WEAR_DEEPLINK_PATH)
        .appendQueryParameter(PARAM_SESSION, sessionId)
        .appendQueryParameter(PARAM_WATCH_KEY, watchPublicKey)
        .appendQueryParameter(PARAM_WATCH_NODE, watchNodeId)
        .build()
    return Intent(Intent.ACTION_VIEW, uri)
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .setPackage(WearablePaths.PHONE_APPLICATION_ID)
}
