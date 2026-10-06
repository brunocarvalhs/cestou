package br.com.brunocarvalhs.howmuch.core.ui.components

import java.text.BreakIterator
import java.text.Normalizer

/** First letter of the first and of the last word ("Maria da Silva" -> "MS"); one word -> one letter. */
internal fun avatarInitials(name: String?): String? {
    val words = name?.let { Normalizer.normalize(it, Normalizer.Form.NFC) }
        ?.trim()?.split(Regex("\\s+"))?.filter { it.isNotEmpty() }.orEmpty()
    if (words.isEmpty()) return null
    val first = words.first().firstGrapheme()
    val last = if (words.size > 1) words.last().firstGrapheme() else ""
    return (first + last).uppercase()
}

/** Same person, same color: a stable slot from the seed (user id). */
internal fun avatarColorSlot(seed: String, paletteSize: Int): Int = Math.floorMod(seed.hashCode(), paletteSize)

private fun String.firstGrapheme(): String {
    val iterator = BreakIterator.getCharacterInstance().apply { setText(this@firstGrapheme) }
    return substring(0, iterator.next().coerceAtLeast(1))
}
