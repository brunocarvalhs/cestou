package br.com.brunocarvalhs.data.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import br.com.brunocarvalhs.data.extensions.dataStore
import br.com.brunocarvalhs.domain.services.Database
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class LocalRealtimeService(
    private val context: Context,
    private val gson: Gson = Gson()
) : Database {

    private val observers = mutableMapOf<String, MutableSharedFlow<Any>>()

    override suspend fun <T> setValue(path: String, value: T) {
        val json = gson.toJson(value)
        val key = stringPreferencesKey(path)

        context.dataStore.edit { prefs ->
            prefs[key] = json
        }

        notifyObservers(path, value as Any)
    }

    override suspend fun removeValue(path: String) {
        val key = stringPreferencesKey(path)
        context.dataStore.edit { prefs ->
            prefs.remove(key)
        }
        notifyObservers(path, Unit)
    }

    override suspend fun <T> getValue(path: String, clazz: Class<T>): T? {
        val key = stringPreferencesKey(path)
        val json = context.dataStore.data
            .map { it[key] }
            .firstOrNull()
        return json?.let { gson.fromJson(it, clazz) }
    }

    override suspend fun <T> queryByChild(path: String, child: String, value: String, clazz: Class<T>): T? {
        val data = getValue(path, clazz) ?: return null
        val fieldValue = clazz.declaredFields
            .firstOrNull { it.name == child }
            ?.apply { isAccessible = true }
            ?.get(data)
        return if (fieldValue?.toString() == value) data else null
    }

    override fun <T> observe(path: String, clazz: Class<T>): Flow<T> {
        val key = stringPreferencesKey(path)
        val baseFlow = context.dataStore.data.map { prefs ->
            prefs[key]?.let { gson.fromJson(it, clazz) }
        }
        val sharedFlow = observers.getOrPut(path) { MutableSharedFlow(replay = 1) }

        return kotlinx.coroutines.flow.merge(
            baseFlow.mapNotNull { it },
            sharedFlow as Flow<T>
        )
    }

    private fun notifyObservers(path: String, value: Any) {
        observers[path]?.tryEmit(value)
    }
}
