package br.com.brunocarvalhs.howmuch.core.common.wearable

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import androidx.wear.remote.interactions.RemoteActivityHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import com.google.android.gms.wearable.MessageClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

internal class WearableSyncServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
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
        try {
            val nodeId = Wearable.getNodeClient(context).localNode.await().id
            Timber.tag("WearableSync").d("Updating pairing code: $code for node: $nodeId")
            val request = PutDataMapRequest.create("/pairing/$nodeId").apply {
                dataMap.putString("code", code)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()
            Wearable.getDataClient(context).putDataItem(request).await()
            Timber.tag("WearableSync").d("Pairing code updated successfully")
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.API_NOT_CONNECTED) {
                Timber.tag("WearableSync").w("Error updating pairing code: Wearable API not connected (Code 17)")
            } else {
                Timber.tag("WearableSync").e(e, "Error updating pairing code")
            }
        } catch (e: Exception) {
            Timber.tag("WearableSync").e(e, "Error updating pairing code")
        }
    }

    override suspend fun findNodeByPairingCode(code: String): String? {
        Timber.tag("WearableSync").d("Searching for node with pairing code: $code")

        // First, check connected nodes to see if we can find a shortcut or at least verify API availability
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            if (nodes.isEmpty()) {
                Timber.tag("WearableSync").w("No connected nodes found. Pairing might not work.")
            }
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.API_NOT_CONNECTED) {
                Timber.tag("WearableSync").w("Wearable API not connected (Code 17). Is Wear OS app installed?")
                return null
            }
            Timber.tag("WearableSync").e(e, "Error checking connected nodes")
        } catch (e: Exception) {
            Timber.tag("WearableSync").e(e, "Unexpected error checking connected nodes")
        }

        repeat(3) { attempt ->
            try {
                // Use withTimeout to prevent hanging if the API is unresponsive
                withTimeout(5000.milliseconds) {
                    val dataItemBuffer = Wearable.getDataClient(context).dataItems.await()
                    try {
                        Timber.tag("WearableSync").d("Attempt ${attempt + 1}: Found ${dataItemBuffer.count} total data items")
                        for (item in dataItemBuffer) {
                            val uri = item.uri
                            if (uri.path?.startsWith("/pairing/") == true) {
                                val dataMap = DataMapItem.fromDataItem(item).dataMap
                                val itemCode = dataMap.getString("code")
                                if (itemCode == code) {
                                    val nodeId = uri.lastPathSegment
                                    Timber.tag("WearableSync").d("Match found! Node ID: $nodeId")
                                    return@withTimeout nodeId
                                }
                            }
                        }
                    } finally {
                        dataItemBuffer.release() // CRITICAL: Release the buffer to avoid leaks
                    }
                    null
                }?.let { return it }

                if (attempt < 2) {
                    Timber.tag("WearableSync").d("No match found, retrying in 1s...")
                    kotlinx.coroutines.delay(1000)
                }
            } catch (e: ApiException) {
                if (e.statusCode == CommonStatusCodes.API_NOT_CONNECTED) {
                    Timber.tag("WearableSync").w("Wearable API not connected (Code 17). Skipping attempts.")
                    return null
                }
                Timber.tag("WearableSync").e(e, "Error finding node by pairing code (API Error)")
            } catch (_: TimeoutCancellationException) {
                Timber.tag("WearableSync").w("Timeout searching for pairing code on attempt ${attempt + 1}")
            } catch (e: Exception) {
                Timber.tag("WearableSync").e(e, "Error finding node by pairing code")
            }
        }
        Timber.tag("WearableSync").w("No node found for code: $code after 3 attempts")
        return null
    }

    override suspend fun sendAuthTokenToNode(nodeId: String, token: String) {
        try {
            Timber.tag("WearableSync").d("Sending auth token to node: $nodeId")
            Wearable.getMessageClient(context).sendMessage(nodeId, "/auth/pair", token.toByteArray()).await()
            Timber.tag("WearableSync").d("Auth token sent successfully")
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.API_NOT_CONNECTED) {
                Timber.tag("WearableSync").w("Error sending auth token: Wearable API not connected (Code 17)")
            } else {
                Timber.tag("WearableSync").e(e, "Error sending auth token")
            }
        } catch (e: Exception) {
            Timber.tag("WearableSync").e(e, "Error sending auth token")
        }
    }

    override suspend fun openPhoneApp() {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val phoneNode = nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()
            if (phoneNode != null) {
                val remoteActivityHelper = RemoteActivityHelper(context, context.mainExecutor)
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (intent != null) {
                    Timber.tag("WearableSync").d("Opening phone app on node: ${phoneNode.id}")
                    remoteActivityHelper.startRemoteActivity(intent, phoneNode.id).await()
                } else {
                    Timber.tag("WearableSync").w("Launch intent not found for package: ${context.packageName}")
                }
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
        val code = (100000..999999).random().toString()
        CoroutineScope(Dispatchers.IO).launch {
            updatePairingCode(code)
        }
        return code
    }

    override val receivedAuthToken: Flow<String> = callbackFlow {
        val listener = MessageClient.OnMessageReceivedListener { messageEvent ->
            if (messageEvent.path == "/auth/pair") {
                trySend(String(messageEvent.data))
            }
        }
        Wearable.getMessageClient(context).addListener(listener)
        awaitClose {
            Wearable.getMessageClient(context).removeListener(listener)
        }
    }
}
