package br.com.brunocarvalhs.howmuch.feature.auth.commons.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.wear.compose.navigation.composable
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.PairingCode
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.LinkPhone
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.wear.screen.PairingCodeWearScreen
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.wear.screen.LinkPhoneWearScreen
import br.com.brunocarvalhs.howmuch.feature.auth.app.presentation.wear.viewmodel.PairingViewModel

fun NavGraphBuilder.authWearGraph(navigator: Navigator) {
    composable(route = PairingCode::class.java.name) {
        val viewModel: PairingViewModel = hiltViewModel()
        PairingCodeWearScreen(
            viewModel = viewModel,
            navigator = navigator
        )
    }

    composable(route = LinkPhone::class.java.name) {
        val viewModel: PairingViewModel = hiltViewModel()
        LinkPhoneWearScreen(
            viewModel = viewModel,
            navigator = navigator
        )
    }
}
