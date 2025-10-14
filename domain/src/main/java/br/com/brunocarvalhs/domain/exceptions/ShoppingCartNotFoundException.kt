package br.com.brunocarvalhs.domain.exceptions

class ShoppingCartNotFoundException(cartId: String? = null) :
    Exception("ShoppingCart ${cartId?.let { "with id $it" }} not found")
