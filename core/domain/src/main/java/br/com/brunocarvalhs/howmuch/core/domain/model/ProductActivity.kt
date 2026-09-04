package br.com.brunocarvalhs.howmuch.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProductActivity(
    val userId: String,
    val action: Action,
    val timestamp: Long = System.currentTimeMillis()
) {
    @Serializable
    enum class Action { ADDED, EDITED, PURCHASED }
}
