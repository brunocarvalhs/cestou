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
import java.text.Normalizer

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

        // Keyed by product name (not category name, unlike KEYWORDS above) so a form can
        // suggest a category as the user types, before any category has been chosen. Each
        // list mixes pt-BR/en/es terms since a product name can be typed in any of the app's
        // locales regardless of the device's current language.
        private val PRODUCT_NAME_KEYWORDS: Map<ProductCategory, List<String>> = mapOf(
            HORTIFRUTI to listOf(
                // pt-BR
                "banana", "maçã", "laranja", "uva", "morango", "abacaxi", "manga",
                "alface", "cenoura", "tomate", "cebola", "batata", "pepino", "pimentão",
                "couve", "brócolis", "abobrinha", "melancia", "limão", "mamão", "pera",
                "abacate", "espinafre",
                // en
                "apple", "orange", "grape", "strawberry", "pineapple", "lettuce",
                "carrot", "cucumber", "broccoli", "zucchini", "watermelon", "lemon",
                "papaya", "avocado", "spinach",
                // es
                "manzana", "fresa", "piña", "lechuga", "zanahoria", "sandía",
                "aguacate", "espinaca", "papa", "patata", "cebolla"
            ),
            CARNES to listOf(
                // pt-BR
                "carne", "frango", "peixe", "linguiça", "bacon", "bisteca", "picanha",
                "alcatra", "costela", "salsicha", "presunto", "filé", "camarão",
                // en
                "beef", "chicken", "fish", "sausage", "steak", "ribs", "fillet",
                "shrimp", "pork", "turkey", "meat",
                // es
                "pollo", "pescado", "chorizo", "tocino", "bistec", "costilla",
                "jamón", "filete", "camarón", "gamba", "cerdo", "pavo"
            ),
            LATICINIOS to listOf(
                // pt-BR
                "leite", "queijo", "manteiga", "iogurte", "requeijão", "margarina",
                "nata", "ricota",
                // en
                "milk", "cheese", "butter", "yogurt", "yoghurt", "heavy cream",
                "sour cream", "cream cheese", "ricotta",
                // es
                "leche", "queso", "mantequilla", "yogur", "crema agria",
                "crema de leche", "requesón"
            ),
            BEBIDAS to listOf(
                // pt-BR
                "refrigerante", "suco", "água", "cerveja", "vinho", "achocolatado",
                "energético", "chá", "refresco",
                // en
                "soda", "juice", "water", "beer", "wine", "energy drink", "tea",
                "soft drink",
                // es
                "jugo", "zumo", "agua", "cerveza", "vino", "té", "bebida energética"
            ),
            LIMPEZA to listOf(
                // pt-BR
                "sabão em pó", "detergente", "desinfetante", "amaciante",
                "água sanitária", "esponja", "alvejante", "limpador",
                // en
                "laundry detergent", "dish soap", "disinfectant", "fabric softener",
                "bleach", "sponge", "cleaner",
                // es
                "desinfectante", "suavizante", "lejía", "limpiador", "lavavajillas"
            ),
            HIGIENE to listOf(
                // pt-BR
                "papel higiênico", "shampoo", "sabonete", "creme dental",
                "escova de dente", "absorvente", "desodorante", "fralda", "pasta de dente",
                // en
                "toilet paper", "soap", "toothpaste", "toothbrush", "sanitary pad",
                "deodorant", "diaper", "conditioner",
                // es
                "papel higiénico", "champú", "jabón", "pasta dental", "dentífrico",
                "cepillo de dientes", "toalla sanitaria", "pañal", "acondicionador"
            ),
            PADARIA to listOf(
                // pt-BR
                "pão", "bolo", "torrada", "croissant", "rosca",
                // en
                "bread", "cake", "toast", "bagel", "bun", "muffin",
                // es
                "pan", "pastel", "tostada", "bollo", "magdalena"
            ),
            CONGELADOS to listOf(
                // pt-BR
                "sorvete", "congelado", "nuggets", "polpa de fruta",
                // en
                "ice cream", "frozen",
                // es
                "helado"
            ),
            MERCEARIA to listOf(
                // pt-BR
                "arroz", "feijão", "macarrão", "óleo", "açúcar", "sal", "farinha",
                "molho", "tempero", "vinagre", "azeite", "biscoito", "bolacha", "café",
                // en
                "rice", "beans", "pasta", "cooking oil", "sugar", "salt", "flour",
                "sauce", "seasoning", "vinegar", "olive oil", "cookie", "cracker", "coffee",
                // es
                "frijoles", "judías", "aceite", "azúcar", "harina", "salsa",
                "condimento", "aceite de oliva", "galleta"
            )
        )

        /**
         * Best-effort guess from a raw product name (e.g. "Arroz" -> [MERCEARIA]), for
         * suggesting a category in real time before the user has picked one explicitly.
         * Returns null rather than [OUTROS] when nothing matches, so callers can tell
         * "no guess yet" apart from an actual OUTROS classification.
         */
        fun suggestFromProductName(name: String): ProductCategory? {
            val normalized = normalize(name)
            if (normalized.isBlank()) return null

            return PRODUCT_NAME_KEYWORDS.entries.firstOrNull { (_, keywords) ->
                keywords.any { normalized.contains(normalize(it)) }
            }?.key
        }

        private fun normalize(value: String): String =
            Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
                .replace("\\p{Mn}+".toRegex(), "")
    }
}
