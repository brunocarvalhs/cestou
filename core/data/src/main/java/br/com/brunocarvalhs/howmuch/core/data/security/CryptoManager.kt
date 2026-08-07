package br.com.brunocarvalhs.howmuch.core.data.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import timber.log.Timber
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor() {

    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun encryptMap(
        inputMap: Map<String, Any?>,
        excludedKeys: Set<String>
    ): Map<String, Any?> {
        Timber.tag(TAG).d("--> ENCRYPT MAP | Excluded: %s", excludedKeys)
        return inputMap.filterValues { it != null }.mapValues { (key, value) ->
            if (key in excludedKeys) value
            else encryptValue(value)
        }.also {
            Timber.tag(TAG).d("<-- SUCCESS ENCRYPT MAP")
        }
    }

    private fun encryptValue(value: Any?): Any? {
        return when (value) {
            null -> null
            is Map<*, *> -> value.mapValues { encryptValue(it.value) }
            is Iterable<*> -> value.map { encryptValue(it) }
            else -> encrypt(
                input = json.encodeToString(
                    serializer = JsonPrimitive.serializer(),
                    value = value.toJsonElement() as JsonPrimitive
                )
            )
        }
    }

    fun decryptMap(
        encodedMap: Map<String, Any?>,
        excludedKeys: Set<String>
    ): Map<String, Any?> {
        Timber.tag(TAG).d("--> DECRYPT MAP | Excluded: %s", excludedKeys)
        return encodedMap.mapValues { (key, value) ->
            if (key in excludedKeys) value
            else decryptValue(value)
        }.also {
            Timber.tag(TAG).d("<-- SUCCESS DECRYPT MAP")
        }
    }

    private fun decryptValue(value: Any?): Any? {
        return when (value) {
            is String -> {
                val decrypted = decrypt(value)
                if (decrypted == value) return value

                runCatching {
                    json.parseToJsonElement(decrypted).toAny() ?: decrypted
                }.getOrElse { decrypted }
            }
            is Map<*, *> -> {
                value.mapValues { decryptValue(it.value) }
            }
            is Iterable<*> -> {
                value.map { decryptValue(it) }
            }
            else -> value
        }
    }

    fun encrypt(input: String): String {
        Timber.tag(TAG).v("--> ENCRYPT | Input: %s", input)

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(input.toByteArray(Charsets.UTF_8))
            .also {
                Timber.tag(TAG).v("<-- RESULT ENCRYPT: %s", it)
            }
    }

    fun decrypt(encoded: String): String {
        Timber.tag(TAG).v("--> DECRYPT | Input: %s", encoded)

        return runCatching {
            val decodedBytes = Base64.getUrlDecoder().decode(encoded)
            val decoded = String(decodedBytes, Charsets.UTF_8)

            val reEncoded = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(decoded.toByteArray(Charsets.UTF_8))

            if (reEncoded == encoded) decoded else encoded
        }.getOrElse {
            encoded
        }
    }

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is JsonElement -> this
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Iterable<*> -> JsonArray(map { it.toJsonElement() })
        is Map<*, *> -> JsonObject(entries.associate { it.key.toString() to it.value.toJsonElement() })
        else -> JsonPrimitive(toString())
    }

    private fun JsonElement.toAny(): Any? = when (this) {
        is JsonNull -> null
        is JsonPrimitive -> {
            if (isString) content
            else booleanOrNull ?: intOrNull ?: longOrNull ?: doubleOrNull ?: content
        }

        is JsonArray -> map { it.toAny() }
        is JsonObject -> mapValues { it.value.toAny() }
    }

    private companion object {
        const val TAG = "CryptoManager"
    }
}
