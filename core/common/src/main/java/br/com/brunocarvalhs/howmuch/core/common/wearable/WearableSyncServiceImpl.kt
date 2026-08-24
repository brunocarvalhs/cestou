package br.com.brunocarvalhs.howmuch.core.common.wearable

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

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
        } catch (e: Exception) {
            // Log error
        }
    }

    override suspend fun syncShoppingLists(shoppingListsJson: String) {
        val request = PutDataMapRequest.create("/shopping_lists").apply {
            dataMap.putString("json", shoppingListsJson)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        try {
            Wearable.getDataClient(context).putDataItem(request).await()
        } catch (e: Exception) {
            // Log error
        }
    }
}
