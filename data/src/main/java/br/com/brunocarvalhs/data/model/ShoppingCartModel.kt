package br.com.brunocarvalhs.data.model

import android.os.Build
import br.com.brunocarvalhs.data.constants.EMPTY_LONG
import br.com.brunocarvalhs.data.constants.EMPTY_STRING
import br.com.brunocarvalhs.data.extensions.randomToken
import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.domain.entities.ShoppingCart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import androidx.annotation.Keep

@Keep
data class ShoppingCartModel(
    override val id: String = UUID.randomUUID().toString(),
    override val token: String = randomToken(),
    val items: MutableList<ProductModel> = mutableListOf(),
    override val market: String = EMPTY_STRING,
    override val totalPrice: Long = EMPTY_LONG,
    override val purchaseDate: String? = null,
) : ShoppingCart {

    override val products: MutableList<Product>
        get() = items as MutableList<Product>

    override fun addProduct(product: Product): ShoppingCart = apply {
        val existingProduct = products.find { it.id == product.id }
        if (existingProduct != null) {
            existingProduct.quantity += product.quantity
        } else {
            this.products.add(product.toProductModel())
        }
    }

    override fun removeProduct(productId: String): ShoppingCart = apply {
        products.removeAll { it.id == productId }
    }

    fun updateProduct(product: Product): ShoppingCart = apply {
        products.replaceAll { if (it.id == product.id) product.toProductModel() else it }
    }

    override fun finalizePurchase(market: String, price: Long): ShoppingCart {
        val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_DATE)
        } else {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

        return this.copy(
            market = market,
            purchaseDate = today,
            totalPrice = price
        )
    }


    override fun toCopy(
        id: String,
        token: String,
        products: MutableList<Product>,
    ): ShoppingCart {
        val productModels = products.map {
            it as? ProductModel ?: ProductModel(
                it.id,
                it.name,
                it.price,
                it.quantity
            )
        }.toMutableList()
        return ShoppingCartModel(id = id, token = token, items = productModels)
    }

    override fun recalculateTotal(): Long {
        return products.sumOf { it.price * it.quantity }
    }
}

internal fun ShoppingCart.toShoppingCartModel(): ShoppingCartModel =
    this as? ShoppingCartModel ?: ShoppingCartModel(
        id = this.id,
        token = this.token,
        items = this.products.map { it.toProductModel() }.toMutableList(),
        market = this.market,
        totalPrice = this.totalPrice,
        purchaseDate = this.purchaseDate
    )