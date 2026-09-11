package br.com.brunocarvalhs.howmuch.core.ui.extensions

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Extension para atualizar o idioma do aplicativo programaticamente.
 */
fun Context.updateAppLanguage(languageCode: String) {
    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(appLocale)
}

/**
 * Idioma configurado no sistema operacional do aparelho, no formato BCP-47
 * (ex.: "pt-BR"), independente de já haver um locale de app aplicado.
 */
fun Context.systemLanguageTag(): String = resources.configuration.locales[0].toLanguageTag()

/**
 * Aplica o idioma do sistema como idioma do app e retorna o código aplicado.
 */
fun Context.applySystemLanguage(): String =
    systemLanguageTag().also { updateAppLanguage(it) }
