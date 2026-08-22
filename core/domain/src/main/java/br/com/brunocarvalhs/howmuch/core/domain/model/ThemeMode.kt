package br.com.brunocarvalhs.howmuch.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}
