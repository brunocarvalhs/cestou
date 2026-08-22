package br.com.brunocarvalhs.howmuch.feature.profile.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.Profile
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.screen.ProfileScreen
import br.com.brunocarvalhs.howmuch.feature.profile.presentation.viewmodel.ProfileViewModel

fun NavGraphBuilder.profileGraph(navigator: Navigator) {
    composable<Profile> {
        val viewModel: ProfileViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ProfileScreen(
            state = uiState,
            intent = viewModel.intent
        )
    }
}
