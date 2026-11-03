package br.com.brunocarvalhs.howmuch.feature.settings

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavGraphBuilder
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsScreen
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ScreenName
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiSettings
import br.com.brunocarvalhs.howmuch.core.navigation.toScreenName
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.AppRate
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.CurrencySettings
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.DataSettings
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.LanguageSettings
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.NotificationSettings
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.OpenSourceLicenses
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.PrivacyPolicy
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.ReleaseNotes
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.ShoppingSettings
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.SupportBugReport
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.SupportContact
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.SupportFeedback
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.TermsOfUse
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.ThemeSettings
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.settingsGraph
import javax.inject.Inject
import kotlin.reflect.KClass

internal class SettingsNavGraph @Inject constructor() : FeatureNavGraph {

    override val screenNames: Map<KClass<*>, ScreenName> = mapOf(
        ThemeSettings::class to AnalyticsScreen.SETTINGS_THEME.toScreenName(),
        LanguageSettings::class to AnalyticsScreen.SETTINGS_LANGUAGE.toScreenName(),
        CurrencySettings::class to AnalyticsScreen.SETTINGS_CURRENCY.toScreenName(),
        AiSettings::class to AnalyticsScreen.SETTINGS_AI.toScreenName(),
        ShoppingSettings::class to AnalyticsScreen.SETTINGS_SHOPPING.toScreenName(),
        DataSettings::class to AnalyticsScreen.SETTINGS_DATA.toScreenName(),
        NotificationSettings::class to AnalyticsScreen.SETTINGS_NOTIFICATIONS.toScreenName(),
        SupportContact::class to AnalyticsScreen.SUPPORT_CONTACT.toScreenName(),
        SupportBugReport::class to AnalyticsScreen.SUPPORT_BUG_REPORT.toScreenName(),
        SupportFeedback::class to AnalyticsScreen.SUPPORT_FEEDBACK.toScreenName(),
        AppRate::class to AnalyticsScreen.APP_RATE.toScreenName(),
        TermsOfUse::class to AnalyticsScreen.TERMS_OF_USE.toScreenName(),
        PrivacyPolicy::class to AnalyticsScreen.PRIVACY_POLICY.toScreenName(),
        OpenSourceLicenses::class to AnalyticsScreen.OPEN_SOURCE_LICENSES.toScreenName(),
        ReleaseNotes::class to AnalyticsScreen.RELEASE_NOTES.toScreenName()
    )

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        windowSizeClass: WindowSizeClass
    ) {
        navGraphBuilder.settingsGraph(navigator)
    }
}
