package br.com.brunocarvalhs.howmuch.core.domain.model

import br.com.brunocarvalhs.howmuch.core.domain.extensions.orEmpty
import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: String,
    val name: String,
    val quantity: Double,
    val price: Double? = null,
    val isPurchased: Boolean = false,
    val category: String = "Outros",
    val barcode: String? = null,
    val history: List<ProductActivity> = emptyList(),
) {
    val total: Double get() = price.orEmpty() * quantity

    val addedBy: String? get() = history.firstOrNull { it.action == ProductActivity.Action.ADDED }?.userId
    val lastEditedBy: String? get() = history.lastOrNull { it.action == ProductActivity.Action.EDITED }?.userId
    val purchasedBy: String? get() = history.lastOrNull { it.action == ProductActivity.Action.PURCHASED }?.userId
    val lastActivity: ProductActivity? get() = history.maxByOrNull { it.timestamp }
}

fun Product.withActivity(action: ProductActivity.Action, userId: String): Product =
    copy(history = history + ProductActivity(userId = userId, action = action))
