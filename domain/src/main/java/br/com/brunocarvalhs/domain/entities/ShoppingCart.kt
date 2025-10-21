package br.com.brunocarvalhs.domain.entities

interface ShoppingCart {
    val id: String
    val market: String

    val totalPrice: Long
    val token: String
    val products: MutableList<Product>
    val purchaseDate: String?

    fun addProduct(product: Product): ShoppingCart

    fun removeProduct(productId: String): ShoppingCart

    fun finalizePurchase(
        market: String = this.market,
        price: Long = this.totalPrice
    ): ShoppingCart

    fun toCopy(
        id: String = this.id,
        token: String = this.token,
        products: MutableList<Product> = this.products,
    ): ShoppingCart

    fun cloneCart(): ShoppingCart

    fun recalculateTotal(): Long

    companion object {
        const val TABLE_NAME = "shopping_carts"
        const val ID = "id"
        const val TOKEN = "token"
        const val PRODUCTS = "products"
        const val TOTAL_PRICE = "totalPrice"
        const val PURCHASE_DATE = "purchaseDate"
    }

}
