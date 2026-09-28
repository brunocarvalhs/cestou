package br.com.brunocarvalhs.howmuch.feature.profile.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Paywall
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Profile
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.screen.ProfileScreen
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.viewmodel.ProfileSubscriptionViewModel
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.viewmodel.ProfileViewModel

private const val PAYWALL_SOURCE = "profile"

fun NavGraphBuilder.profileGraph(navigator: Navigator) {
    composable<Profile> {
        val viewModel: ProfileViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val subscriptionViewModel: ProfileSubscriptionViewModel = hiltViewModel()
        val subscriptionStatus by subscriptionViewModel.status.collectAsStateWithLifecycle()

        ProfileScreen(
            state = uiState,
            subscriptionStatus = subscriptionStatus,
            intent = viewModel.intent.copy(
                onManageSubscription = { navigator.navigate(Paywall(source = PAYWALL_SOURCE)) }
            )
        )
    }
}
