package br.com.brunocarvalhs.howmuch.feature.products.data.extensions

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.data.model.ProductModel

internal fun ProductModel.toDomain(): Product = Product(
    id = id,
    name = name,
    quantity = quantity,
    price = price,
    isPurchased = isPurchased,
    category = category,
    barcode = barcode,
)

internal fun Product.toModel(): ProductModel = ProductModel(
    id = id,
    name = name,
    quantity = quantity,
    price = price,
    isPurchased = isPurchased,
    category = category,
    barcode = barcode,
)
