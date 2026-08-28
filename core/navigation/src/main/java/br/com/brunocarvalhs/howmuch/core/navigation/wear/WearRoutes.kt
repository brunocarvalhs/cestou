package br.com.brunocarvalhs.howmuch.core.navigation.wear

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class ShoppingDetail(val shoppingId: String) : NavKey
