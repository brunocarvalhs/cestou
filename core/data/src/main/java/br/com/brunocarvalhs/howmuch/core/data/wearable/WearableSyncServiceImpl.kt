package br.com.brunocarvalhs.howmuch.core.data.wearable

import android.content.Context
import android.content.Intent
import androidx.wear.remote.interactions.RemoteActivityHelper
import br.com.brunocarvalhs.howmuch.core.common.wearable.PairingCodeHolder
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearablePaths
import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

// Pairing codes are always 6 digits.
private const val PAIRING_CODE_MIN = 100_000
private const val PAIRING_CODE_MAX = 999_999

internal class WearableSyncServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pairingCodeHolder: PairingCodeHolder
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

    override suspend fun updatePairingCode(code: String) {
        // Stored in memory only - the wear app advertises itself via the
        // "howmuch_wear_app" capability (see wear/res/values/wear.xml)
        // instead of publishing the code through DataClient, since
        // DataClient item sync requires both devices' apps to share the same
        // applicationId, which is no longer true (wear has its own).
        pairingCodeHolder.currentCode = code
        Timber.tag("WearableSync").d("Pairing code stored locally: $code")
    }

    override suspend fun findNodeByPairingCode(code: String): String? {
        Timber.tag("WearableSync").d("Searching for a wear node with pairing code: $code")

        val candidateNodes = try {
            Wearable.getCapabilityClient(context)
                .getCapability(WearablePaths.WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.API_NOT_CONNECTED) {
                Timber.tag("WearableSync").w("Wearable API not connected (Code 17). Is Wear OS app installed?")
            } else {
                Timber.tag("WearableSync").e(e, "Error querying the wear capability")
            }
            return null
        } catch (e: Exception) {
            Timber.tag("WearableSync").e(e, "Unexpected error querying the wear capability")
            return null
        }

        if (candidateNodes.isEmpty()) {
            Timber.tag("WearableSync").w("No reachable node advertises the wear capability. Pairing might not work.")
            return null
        }

        for (node in candidateNodes) {
            if (verifyPairingCodeWithNode(node.id, code)) {
                Timber.tag("WearableSync").d("Pairing code confirmed by node: ${node.id}")
                return node.id
            }
        }
        Timber.tag("WearableSync").w("No node confirmed pairing code: $code")
        return null
    }

    /**
     * Sends the code to [nodeId] and waits for that same node to confirm it
     * matches what it currently has stored (see WearAuthListenerService).
     * Uses MessageClient rather than DataClient because message delivery is
     * routed by manifest intent-filter, not by matching applicationId.
     */
    private suspend fun verifyPairingCodeWithNode(nodeId: String, code: String): Boolean {
        val messageClient = Wearable.getMessageClient(context)
        return try {
            withTimeout(5000.milliseconds) {
                val confirmed = CompletableDeferred<Boolean>()
                val listener = MessageClient.OnMessageReceivedListener { event ->
                    if (event.sourceNodeId == nodeId && event.path == WearablePaths.PAIRING_CONFIRM) {
                        confirmed.complete(true)
                    }
                }
                messageClient.addListener(listener)
                try {
                    messageClient.sendMessage(nodeId, WearablePaths.PAIRING_VERIFY, code.toByteArray()).await()
                    confirmed.await()
                } finally {
                    messageClient.removeListener(listener)
                }
            }
        } catch (_: TimeoutCancellationException) {
            Timber.tag("WearableSync").d("Node $nodeId did not confirm pairing code $code in time")
            false
        } catch (e: ApiException) {
            Timber.tag("WearableSync").e(e, "Error verifying pairing code with node $nodeId")
            false
        } catch (e: Exception) {
            Timber.tag("WearableSync").e(e, "Unexpected error verifying pairing code with node $nodeId")
            false
        }
    }

    override suspend fun sendAuthPairToNode(nodeId: String, userId: String, googleIdToken: String) {
        try {
            Timber.tag("WearableSync").d("Sending auth pair to node: $nodeId")
            Wearable.getMessageClient(context)
                .sendMessage(nodeId, WearablePaths.AUTH_PAIR, WearablePaths.encodeAuthPair(userId, googleIdToken))
                .await()
            Timber.tag("WearableSync").d("Auth pair sent successfully")
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.API_NOT_CONNECTED) {
                Timber.tag("WearableSync").w("Error sending auth pair: Wearable API not connected (Code 17)")
            } else {
                Timber.tag("WearableSync").e(e, "Error sending auth pair")
            }
        } catch (e: Exception) {
            Timber.tag("WearableSync").e(e, "Error sending auth pair")
        }
    }

    override suspend fun notifySignOut() {
        try {
            Wearable.getCapabilityClient(context)
                .getCapability(WearablePaths.WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes
                .forEach { node ->
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, WearablePaths.AUTH_SIGN_OUT, ByteArray(0))
                        .await()
                }
        } catch (e: Exception) {
            // ponytail: an unreachable watch keeps its session until it next pairs; queue it if that matters.
            Timber.tag("WearableSync").w(e, "Could not notify the watch of the sign-out")
        }
    }

    override suspend fun openPhoneApp() {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val phoneNode = nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()
            if (phoneNode != null) {
                val remoteActivityHelper = RemoteActivityHelper(context, context.mainExecutor)
                // Package-only (no explicit component): wear no longer shares
                // the phone app's applicationId, so context.packageName no
                // longer identifies it. RemoteActivityHelper resolves this
                // intent on the remote (phone) node using its own
                // PackageManager, so a package-targeted MAIN/LAUNCHER intent
                // resolves correctly there regardless of wear's own package.
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setPackage(WearablePaths.PHONE_APPLICATION_ID)
                }
                Timber.tag("WearableSync").d("Opening phone app on node: ${phoneNode.id}")
                remoteActivityHelper.startRemoteActivity(intent, phoneNode.id).await()
            } else {
                Timber.tag("WearableSync").w("No connected nodes found to open phone app")
            }
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.API_NOT_CONNECTED) {
                Timber.tag("WearableSync").w("Error opening phone app: Wearable API not connected (Code 17)")
            } else {
                Timber.tag("WearableSync").e(e, "Error opening phone app")
            }
        } catch (e: Exception) {
            Timber.tag("WearableSync").e(e, "Error opening phone app")
        }
    }

    override fun generatePairingCode(): String {
        val code = (PAIRING_CODE_MIN..PAIRING_CODE_MAX).random().toString()
        CoroutineScope(Dispatchers.IO).launch {
            updatePairingCode(code)
        }
        return code
    }
}
