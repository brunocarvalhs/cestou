package br.com.brunocarvalhs.howmuch.app.modules.shoppingCart.helpers

import br.com.brunocarvalhs.domain.entities.Product
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.toCurrencyString

fun generateShareableCart(products: List<Product>, totalPrice: Long): String {
    return buildString {
        append("Meu carrinho de compras:\n\n")
        products.forEach { product ->
            append("${product.name} x${product.quantity} - R$ ${product.price.toCurrencyString()}\n")
        }
        append("\n\nTotal: R$ ${totalPrice.toCurrencyString()}")
    }
}

fun generateShareableToken(token: String?): String {
    return buildString {
        append("🎉 Olha só! Você pode acessar meu carrinho de compras usando este token:\n\n")
        append(token.orEmpty())
        append("\n\nBasta inserir o token no app para ver todos os produtos e até adicionar os seus! 🛒")
    }
}
