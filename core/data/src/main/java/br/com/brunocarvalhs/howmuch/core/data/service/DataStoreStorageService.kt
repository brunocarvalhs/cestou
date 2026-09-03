package br.com.brunocarvalhs.howmuch.core.data.service

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.brunocarvalhs.howmuch.core.domain.services.StorageService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Generic [StorageService] backed by a single Preferences [DataStore], with values encoded as
 * JSON (except [String], stored raw) so callers can persist arbitrary serializable types under
 * a string key without hand-rolling a `Preferences.Key` for each one.
 */
@OptIn(InternalSerializationApi::class)
class DataStoreStorageService @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : StorageService {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun <T : Any> observe(key: String, type: KClass<T>, valueType: KType?): Flow<T?> =
        dataStore.data
            .catch { error ->
                if (error is IOException) emit(emptyPreferences()) else throw error
            }
            .map { preferences -> decode(key, preferences[stringPreferencesKey(key)], type, valueType) }

    override suspend fun <T : Any> get(key: String, type: KClass<T>, valueType: KType?): T? {
        val preferences = dataStore.data.firstOrNull() ?: return null
        return decode(key, preferences[stringPreferencesKey(key)], type, valueType)
    }

    override suspend fun <T : Any> save(key: String, value: T) {
        val raw = encode(value)
        dataStore.edit { preferences -> preferences[stringPreferencesKey(key)] = raw }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { preferences -> preferences.remove(stringPreferencesKey(key)) }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    private fun <T : Any> decode(key: String, raw: String?, type: KClass<T>, valueType: KType?): T? {
        if (raw == null) return null
        return try {
            if (type == String::class) {
                @Suppress("UNCHECKED_CAST")
                raw as T
            } else {
                @Suppress("UNCHECKED_CAST")
                val serializer = (valueType?.let { json.serializersModule.serializer(it) }
                    ?: type.serializer()) as KSerializer<T>
                json.decodeFromString(serializer, raw)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to decode value for key [%s]", key)
            null
        }
    }

    private fun <T : Any> encode(value: T): String = if (value is String) {
        value
    } else {
        @Suppress("UNCHECKED_CAST")
        val serializer = value::class.serializer() as KSerializer<T>
        json.encodeToString(serializer, value)
    }

    companion object {
        private const val TAG = "DataStoreStorageService"
    }
}
