package br.com.brunocarvalhs.howmuch.feature.shopping.data.mapper

import br.com.brunocarvalhs.howmuch.feature.shopping.data.model.ShoppingModel
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping

internal fun ShoppingModel.toDomain(): Shopping = Shopping(
    id = id,
    title = title,
    description = description,
    price = price,
    status = status,
    users = users,
    roles = roles,
    createdAt = createdAt,
    isFavorite = isFavorite,
    isCategorized = isCategorized,
    shortCode = shortCode,
    budget = budget,
    position = position
)

internal fun Shopping.toModel(): ShoppingModel = ShoppingModel(
    id = id,
    title = title,
    description = description,
    price = price,
    status = status,
    users = users,
    roles = roles,
    createdAt = createdAt,
    isFavorite = isFavorite,
    isCategorized = isCategorized,
    shortCode = shortCode,
    budget = budget,
    position = position
)
