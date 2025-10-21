package br.com.brunocarvalhs.domain.services

import kotlinx.coroutines.flow.Flow

interface Database {
    suspend fun <T> setValue(path: String, value: T)
    suspend fun removeValue(path: String)
    suspend fun <T> getValue(path: String, clazz: Class<T>): T?
    suspend fun <T> queryByChild(path: String, child: String, value: String, clazz: Class<T>): T?
    fun <T> observe(path: String, clazz: Class<T>): Flow<T>
}
