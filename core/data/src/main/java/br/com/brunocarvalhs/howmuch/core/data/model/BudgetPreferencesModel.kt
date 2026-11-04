package br.com.brunocarvalhs.howmuch.core.data.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

/** Documento `users/{uid}/preferences/budget`. */
@OptIn(InternalSerializationApi::class)
@Serializable
data class BudgetPreferencesModel(
    val id: String? = null,
    val monthlyGoal: Double? = null
)
