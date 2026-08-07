package br.com.brunocarvalhs.howmuch.core.domain.entity

data class AuthenticatedUser(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val phoneNumber: String? = null
)
