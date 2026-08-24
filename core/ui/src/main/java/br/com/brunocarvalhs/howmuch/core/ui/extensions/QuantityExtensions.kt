package br.com.brunocarvalhs.howmuch.core.ui.extensions

/**
 * Product quantities are stored as Double to support weight-based units (e.g. 0.5 kg),
 * but whole counts (e.g. 1 un) shouldn't display the trailing ".0".
 */
fun Double.formatQuantity(): String {
    return if (this == this.toLong().toDouble()) {
        this.toLong().toString()
    } else {
        this.toString()
    }
}
