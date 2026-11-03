package br.com.brunocarvalhs.howmuch.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticatedUser(
    val id: String,
    val email: String? = null,
    val displayName: String? = null,
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    /** Epoch millis em que a conta foi criada; alimenta a faixa de idade da conta no analytics. */
    val createdAt: Long? = null
)
