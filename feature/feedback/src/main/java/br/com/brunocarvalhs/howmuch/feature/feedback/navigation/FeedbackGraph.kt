package br.com.brunocarvalhs.howmuch.feature.feedback.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.FeedbackSurvey
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.StoreReview
import br.com.brunocarvalhs.howmuch.feature.feedback.presentation.screen.FeedbackSurveyScreen
import br.com.brunocarvalhs.howmuch.feature.feedback.presentation.viewmodel.FeedbackSurveyViewModel

internal fun NavGraphBuilder.feedbackGraph(navigator: Navigator) {
    dialog<FeedbackSurvey> { backStackEntry ->
        val route: FeedbackSurvey = backStackEntry.toRoute()
        val viewModel: FeedbackSurveyViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val activity = LocalActivity.current

        LaunchedEffect(route.kind, route.feature) {
            viewModel.setPrompt(route.kind, route.feature)
        }

        FeedbackSurveyScreen(
            state = uiState,
            intent = viewModel.intent.copy(
                onDismiss = {
                    viewModel.intent.onDismiss()
                    navigator.goBack()
                }
            )
        )
    }

    dialog<StoreReview> {
        val viewModel: FeedbackSurveyViewModel = hiltViewModel()
        val activity = LocalActivity.current

        LaunchedEffect(Unit) {
            // Sem tela: o fluxo da Play é uma folha própria sobre o app; ao terminar, a rota some.
            activity?.let { viewModel.requestStoreReview(it) }
            navigator.goBack()
        }
    }
}
