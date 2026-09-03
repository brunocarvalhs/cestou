package br.com.brunocarvalhs.howmuch.core.common.wearable

interface WearableSyncService {
    suspend fun syncAuthStatus(isAuthenticated: Boolean, userId: String?)
    suspend fun syncShoppingLists(shoppingListsJson: String)
    suspend fun updatePairingCode(code: String)
    suspend fun findNodeByPairingCode(code: String): String?
    suspend fun sendAuthTokenToNode(nodeId: String, token: String)
    suspend fun openPhoneApp()
    fun generatePairingCode(): String
    val receivedAuthToken: kotlinx.coroutines.flow.Flow<String>
}
