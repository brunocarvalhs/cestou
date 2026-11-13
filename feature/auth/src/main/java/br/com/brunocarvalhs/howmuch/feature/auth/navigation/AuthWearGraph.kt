package br.com.brunocarvalhs.howmuch.feature.auth.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.wear.compose.navigation.composable
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.LinkPhone
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.PairingWear
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.screen.LinkPhoneWearScreen
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.screen.PairingWearScreen
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.viewmodel.PairingViewModel

internal fun NavGraphBuilder.authWearGraph(navigator: Navigator) {
    composable(route = PairingWear::class.java.name) {
        val viewModel: PairingViewModel = hiltViewModel()
        PairingWearScreen(
            viewModel = viewModel,
            navigator = navigator
        )
    }

    composable(route = LinkPhone::class.java.name) {
        LinkPhoneWearScreen(navigator = navigator)
    }
}
