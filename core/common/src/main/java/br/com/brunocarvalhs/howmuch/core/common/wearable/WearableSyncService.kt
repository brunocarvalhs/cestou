package br.com.brunocarvalhs.howmuch.core.common.wearable

interface WearableSyncService {
    suspend fun syncAuthStatus(isAuthenticated: Boolean, userId: String?)
    suspend fun syncShoppingLists(shoppingListsJson: String)
}
