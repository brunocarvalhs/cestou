package br.com.brunocarvalhs.howmuch.core.data.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.lang.reflect.Array
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompatibilityConverter @Inject constructor() {

    fun toJsonElement(data: Any?): JsonElement = when (data) {
        null -> JsonNull
        is JsonElement -> data
        is Boolean -> JsonPrimitive(data)
        is Number -> JsonPrimitive(data)
        is String -> JsonPrimitive(data)
        is com.google.firebase.Timestamp -> JsonPrimitive(data.toDate().time)
        is Iterable<*> -> JsonArray(data.map { toJsonElement(it) })
        is Map<*, *> -> {
            val keys = data.keys.mapNotNull { it?.toString() }
            
            val isNumericIndexed = keys.isNotEmpty() && keys.all { it.toIntOrNull() != null }
            val valuesAreObjects = data.values.all { it is Map<*, *> }

            if (isNumericIndexed) {
                val sortedList = data.entries
                    .sortedBy { it.key.toString().toInt() }
                    .map { toJsonElement(it.value) }
                JsonArray(sortedList)
            } else if (valuesAreObjects && keys.isNotEmpty()) {
                JsonArray(data.values.map { toJsonElement(it) })
            } else {
                JsonObject(data.entries.associate { it.key.toString() to toJsonElement(it.value) })
            }
        }
        else -> JsonPrimitive(data.toString())
    }

    fun listToTypedArray(list: List<*>, arrayClass: Class<*>): Any? {
        val componentType = arrayClass.componentType ?: return null
        val array = Array.newInstance(componentType, list.size)
        for (i in list.indices) {
            val item = list[i]
            if (item != null && componentType.isInstance(item)) {
                Array.set(array, i, item)
            }
        }
        return array
    }
}
