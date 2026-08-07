package br.com.brunocarvalhs.howmuch.feature.shopping.data.model

import br.com.brunocarvalhs.howmuch.feature.products.data.model.ProductModel
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ShoppingModel(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val status: Shopping.Status,
    val users: List<String>,
    val roles: Map<String, String>,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isCategorized: Boolean = true,
    val shortCode: String? = null,
    val budget: Double? = null,
    val position: Int = 0
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "title" to title,
            "description" to description,
            "price" to price,
            "status" to status.name,
            "users" to users,
            "roles" to roles,
            "createdAt" to createdAt,
            "isFavorite" to isFavorite,
            "isCategorized" to isCategorized,
            "shortCode" to shortCode,
            "budget" to budget,
            "position" to position
        )
    }
}
