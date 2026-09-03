package br.com.brunocarvalhs.howmuch.core.domain.services

import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface StorageService {

    fun <T : Any> observe(key: String, type: KClass<T>, valueType: KType? = null): Flow<T?>

    suspend fun <T : Any> get(key: String, type: KClass<T>, valueType: KType? = null): T?

    suspend fun <T : Any> save(key: String, value: T)

    suspend fun remove(key: String)

    suspend fun clear()
}

inline fun <reified T : Any> StorageService.observe(key: String): Flow<T?> =
    observe(key, T::class, typeOf<T>())

suspend inline fun <reified T : Any> StorageService.get(key: String): T? =
    get(key, T::class, typeOf<T>())
