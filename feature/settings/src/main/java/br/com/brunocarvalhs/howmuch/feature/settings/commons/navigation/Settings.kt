package br.com.brunocarvalhs.howmuch.feature.settings.commons.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Settings : NavKey

@Serializable
internal data object ThemeSettings : NavKey

@Serializable
internal data object LanguageSettings : NavKey

@Serializable
internal data object CurrencySettings : NavKey

@Serializable
internal data object ShoppingSettings : NavKey

@Serializable
internal data object DataSettings : NavKey

@Serializable
internal data object NotificationSettings : NavKey

@Serializable
internal data object SupportContact : NavKey

@Serializable
internal data object SupportBugReport : NavKey

@Serializable
internal data object SupportFeedback : NavKey

@Serializable
internal data object AppRate : NavKey

@Serializable
internal data object TermsOfUse : NavKey

@Serializable
internal data object PrivacyPolicy : NavKey

@Serializable
internal data object OpenSourceLicenses : NavKey

@Serializable
internal data object ReleaseNotes : NavKey

@Serializable
internal data object SupportSettings : NavKey

@Serializable
internal data object AboutSettings : NavKey
