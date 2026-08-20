package br.com.brunocarvalhs.howmuch.core.data.mapper

import br.com.brunocarvalhs.howmuch.core.data.model.UserProfileModel
import br.com.brunocarvalhs.howmuch.core.domain.entity.UserProfile

fun UserProfileModel.toDomain() = UserProfile(
    id = id,
    name = name,
    email = email,
    photoUrl = photoUrl,
    partnerId = partnerId
)

fun UserProfile.toModel() = UserProfileModel(
    id = id,
    name = name,
    email = email,
    photoUrl = photoUrl,
    partnerId = partnerId
)

fun UserProfileModel.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "email" to email,
    "photoUrl" to photoUrl,
    "partnerId" to partnerId
)
