package br.com.brunocarvalhs.howmuch.feature.chat.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiSettings
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen.AiChatScreen
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel.AiChatViewModel

fun NavGraphBuilder.chatGraph(navigator: Navigator) {
    composable<AiChat> {
        val viewModel: AiChatViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        AiChatScreen(
            state = uiState,
            intent = viewModel.intent.copy(
                onSettings = { navigator.navigate(AiSettings) }
            )
        )
    }
}
