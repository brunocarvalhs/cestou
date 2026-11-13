package br.com.brunocarvalhs.howmuch.feature.chat.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.toRoute
import androidx.navigation3.runtime.NavKey
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.RouteProtocol
import br.com.brunocarvalhs.howmuch.core.navigation.RouteType
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiSettings
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Paywall
import br.com.brunocarvalhs.howmuch.core.ui.contract.LocalAiInterstitial
import br.com.brunocarvalhs.howmuch.core.ui.util.findActivity
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen.AiChatScreen
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen.AiHistoryScreen
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel.AiChatViewModel
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel.AiHistoryViewModel
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel.ChatAiGateViewModel
import kotlinx.serialization.Serializable

private const val PAYWALL_SOURCE = "ai_chat"

@Serializable
data object AiHistory : NavKey, RouteProtocol {
    override val routeType: RouteType = RouteType.PROTECTED
}

/**
 * Intersticial de IA do plano FREE (sdd-059). Prepara o anúncio (consentimento e carregamento)
 * ao abrir o chat e o mostra quando o ViewModel sinaliza o primeiro uso da mensagem gratuita.
 * Sem provedor de anúncios (ex.: `:wear`), [LocalAiInterstitial] não faz nada.
 */
@Composable
private fun AiInterstitialEffects(gateViewModel: ChatAiGateViewModel) {
    val activity = LocalContext.current.findActivity()
    val aiInterstitial = LocalAiInterstitial.current

    LaunchedEffect(activity) {
        if (activity != null && gateViewModel.isAiInterstitialEligible()) {
            aiInterstitial.prepare(activity)
        }
    }
    LaunchedEffect(activity) {
        gateViewModel.aiInterstitialRequests.collect {
            if (activity != null && gateViewModel.isAiInterstitialEligible()) {
                aiInterstitial.showIfReady(activity)
            }
        }
    }
}

fun NavGraphBuilder.chatGraph(navigator: Navigator) {
    dialog<AiChat> { backStackEntry ->
        val route: AiChat = backStackEntry.toRoute()
        val viewModel: AiChatViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        // `also` é inline: os efeitos do intersticial (sdd-059) compõem junto com o ViewModel.
        val gateViewModel = hiltViewModel<ChatAiGateViewModel>().also { AiInterstitialEffects(it) }
        val isSendEnabled by gateViewModel.isSendEnabled.collectAsStateWithLifecycle()
        LaunchedEffect(route.shoppingId, route.sessionId) {
            val sessionId = route.sessionId
            val shoppingId = route.shoppingId
            when {
                sessionId != null -> viewModel.loadSession(sessionId)
                shoppingId != null -> viewModel.setShoppingContext(shoppingId)
            }
        }

        /** Sair da composição do dialog (fechar, voltar ou rotacionar) avisa o fim do chat para o CSAT. */
        DisposableEffect(viewModel) {
            viewModel.setChatVisible(true)
            onDispose {
                viewModel.setChatVisible(false)
                viewModel.intent.onClose()
            }
        }

        AiChatScreen(
            state = uiState,
            isProactiveEnabled = gateViewModel.isProactiveEnabled,
            intent = viewModel.intent.copy(
                onSettings = { navigator.navigate(AiSettings) },
                onBack = {
                    viewModel.dismissPendingApproval()
                    navigator.goBack()
                },
                onToggleHistory = { navigator.navigate(AiHistory) }
            ),
            isSendEnabled = isSendEnabled,
            onUpgradeClick = { navigator.navigate(Paywall(source = PAYWALL_SOURCE)) }
        )
    }
    composable<AiHistory> {
        val viewModel: AiHistoryViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(uiState.selectedSessionId) {
            val sessionId = uiState.selectedSessionId
            if (sessionId != null) {
                navigator.navigate(AiChat(sessionId = sessionId)) {
                    popUpTo(AiHistory) { inclusive = true }
                }
            }
        }

        AiHistoryScreen(
            state = uiState,
            intent = viewModel.intent.copy(
                onBack = { navigator.goBack() },
                onSelectSession = { sessionId ->
                    viewModel.intent.onSelectSession(sessionId)
                }
            )
        )
    }
}
