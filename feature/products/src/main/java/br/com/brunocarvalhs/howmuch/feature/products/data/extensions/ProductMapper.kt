package br.com.brunocarvalhs.howmuch.feature.products.data.extensions

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.data.model.ProductModel
import br.com.brunocarvalhs.howmuch.feature.products.data.model.toDomain
import br.com.brunocarvalhs.howmuch.feature.products.data.model.toModel

internal fun ProductModel.toDomain(): Product = Product(
    id = id,
    name = name,
    quantity = quantity,
    price = price,
    isPurchased = isPurchased,
    category = category,
    barcode = barcode,
    history = history.mapNotNull { it.toDomain() },
)

internal fun Product.toModel(): ProductModel = ProductModel(
    id = id,
    name = name,
    quantity = quantity,
    price = price,
    isPurchased = isPurchased,
    category = category,
    barcode = barcode,
    history = history.map { it.toModel() },
)
