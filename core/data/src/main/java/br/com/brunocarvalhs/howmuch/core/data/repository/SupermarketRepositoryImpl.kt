package br.com.brunocarvalhs.howmuch.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.brunocarvalhs.howmuch.core.domain.model.ChainPriceIndex
import br.com.brunocarvalhs.howmuch.core.domain.model.UserLocation
import br.com.brunocarvalhs.howmuch.core.domain.repository.SupermarketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private object LocationKeys {
    val STATE = stringPreferencesKey("supermarket_state")
    val CITY = stringPreferencesKey("supermarket_city")
}

/**
 * A localização fica no mesmo DataStore das preferências do app. Os índices vêm de
 * [SupermarketPriceTable] e ficam em cache por UF, já que a tabela não distingue cidades,
 * então repetir a comparação não refaz a busca.
 */
@Singleton
class SupermarketRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SupermarketRepository {

    private val indexCache = ConcurrentHashMap<String, List<ChainPriceIndex>>()

    override fun observeLocation(): Flow<UserLocation?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            val state = prefs[LocationKeys.STATE]
            val city = prefs[LocationKeys.CITY]
            if (state.isNullOrBlank() || city.isNullOrBlank()) null else UserLocation(state, city)
        }
        .distinctUntilChanged()

    override suspend fun saveLocation(location: UserLocation) {
        dataStore.edit {
            it[LocationKeys.STATE] = location.state.trim().uppercase()
            it[LocationKeys.CITY] = location.city.trim()
        }
    }

    override suspend fun getChainIndexes(location: UserLocation): List<ChainPriceIndex> =
        location.state.trim().uppercase().let { uf -> indexCache.getOrPut(uf) { SupermarketPriceTable.forState(uf) } }
}
