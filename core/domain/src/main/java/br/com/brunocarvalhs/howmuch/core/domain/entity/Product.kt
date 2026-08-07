package br.com.brunocarvalhs.howmuch.core.domain.entity

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class Product(
    val id: String,
    val name: String,
    val quantity: Double,
    val price: Double,
    val isPurchased: Boolean = false,
    val category: String = "Outros",
    val barcode: String? = null,
    val unit: String = "un"
) {
    val total: Double get() = price * quantity
}
