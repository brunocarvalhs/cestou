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
) {
    val total: Double get() = price.orEmpty() * quantity
}
