package br.com.brunocarvalhs.howmuch.core.domain.services

/**
 * Supplies the bearer token attached to outgoing Supabase requests, sourced from the
 * signed-in Firebase Auth user (Firebase Auth stays the identity provider; only the
 * data layer moves to Supabase).
 */
interface AccessTokenProvider {

    suspend fun getToken(forceRefresh: Boolean = false): String?
}
