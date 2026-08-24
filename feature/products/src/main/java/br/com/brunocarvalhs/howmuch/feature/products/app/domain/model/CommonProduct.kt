package br.com.brunocarvalhs.howmuch.feature.products.app.domain.model

data class CommonProduct(
    val id: String,
    val name: String,
    val category: String = "Outros",
    val unit: String = "un"
)
