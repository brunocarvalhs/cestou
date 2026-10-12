package br.com.brunocarvalhs.howmuch.feature.shopping.data.model

import kotlinx.serialization.Serializable

/** `shortCodes/{code}`: lets a non-member resolve a share code without reading the list (AD-009). */
@Serializable
internal data class ShortCodeModel(
    val id: String,
    val shoppingId: String
) {
    fun toMap(): Map<String, Any?> = mapOf("id" to id, "shoppingId" to shoppingId)
}
