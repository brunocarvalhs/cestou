package br.com.brunocarvalhs.howmuch.core.common.wearable

/** Separator between the fields of a payload (UTF-8, see [WearAiCodec]). */
private const val SEPARATOR = "\n"

/** Joins [head] fields, then the free-text [tail] as the last field, which may contain the separator. */
internal fun joinWithTail(vararg head: String, tail: String): ByteArray =
    (head.toList() + tail).joinToString(SEPARATOR).toByteArray()

/**
 * Splits [headCount] required fields plus a free-text tail. Returns (head, tail), or null when a
 * head field is blank. The tail is everything after the head fields, separators included.
 */
internal fun splitWithTail(data: ByteArray, headCount: Int): Pair<List<String>, String>? {
    val parts = String(data).split(SEPARATOR, limit = headCount + 1)
    if (parts.size != headCount + 1) return null
    val head = parts.take(headCount)
    return if (head.all { it.isNotBlank() }) head to parts.last() else null
}

/** Like [splitWithTail], but the free-text tail is required too: a blank one is rejected. */
internal fun splitRequiredText(data: ByteArray, headCount: Int): Pair<String, String>? =
    splitWithTail(data, headCount)?.let { (head, tail) ->
        if (tail.isBlank()) null else head[0] to tail
    }
