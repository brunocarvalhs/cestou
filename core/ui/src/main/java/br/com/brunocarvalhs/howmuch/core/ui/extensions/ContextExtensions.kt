package br.com.brunocarvalhs.howmuch.core.ui.extensions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import br.com.brunocarvalhs.howmuch.core.ui.R

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

/**
 * Idioma atualmente em vigor no app: o locale explicitamente aplicado via
 * [AppCompatDelegate], ou o idioma do sistema quando nenhum foi definido.
 */
fun Context.currentAppLocaleTag(): String =
    AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag() ?: systemLanguageTag()

/**
 * Nome de exibição e código BCP-47 de cada idioma suportado pelo app,
 * na mesma ordem declarada em `supported_languages` / `supported_languages_codes`.
 */
fun Context.supportedLanguages(): List<Pair<String, String>> {
    val names = resources.getStringArray(R.array.supported_languages)
    val codes = resources.getStringArray(R.array.supported_languages_codes)
    return names.zip(codes)
}

fun Context.appVersionName(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager
            .getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
            .versionName
            .orEmpty()
    } else {
        packageManager
            .getPackageInfo(packageName, 0)
            .versionName
            .orEmpty()
    }
}
