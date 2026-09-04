package br.com.brunocarvalhs.howmuch.core.data.network

import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import kotlinx.coroutines.flow.Flow

/**
 * Provider-agnostic seam between [NetworkManager] (JSON decode, decrypt, logging) and the
 * concrete backend performing the raw CRUD ([FirebaseFirestoreManager] today,
 * [SupabasePostgrestManager] during/after the Supabase migration).
 */
interface RawDataGateway {

    suspend fun execute(
        endpoint: String,
        method: NetworkService.Method,
        data: Map<String, Any?>? = null,
        query: Map<String, Any?>? = null
    ): Any?

    fun observe(
        endpoint: String,
        query: Map<String, Any?>? = null
    ): Flow<Any?>
}
