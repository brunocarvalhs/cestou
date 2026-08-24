package br.com.brunocarvalhs.howmuch.feature.profile.commons.navigation.wear

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.wear.compose.navigation.composable
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.Profile
import br.com.brunocarvalhs.howmuch.feature.profile.app.presentation.wear.screen.ProfileWearScreen
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.viewmodel.ProfileViewModel

fun NavGraphBuilder.profileWearGraph(navigator: Navigator) {
    composable(Profile::class.java.name) {
        val viewModel: ProfileViewModel = hiltViewModel()
        ProfileWearScreen(viewModel = viewModel)
    }
}
