package br.com.brunocarvalhs.data.model

import br.com.brunocarvalhs.data.constants.EMPTY_STRING
import br.com.brunocarvalhs.data.constants.ONE_INT
import br.com.brunocarvalhs.domain.entities.Product
import java.util.UUID
import androidx.annotation.Keep
import br.com.brunocarvalhs.data.constants.EMPTY_LONG

@Keep
data class ProductModel(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = EMPTY_STRING,
    override val price: Long = EMPTY_LONG,
    override var quantity: Int = ONE_INT,
    override var isChecked: Boolean = false
) : Product {
    override fun toCopy(
        id: String,
        name: String,
        price: Long,
        quantity: Int,
        isChecked: Boolean
    ): Product = copy(
        id = id,
        name = name,
        price = price,
        quantity = quantity,
        isChecked = isChecked
    )
}

internal fun Product.toProductModel(): ProductModel =
    this as? ProductModel ?: ProductModel(
        id = this.id,
        name = this.name,
        price = this.price,
        quantity = this.quantity,
        isChecked = this.isChecked
    )
