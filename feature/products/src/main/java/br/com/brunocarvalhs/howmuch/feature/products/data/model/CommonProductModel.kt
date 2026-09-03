package br.com.brunocarvalhs.howmuch.feature.products.data.model

import kotlinx.serialization.Serializable

@Serializable
internal data class CommonProductModel(
    val id: String,
    val name: String,
    val category: String = "Outros",
    val unit: String = "un"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "category" to category,
        "unit" to unit
    )
}
