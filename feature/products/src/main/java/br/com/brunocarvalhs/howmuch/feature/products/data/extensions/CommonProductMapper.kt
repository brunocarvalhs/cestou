package br.com.brunocarvalhs.howmuch.feature.products.data.extensions

import br.com.brunocarvalhs.howmuch.feature.products.data.model.CommonProductModel
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.CommonProduct

internal fun CommonProductModel.toDomain(): CommonProduct = CommonProduct(
    id = id,
    name = name,
    category = category,
    unit = unit
)

internal fun CommonProduct.toModel(): CommonProductModel = CommonProductModel(
    id = id,
    name = name,
    category = category,
    unit = unit
)
