package br.com.brunocarvalhs.howmuch.feature.shopping.data.local

import br.com.brunocarvalhs.howmuch.core.domain.services.StorageService
import br.com.brunocarvalhs.howmuch.core.domain.services.get
import br.com.brunocarvalhs.howmuch.core.domain.services.observe
import br.com.brunocarvalhs.howmuch.feature.shopping.di.WearListDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** The account's PRO state, as the phone last published it. */
@Serializable
data class WearSubscriptionState(val isPro: Boolean)

/**
 * Keeps the PRO state the phone publishes. Emits null until the phone has sent one, so the watch
 * can tell "not PRO" apart from "not known yet".
 */
@Singleton
class WearSubscriptionStore @Inject constructor(
    @WearListDataStore private val storage: StorageService
) {
    val isPro: Flow<Boolean?> = storage.observe<WearSubscriptionState>(KEY).map { it?.isPro }

    suspend fun isPro(): Boolean? = storage.get<WearSubscriptionState>(KEY)?.isPro

    suspend fun save(isPro: Boolean) = storage.save(KEY, WearSubscriptionState(isPro))

    /** Sem status até o celular publicar de novo: o gate PRO volta a esperar. */
    suspend fun clear() = storage.remove(KEY)

    private companion object {
        const val KEY = "subscription_state"
    }
}
