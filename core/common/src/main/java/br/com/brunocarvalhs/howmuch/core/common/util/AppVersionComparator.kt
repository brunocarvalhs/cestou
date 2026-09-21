package br.com.brunocarvalhs.howmuch.core.common.util

/**
 * Compara versionNames no formato "major.minor.patch" (ex: "1.3.0") sem depender de uma
 * lib de semver externa. Segmentos não numéricos ou ausentes contam como 0.
 */
object AppVersionComparator : Comparator<String> {

    override fun compare(a: String, b: String): Int {
        val partsA = a.toVersionParts()
        val partsB = b.toVersionParts()
        val length = maxOf(partsA.size, partsB.size)

        for (index in 0 until length) {
            val diff = partsA.getOrElse(index) { 0 } - partsB.getOrElse(index) { 0 }
            if (diff != 0) return diff
        }
        return 0
    }

    private fun String.toVersionParts(): List<Int> =
        split(".").map { segment -> segment.filter(Char::isDigit).toIntOrNull() ?: 0 }
}

fun String.isAtLeastVersion(other: String): Boolean = AppVersionComparator.compare(this, other) >= 0

fun String.isAtMostVersion(other: String): Boolean = AppVersionComparator.compare(this, other) <= 0
