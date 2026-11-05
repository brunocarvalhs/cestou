package br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PsychologyAlt
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Tune
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.extensions.trackAction
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsElements
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.common.contract.AppVersionProvider
import br.com.brunocarvalhs.howmuch.core.common.extensions.openBrowser
import br.com.brunocarvalhs.howmuch.core.common.extensions.openEmail
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.UpdateLanguageUseCase
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiSettings
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.UpdateCurrencyUseCase
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
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.intent.SettingsIntent
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingItem
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingSection
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.state.SettingsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal const val SUPPORT_EMAIL = "brunocarvalhs@outlook.com.br"

internal const val SECTION_ID_GENERAL = "general"
internal const val SECTION_ID_AI = "ai"
internal const val SECTION_ID_SHOPPING = "shopping"
internal const val SECTION_ID_DATA = "data"
internal const val SECTION_ID_NOTIFICATIONS = "notifications"
internal const val SECTION_ID_SUPPORT = "support"
internal const val SECTION_ID_ABOUT = "about"

@HiltViewModel
class SettingsViewModel @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val updateLanguageUseCase: UpdateLanguageUseCase,
    private val updateCurrencyUseCase: UpdateCurrencyUseCase,
    private val analyticsTracker: AnalyticsTracker,
    private val appVersionProvider: AppVersionProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var _navigator: Navigator? = null

    val intent = SettingsIntent(
        onNavigate = { route ->
            analyticsTracker.trackAction(AnalyticsElements.SETTINGS_NAVIGATE)
            _navigator?.navigate(route)
        },
        onBack = { _navigator?.goBack() },
        onSendEmail = { subject ->
            analyticsTracker.trackAction(AnalyticsElements.SETTINGS_SEND_EMAIL)
            context.openEmail(SUPPORT_EMAIL, subject)
        },
        onOpenUrl = { url ->
            analyticsTracker.trackAction(AnalyticsElements.SETTINGS_OPEN_URL)
            context.openBrowser(url)
        },
        onUpdateLanguage = { language ->
            viewModelScope.launch {
                updateLanguageUseCase(language)
                analyticsTracker.trackEvent(
                    AnalyticsEvents.SETTINGS_LANGUAGE_CHANGED,
                    mapOf(AnalyticsParams.LANGUAGE to language)
                )
            }
        },
        onUpdateCurrency = { currency ->
            viewModelScope.launch {
                updateCurrencyUseCase(currency)
                analyticsTracker.trackEvent(
                    AnalyticsEvents.SETTINGS_CURRENCY_CHANGED,
                    mapOf(AnalyticsParams.CURRENCY to currency)
                )
            }
        },
        onToggleSection = { sectionId -> toggleSection(sectionId) }
    )

    init {
        observeSettings()
    }

    private fun observeSettings() {
        settingsRepository.getSettings().onEach { settings ->
            _uiState.update { it.copy(sections = buildSections(settings)) }
        }.launchIn(viewModelScope)
    }

    private fun toggleSection(sectionId: String) {
        val isExpanding = sectionId !in _uiState.value.expandedSectionIds
        _uiState.update { state ->
            val expandedSectionIds = state.expandedSectionIds.toMutableSet()
            if (isExpanding) expandedSectionIds += sectionId else expandedSectionIds -= sectionId
            state.copy(expandedSectionIds = expandedSectionIds)
        }
        analyticsTracker.trackEvent(
            AnalyticsEvents.SETTINGS_SECTION_TOGGLED,
            mapOf(
                AnalyticsParams.SECTION_ID to sectionId,
                AnalyticsParams.EXPANDED to isExpanding
            )
        )
    }

    private fun buildSections(settings: AppSettings): List<SettingSection> {
        return listOf(
            buildGeneralSection(settings),
            buildAiSection(settings),
            buildShoppingSection(settings),
            buildDataSection(),
            buildNotificationsSection(settings),
            buildSupportSection(),
            buildAboutSection()
        )
    }

    private fun buildGeneralSection(settings: AppSettings) = SettingSection(
        id = SECTION_ID_GENERAL,
        title = UiText.StringResource(R.string.settings_section_general),
        icon = Icons.Outlined.Tune,
        items = listOf(
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_theme),
                subtitle = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> UiText.StringResource(R.string.settings_theme_system)
                    ThemeMode.LIGHT -> UiText.StringResource(R.string.settings_theme_light)
                    ThemeMode.DARK -> UiText.StringResource(R.string.settings_theme_dark)
                },
                icon = Icons.Outlined.DarkMode,
                route = ThemeSettings
            ),
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_language),
                subtitle = when (settings.language) {
                    "pt-BR" -> UiText.StringResource(R.string.settings_language_pt)
                    "en" -> UiText.StringResource(R.string.settings_language_en)
                    "es" -> UiText.StringResource(R.string.settings_language_es)
                    else -> UiText.DynamicString(settings.language)
                },
                icon = Icons.Outlined.Language,
                route = LanguageSettings
            ),
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_currency),
                subtitle = when (settings.currency) {
                    "BRL" -> UiText.StringResource(R.string.settings_currency_brl)
                    "USD" -> UiText.StringResource(R.string.settings_currency_usd)
                    "EUR" -> UiText.StringResource(R.string.settings_currency_eur)
                    else -> UiText.DynamicString(settings.currency)
                },
                icon = Icons.Outlined.Payments,
                route = CurrencySettings
            )
        )
    )

    private fun buildAiSection(settings: AppSettings) = SettingSection(
        id = SECTION_ID_AI,
        title = UiText.StringResource(R.string.settings_section_ai),
        icon = Icons.Outlined.SmartToy,
        items = listOf(
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_ai_model),
                subtitle = UiText.DynamicString(settings.aiModel),
                icon = Icons.Outlined.PsychologyAlt,
                route = AiSettings
            ),
        )
    )

    private fun buildShoppingSection(settings: AppSettings) =
        SettingSection(
            id = SECTION_ID_SHOPPING,
            title = UiText.StringResource(R.string.settings_section_shopping),
            icon = Icons.Outlined.ShoppingCart,
            items = listOf(
                SettingItem(
                    title = UiText.StringResource(R.string.settings_item_default_list),
                    subtitle = settings.defaultListId?.let { UiText.DynamicString(it) }
                        ?: UiText.StringResource(R.string.settings_shopping_none_selected),
                    icon = Icons.Outlined.ShoppingCart,
                    route = ShoppingSettings
                ),
            )
        )

    private fun buildDataSection() = SettingSection(
        id = SECTION_ID_DATA,
        title = UiText.StringResource(R.string.settings_section_data),
        icon = Icons.Outlined.Storage,
        items = listOf(
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_delete_all),
                subtitle = UiText.StringResource(R.string.settings_item_delete_all_desc),
                icon = Icons.Outlined.DeleteForever,
                route = DataSettings
            )
        )
    )

    private fun buildNotificationsSection(settings: AppSettings) = SettingSection(
        id = SECTION_ID_NOTIFICATIONS,
        title = UiText.StringResource(R.string.settings_section_notifications),
        icon = Icons.Outlined.NotificationsActive,
        items = listOf(
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_notifications),
                subtitle = if (settings.notificationsEnabled) {
                    UiText.StringResource(R.string.settings_notifications_enabled)
                } else {
                    UiText.StringResource(R.string.settings_notifications_disabled)
                },
                icon = Icons.Outlined.NotificationsActive,
                route = NotificationSettings
            ),
        )
    )

    private fun buildSupportSection() = SettingSection(
        id = SECTION_ID_SUPPORT,
        title = UiText.StringResource(R.string.settings_section_support),
        icon = Icons.Outlined.SupportAgent,
        items = listOf(
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_contact),
                subtitle = UiText.DynamicString(SUPPORT_EMAIL),
                icon = Icons.Outlined.Email,
                route = SupportContact
            ),
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_bug_report),
                subtitle = UiText.StringResource(R.string.settings_item_bug_report_desc),
                icon = Icons.Outlined.BugReport,
                route = SupportBugReport
            ),
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_feedback),
                subtitle = UiText.StringResource(R.string.settings_item_feedback_desc),
                icon = Icons.Outlined.Lightbulb,
                route = SupportFeedback
            ),
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_rate),
                subtitle = UiText.StringResource(R.string.settings_item_rate_desc),
                icon = Icons.Outlined.StarOutline,
                route = AppRate
            )
        )
    )

    private fun buildAboutSection() = SettingSection(
        id = SECTION_ID_ABOUT,
        title = UiText.StringResource(R.string.settings_section_about),
        icon = Icons.Outlined.Info,
        items = listOf(
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_terms),
                icon = Icons.Outlined.Description,
                route = TermsOfUse
            ),
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_privacy),
                icon = Icons.Outlined.Policy,
                route = PrivacyPolicy
            ),
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_licenses),
                icon = Icons.Outlined.Code,
                route = OpenSourceLicenses
            ),
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_release_notes),
                subtitle = UiText.StringResource(R.string.settings_item_release_notes_desc),
                icon = Icons.Outlined.NewReleases,
                route = ReleaseNotes
            ),
            SettingItem(
                title = UiText.StringResource(R.string.settings_item_version),
                subtitle = UiText.DynamicString(appVersionProvider.versionName()),
                icon = Icons.Outlined.Info,
                route = null
            )
        )
    )

    fun setNavigator(navigator: Navigator) {
        _navigator = navigator
    }
}
