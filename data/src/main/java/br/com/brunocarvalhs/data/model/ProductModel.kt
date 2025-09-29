package br.com.brunocarvalhs.data.model

import br.com.brunocarvalhs.data.constants.EMPTY_LONG
import br.com.brunocarvalhs.data.constants.EMPTY_STRING
import br.com.brunocarvalhs.data.constants.ONE_INT
import br.com.brunocarvalhs.domain.entities.Product
import java.util.UUID
import androidx.annotation.Keep

@Keep
data class ProductModel(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = EMPTY_STRING,
    override val price: Long = EMPTY_LONG,
    override var quantity: Int = ONE_INT
) : Product {
    override fun toCopy(
        id: String,
        name: String,
        price: Long,
        quantity: Int
    ): Product = copy(id = id, name = name, price = price, quantity = quantity)
}

internal fun Product.toProductModel(): ProductModel =
    this as? ProductModel ?: ProductModel(
        id = this.id,
        name = this.name,
        price = this.price,
        quantity = this.quantity
    )