package br.com.brunocarvalhs.domain.entities

interface Product {
    val id: String
    val name: String
    val price: Long?
    var quantity: Int
    var isChecked: Boolean

    fun toCopy(
        id: String = this.id,
        name: String = this.name,
        price: Long? = this.price,
        quantity: Int = this.quantity,
        isChecked: Boolean = this.isChecked
    ): Product
}
