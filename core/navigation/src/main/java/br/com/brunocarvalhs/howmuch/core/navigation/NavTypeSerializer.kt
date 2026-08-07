package br.com.brunocarvalhs.howmuch.core.navigation

import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.json.Json
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

val navJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

inline fun <reified T : Any> navTypeSerializer(): NavType<T> = object : NavType<T>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): T? {
        return bundle.getString(key)?.let { navJson.decodeFromString(it) }
    }

    override fun parseValue(value: String): T {
        return navJson.decodeFromString(URLDecoder.decode(value, StandardCharsets.UTF_8.name()))
    }

    override fun put(bundle: Bundle, key: String, value: T) {
        bundle.putString(key, navJson.encodeToString(value))
    }

    override fun serializeAsValue(value: T): String {
        return URLEncoder.encode(navJson.encodeToString(value), StandardCharsets.UTF_8.name())
    }
}

internal inline fun <reified T : Any> navTypeListSerializer(): NavType<List<T>> =
    object : NavType<List<T>>(isNullableAllowed = false) {
        override fun get(bundle: Bundle, key: String): List<T>? {
            return bundle.getString(key)?.let { navJson.decodeFromString(it) }
        }

        override fun parseValue(value: String): List<T> {
            return navJson.decodeFromString(URLDecoder.decode(value, StandardCharsets.UTF_8.name()))
        }

        override fun put(bundle: Bundle, key: String, value: List<T>) {
            bundle.putString(key, navJson.encodeToString(value))
        }

        override fun serializeAsValue(value: List<T>): String {
            return URLEncoder.encode(navJson.encodeToString(value), StandardCharsets.UTF_8.name())
        }
    }

inline fun <reified T : Any> navTypeSerializerNullable(): NavType<T?> =
    object : NavType<T?>(isNullableAllowed = true) {

        override fun get(bundle: Bundle, key: String): T? {
            return bundle.getString(key)?.let { navJson.decodeFromString(it) }
        }

        override fun parseValue(value: String): T? {
            return if (value == "null") null
            else navJson.decodeFromString(
                URLDecoder.decode(value, StandardCharsets.UTF_8.name())
            )
        }

        override fun put(bundle: Bundle, key: String, value: T?) {
            bundle.putString(
                key, value?.let { navJson.encodeToString(it) })
        }

        override fun serializeAsValue(value: T?): String {
            return value?.let {
                URLEncoder.encode(navJson.encodeToString(value), StandardCharsets.UTF_8.name())
            } ?: "null"
        }
    }
