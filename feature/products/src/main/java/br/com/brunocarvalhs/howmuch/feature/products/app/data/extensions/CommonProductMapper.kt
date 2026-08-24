package br.com.brunocarvalhs.howmuch.feature.products.app.data.extensions

import br.com.brunocarvalhs.howmuch.feature.products.app.data.model.CommonProductModel
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.model.CommonProduct

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
