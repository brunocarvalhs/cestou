package br.com.brunocarvalhs.howmuch.feature.chat.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiSettings
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen.AiChatScreen
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel.AiChatViewModel

fun NavGraphBuilder.chatGraph(navigator: Navigator) {
    composable<AiChat> { backStackEntry ->
        val route: AiChat = backStackEntry.toRoute()
        val viewModel: AiChatViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(route.shoppingId) {
            viewModel.setShoppingContext(route.shoppingId)
        }

        AiChatScreen(
            state = uiState,
            intent = viewModel.intent.copy(
                onSettings = { navigator.navigate(AiSettings) },
                onBack = { navigator.goBack() }
            )
        )
    }
}
