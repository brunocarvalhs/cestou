package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cache of per-category totals of a finished list. Products of a FINISH list are locked
 * for writes, so the totals only change when the list is reopened and finished again, which changes
 * `finishedAt` and therefore the key. Memory is enough: the data is derived, small and cheap to
 * recompute, and Firestore's own cache already covers offline.
 */
@Singleton
internal class CategorySpendingCache @Inject constructor() {

    private data class Key(val shoppingId: String, val finishedAt: Long?)

    private val entries = ConcurrentHashMap<Key, Map<String, Double>>()

    fun get(shoppingId: String, finishedAt: Long?): Map<String, Double>? =
        entries[Key(shoppingId, finishedAt)]

    fun put(shoppingId: String, finishedAt: Long?, totals: Map<String, Double>) {
        entries[Key(shoppingId, finishedAt)] = totals
    }
}
