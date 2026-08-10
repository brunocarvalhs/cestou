package br.com.brunocarvalhs.howmuch.feature.products.data.mapper

import br.com.brunocarvalhs.howmuch.core.domain.entity.Product
import br.com.brunocarvalhs.howmuch.feature.products.data.model.ProductModel

internal fun ProductModel.toDomain(): Product = Product(
    id = id,
    name = name,
    quantity = quantity,
    price = price,
    isPurchased = isPurchased,
    category = category,
    barcode = barcode,
    unit = unit
)

internal fun Product.toModel(): ProductModel = ProductModel(
    id = id,
    name = name,
    quantity = quantity,
    price = price,
    isPurchased = isPurchased,
    category = category,
    barcode = barcode,
    unit = unit
)
