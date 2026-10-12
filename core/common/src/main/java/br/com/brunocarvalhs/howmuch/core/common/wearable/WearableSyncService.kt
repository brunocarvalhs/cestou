package br.com.brunocarvalhs.howmuch.core.common.wearable

interface WearableSyncService {
    suspend fun syncAuthStatus(isAuthenticated: Boolean, userId: String?)
    suspend fun syncShoppingLists(shoppingListsJson: String)
    suspend fun updatePairingCode(code: String)
    suspend fun findNodeByPairingCode(code: String): String?
    suspend fun sendAuthPairToNode(nodeId: String, userId: String, googleIdToken: String)
    suspend fun notifySignOut()
    suspend fun openPhoneApp()
    fun generatePairingCode(): String
}
