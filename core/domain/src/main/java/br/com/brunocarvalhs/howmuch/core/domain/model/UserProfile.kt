package br.com.brunocarvalhs.howmuch.core.domain.model

data class UserProfile(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val photoUrl: String? = null
)
