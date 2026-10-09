package br.com.brunocarvalhs.howmuch.feature.auth

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.LinkWearDevice
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.CompleteName
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.Welcome
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.authGraph
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.authWearGraph
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen.LinkWearDeviceScreen
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.viewmodel.LinkWearViewModel
import javax.inject.Inject

internal class AuthNavGraph @Inject constructor() : FeatureNavGraph {
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        windowSizeClass: WindowSizeClass
    ) {
        navGraphBuilder.authGraph(
            navigator = navigator,
            onAuthSuccess = {
                navigator.navigate(ShoppingList) {
                    popUpTo(Welcome) { inclusive = true }
                }
            },
            onNameCompleted = {
                navigator.navigate(ShoppingList) {
                    popUpTo(CompleteName) { inclusive = true }
                }
            }
        )
        navGraphBuilder.composable<LinkWearDevice> {
            val viewModel: LinkWearViewModel = hiltViewModel()
            LinkWearDeviceScreen(
                state = viewModel.uiState.collectAsState().value,
                intent = viewModel.intent,
                onBack = { navigator.goBack() }
            )
        }
    }

    override fun registerWearGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator
    ) {
        navGraphBuilder.authWearGraph(navigator)
    }
}
