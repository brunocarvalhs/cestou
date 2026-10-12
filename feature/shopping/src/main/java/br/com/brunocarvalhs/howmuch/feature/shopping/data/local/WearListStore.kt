package br.com.brunocarvalhs.howmuch.feature.shopping.data.local

import br.com.brunocarvalhs.howmuch.core.domain.services.StorageService
import br.com.brunocarvalhs.howmuch.core.domain.services.get
import br.com.brunocarvalhs.howmuch.core.domain.services.observe
import br.com.brunocarvalhs.howmuch.feature.shopping.di.WearListDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** What the watch shows without opening the list: the tile, and which list the app reopens. */
@Serializable
data class WearListSummary(
    val shoppingId: String,
    val title: String,
    val emoji: String = "",
    /** Sum of every item's price × quantity; items without a price count as zero. */
    val total: Double,
    val budget: Double? = null,
    val pending: Int,
    val itemCount: Int
)

/** The last list opened on the watch, refreshed on every change while it is open. */
@Singleton
class WearListStore @Inject constructor(
    @WearListDataStore private val storage: StorageService
) {
    val lastList: Flow<WearListSummary?> = storage.observe(KEY)

    suspend fun lastList(): WearListSummary? = storage.get(KEY)

    suspend fun save(summary: WearListSummary) = storage.save(KEY, summary)

    suspend fun clear() = storage.remove(KEY)

    private companion object {
        const val KEY = "last_list"
    }
}
