package br.com.brunocarvalhs.howmuch.core.domain.entity

data class UserProfile(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val partnerId: String? = null
)
