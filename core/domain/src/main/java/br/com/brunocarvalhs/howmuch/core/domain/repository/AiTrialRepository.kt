package br.com.brunocarvalhs.howmuch.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Tracks whether a Free user has already spent their one free AI chat message. Deliberately
 * has nothing to do with [SubscriptionRepository]/billing — a Pro check combines the two, but
 * this contract by itself is safe for any module (including `:wear`) to depend on.
 */
interface AiTrialRepository {
    fun hasUsedFreeMessage(): Flow<Boolean>
    suspend fun markFreeMessageUsed()
}
