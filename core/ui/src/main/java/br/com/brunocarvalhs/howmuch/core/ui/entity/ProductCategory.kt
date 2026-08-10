package br.com.brunocarvalhs.howmuch.core.ui.entity

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.brunocarvalhs.howmuch.core.ui.R

enum class ProductCategory(
    @StringRes val displayNameRes: Int,
    val icon: ImageVector,
    val color: Color
) {
    HORTIFRUTI(R.string.category_hortifruti, Icons.Default.Eco, Color(0xFF4CAF50)),
    CARNES(R.string.category_carnes, Icons.Default.Restaurant, Color(0xFFF44336)),
    LATICINIOS(R.string.category_laticinios, Icons.Default.Egg, Color(0xFFFFEB3B)),
    BEBIDAS(R.string.category_bebidas, Icons.Default.LocalDrink, Color(0xFF2196F3)),
    LIMPEZA(R.string.category_limpeza, Icons.Default.CleaningServices, Color(0xFF9C27B0)),
    HIGIENE(R.string.category_higiene, Icons.Default.Face, Color(0xFFE91E63)),
    MERCEARIA(R.string.category_mercearia, Icons.Default.Inventory, Color(0xFFFF9800)),
    LEGUMES(R.string.category_legumes, Icons.Default.Scale, Color(0xFF8BC34A)),
    PERECIVEIS(R.string.category_pereciveis, Icons.Default.Kitchen, Color(0xFFFF5722)),
    CONGELADOS(R.string.category_congelados, Icons.Default.AcUnit, Color(0xFF00BCD4)),
    PADARIA(R.string.category_padaria, Icons.Default.BakeryDining, Color(0xFF795548)),
    OUTROS(R.string.category_outros, Icons.Default.Category, Color(0xFF9E9E9E));

    companion object {
        private val KEYWORDS = mapOf(
            LEGUMES to listOf("legum", "vegetal"),
            PERECIVEIS to listOf("pereciv", "fresco"),
            CONGELADOS to listOf("congel", "frozen"),
            HORTIFRUTI to listOf("fruit", "veg", "horti"),
            CARNES to listOf("meat", "carne"),
            LATICINIOS to listOf("dairy", "milk", "leite", "lati"),
            BEBIDAS to listOf("beverage", "drink", "bebi"),
            LIMPEZA to listOf("clean", "limp"),
            HIGIENE to listOf("hygiene", "higi", "beauty"),
            MERCEARIA to listOf("grocery", "merc"),
            PADARIA to listOf("bakery", "bread", "pada")
        )

        fun fromString(value: String?): ProductCategory {
            val normalized = value?.lowercase() ?: return OUTROS

            for ((category, keys) in KEYWORDS) {
                if (keys.any { normalized.contains(it) }) return category
            }

            return entries.find { it.name.equals(value, ignoreCase = true) } ?: OUTROS
        }
    }
}
