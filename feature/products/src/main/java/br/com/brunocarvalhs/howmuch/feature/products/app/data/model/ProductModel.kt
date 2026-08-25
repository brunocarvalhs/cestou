package br.com.brunocarvalhs.howmuch.feature.products.app.data.model

import br.com.brunocarvalhs.howmuch.core.domain.extensions.orEmpty
import kotlinx.serialization.Serializable

@Serializable
data class ProductModel(
    val id: String,
    val name: String,
    val quantity: Double,
    val price: Double? = null,
    val isPurchased: Boolean = false,
    val category: String = "Outros",
    val barcode: String? = null,
    val unit: String = "un"
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "quantity" to quantity,
            "price" to price,
            "isPurchased" to isPurchased,
            "category" to category,
            "barcode" to barcode,
            "unit" to unit,
            "total" to price.orEmpty() * quantity
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any?>): ProductModel {
            return ProductModel(
                id = map["id"] as String,
                name = map["name"] as String,
                quantity = (map["quantity"] as? Number)?.toDouble() ?: 1.0,
                price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                isPurchased = map["isPurchased"] as? Boolean ?: false,
                category = map["category"] as? String ?: "Outros",
                barcode = map["barcode"] as? String,
                unit = map["unit"] as? String ?: "un"
            )
        }
    }
}
