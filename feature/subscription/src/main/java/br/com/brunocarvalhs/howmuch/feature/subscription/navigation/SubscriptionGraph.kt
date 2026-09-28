package br.com.brunocarvalhs.howmuch.feature.subscription.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Paywall
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.screen.PaywallScreen
import br.com.brunocarvalhs.howmuch.feature.subscription.presentation.viewmodel.PaywallViewModel

fun NavGraphBuilder.subscriptionGraph(navigator: Navigator) {
    composable<Paywall> { backStackEntry ->
        val route: Paywall = backStackEntry.toRoute()
        val viewModel: PaywallViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val activity = LocalActivity.current

        LaunchedEffect(route.source) {
            viewModel.setSource(route.source)
        }

        PaywallScreen(
            state = uiState,
            intent = viewModel.intent.copy(onBack = { navigator.goBack() }),
            activity = activity
        )
    }
}
