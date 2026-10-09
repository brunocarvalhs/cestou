package br.com.brunocarvalhs.howmuch.feature.products.domain.exception

/** The captured image could not be opened or decoded, so there is nothing to analyze. */
class ImageUnreadableException(cause: Throwable? = null) : Exception("Image could not be read", cause)
