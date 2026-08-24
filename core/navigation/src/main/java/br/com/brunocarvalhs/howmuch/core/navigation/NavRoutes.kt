package br.com.brunocarvalhs.howmuch.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object ShoppingList : NavKey

@Serializable
data object AiChat : NavKey

@Serializable
data object Profile : NavKey

@Serializable
data class JoinList(val token: String? = null) : NavKey

@Serializable
data object Notifications : NavKey
