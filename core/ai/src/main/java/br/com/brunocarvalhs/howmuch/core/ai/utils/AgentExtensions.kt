package br.com.brunocarvalhs.howmuch.core.ai.utils

/**
 * Extensões para facilitar a recuperação de parâmetros enviados pela IA (Gemini/OpenRouter).
 * Trata conversões de JsonLiteral e tipos primitivos de forma robusta.
 */

fun Map<String, Any?>.getString(key: String): String? {
    return this[key]?.toString()?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotEmpty() }
}

fun Map<String, Any?>.getInt(key: String): Int? {
    val value = this[key]
    if (value is Number) return value.toInt()
    return getString(key)?.toIntOrNull()
}

fun Map<String, Any?>.getDouble(key: String): Double? {
    val value = this[key]
    if (value is Number) return value.toDouble()
    return getString(key)?.toDoubleOrNull()
}

fun Map<String, Any?>.getBoolean(key: String): Boolean? {
    val value = this[key]
    if (value is Boolean) return value
    val str = getString(key)?.lowercase()
    return when (str) {
        "true", "1", "yes", "sim" -> true
        "false", "0", "no", "não" -> false
        else -> null
    }
}

/**
 * Retorna uma lista de strings a partir de um argumento (útil para listas de IDs ou nomes)
 */
fun Map<String, Any?>.getList(key: String): List<String> {
    val value = this[key]
    if (value is List<*>) return value.mapNotNull { it?.toString()?.removeSurrounding("\"") }
    
    // Se vier como uma string separada por vírgula
    return getString(key)?.split(",")?.map { it.trim() } ?: emptyList()
}

/**
 * Retorna um mapa de String -> String a partir de um argumento.
 */
fun Map<String, Any?>.getMap(key: String): Map<String, String> {
    return when (val value = this[key]) {
        is Map<*, *> -> value.entries.associate { (k, v) ->
            k.toString().removeSurrounding("\"") to
                (v?.toString()?.removeSurrounding("\"") ?: "")
        }

        else -> emptyMap()
    }
}
