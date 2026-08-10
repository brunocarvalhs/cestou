package br.com.brunocarvalhs.howmuch.core.domain.entity

import kotlinx.serialization.Serializable

interface User {
    val id: String
    val role: Role

    @Serializable
    enum class Role {
        OWNER,
        EDITOR
    }

    fun toMap(): Map<String, Any?>
}
