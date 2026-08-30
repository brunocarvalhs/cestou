package br.com.brunocarvalhs.howmuch.feature.settings.navigation

import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiSettings
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.settings.R
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen.AiSettingsScreen
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen.DataSettingsScreen
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen.LegalContentScreen
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen.NotificationSettingsScreen
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen.OpenSourceLicensesScreen
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen.PlaceholderSettingsScreen
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen.ReleaseNotesScreen
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen.SettingsScreen
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen.ShoppingSettingsScreen
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen.ThemeSettingsScreen
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel.AiSettingsViewModel
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel.DataSettingsViewModel
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel.NotificationSettingsViewModel
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel.SettingsViewModel
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel.ShoppingSettingsViewModel
import br.com.brunocarvalhs.howmuch.feature.settings.presentation.viewmodel.ThemeSettingsViewModel

fun NavGraphBuilder.settingsGraph(
    navigator: Navigator
) {
    generalSettings(navigator)
    advancedSettings(navigator)
    supportSettings(navigator)
    legalSettings(navigator)
}

private fun NavGraphBuilder.generalSettings(navigator: Navigator) {
    composable<Settings> {
        val viewModel: SettingsViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        SettingsScreen(
            state = uiState,
            intent = viewModel.intent
        )
    }

    composable<ThemeSettings> {
        val viewModel: ThemeSettingsViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ThemeSettingsScreen(
            state = uiState,
            intent = viewModel.intent
        )
    }

    composable<LanguageSettings> {
        val viewModel: SettingsViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Simples placeholder ou implementação real
        PlaceholderSettingsScreen(
            title = "Idioma",
            onBack = { navigator.goBack() }
        )
    }

    composable<CurrencySettings> {
        val viewModel: SettingsViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Simples placeholder ou implementação real
        PlaceholderSettingsScreen(
            title = "Moeda",
            onBack = { navigator.goBack() }
        )
    }
}

private fun NavGraphBuilder.advancedSettings(navigator: Navigator) {
    composable<AiSettings> {
        val viewModel: AiSettingsViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        AiSettingsScreen(
            state = uiState,
            intent = viewModel.intent
        )
    }

    composable<ShoppingSettings> {
        val viewModel: ShoppingSettingsViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ShoppingSettingsScreen(
            state = uiState,
            intent = viewModel.intent
        )
    }

    composable<DataSettings> {
        val viewModel: DataSettingsViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        DataSettingsScreen(
            state = uiState,
            intent = viewModel.intent
        )
    }

    composable<NotificationSettings> {
        val viewModel: NotificationSettingsViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        NotificationSettingsScreen(
            state = uiState,
            intent = viewModel.intent
        )
    }
}

private fun NavGraphBuilder.supportSettings(navigator: Navigator) {
    composable<SupportContact> {
        val viewModel: SettingsViewModel = hiltViewModel()
        viewModel.intent.onSendEmail("Suporte ao Usuário")
        navigator.goBack()
    }

    composable<SupportBugReport> {
        val viewModel: SettingsViewModel = hiltViewModel()
        viewModel.intent.onSendEmail("Reportar Bug")
        navigator.goBack()
    }

    composable<SupportFeedback> {
        val viewModel: SettingsViewModel = hiltViewModel()
        viewModel.intent.onSendEmail("Sugestão/Feedback")
        navigator.goBack()
    }

    composable<AppRate> {
        val viewModel: SettingsViewModel = hiltViewModel()
        viewModel.intent.onOpenUrl("https://play.google.com/store/apps/details?id=br.com.brunocarvalhs.howmuch")
        navigator.goBack()
    }
}

private fun NavGraphBuilder.legalSettings(navigator: Navigator) {
    composable<TermsOfUse> {
        LegalContentScreen(
            title = stringResource(R.string.settings_item_terms),
            content = stringResource(R.string.settings_terms_content),
            onBack = { navigator.goBack() }
        )
    }

    composable<PrivacyPolicy> {
        LegalContentScreen(
            title = stringResource(R.string.settings_item_privacy),
            content = stringResource(R.string.settings_privacy_content),
            onBack = { navigator.goBack() }
        )
    }

    composable<OpenSourceLicenses> {
        OpenSourceLicensesScreen(onBack = { navigator.goBack() })
    }

    composable<ReleaseNotes> {
        ReleaseNotesScreen(onBack = { navigator.goBack() })
    }
}
