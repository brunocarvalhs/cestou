package br.com.brunocarvalhs.howmuch.core.data.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class UserProfileModel(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val photoUrl: String? = null
)
