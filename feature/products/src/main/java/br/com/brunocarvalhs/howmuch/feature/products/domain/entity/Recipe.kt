package br.com.brunocarvalhs.howmuch.feature.products.domain.entity

import br.com.brunocarvalhs.howmuch.core.domain.entity.Product

data class Recipe(
    val id: String,
    val name: String,
    val description: String,
    val instructions: String? = null,
    val ingredients: List<Product>,
    val imageUrl: String? = null
)
