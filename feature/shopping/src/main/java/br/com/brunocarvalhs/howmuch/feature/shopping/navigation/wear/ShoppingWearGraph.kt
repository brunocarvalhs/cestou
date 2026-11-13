package br.com.brunocarvalhs.howmuch.feature.shopping.navigation.wear

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.wear.compose.navigation.composable
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Profile
import br.com.brunocarvalhs.howmuch.core.navigation.wear.ShoppingDetail
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen.ShoppingDetailScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen.ShoppingWearScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen.WearAiScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen.WearHistoryScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen.WearMenuScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen.WearNotificationsScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen.WearProGate
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen.WearSearchScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.ShoppingDetailViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearAiViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearHistoryViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearHomeViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearMenuViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearNotificationsViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearProViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearSearchViewModel
import androidx.navigation.compose.composable as typedComposable

/**
 * Relógio. A home abre direto na lista de listas. Toda tela de conteúdo passa por [WearProGate]:
 * sem assinatura PRO, mostra a tela de assinatura. Configurações é o perfil já existente, e fica
 * fora do gate para o usuário continuar podendo sair e revisar a conta.
 */
fun NavGraphBuilder.shoppingWearGraph(navigator: Navigator) {
    gatedComposable(ShoppingList::class.java.name) {
        val viewModel: WearHomeViewModel = hiltViewModel()
        ShoppingWearScreen(
            viewModel = viewModel,
            onNavigateToDetail = { id -> navigator.navigate(ShoppingDetail(id)) },
            onNavigateToSearch = { navigator.navigate(WearSearch::class.java.name) },
            onNavigateToMenu = { navigator.navigate(WearMenu::class.java.name) },
            onNavigateToSettings = { navigator.navigate(Profile) }
        )
    }

    // String routes, like the home: typed object routes crash the watch NavHost at start.
    gatedComposable(WearSearch::class.java.name) {
        val viewModel: WearSearchViewModel = hiltViewModel()
        WearSearchScreen(
            viewModel = viewModel,
            onNavigateToDetail = { id -> navigator.navigate(ShoppingDetail(id)) }
        )
    }

    gatedComposable(WearMenu::class.java.name) {
        val menuViewModel: WearMenuViewModel = hiltViewModel()
        WearMenuRoute(menuViewModel = menuViewModel, navigator = navigator)
    }

    gatedComposable(WearNotifications::class.java.name) {
        val viewModel: WearNotificationsViewModel = hiltViewModel()
        WearNotificationsScreen(viewModel = viewModel)
    }

    gatedComposable(WearAi::class.java.name) {
        val viewModel: WearAiViewModel = hiltViewModel()
        WearAiScreen(viewModel = viewModel)
    }

    gatedComposable(WearHistory::class.java.name) {
        val viewModel: WearHistoryViewModel = hiltViewModel()
        WearHistoryScreen(viewModel = viewModel)
    }

    // Typed, not by name: ShoppingDetailViewModel reads shoppingId with toRoute<ShoppingDetail>().
    typedComposable<ShoppingDetail> {
        WearProGate(viewModel = hiltViewModel<WearProViewModel>()) {
            val viewModel: ShoppingDetailViewModel = hiltViewModel()
            viewModel.setNavigator(navigator)
            ShoppingDetailScreen(viewModel = viewModel)
        }
    }
}

/** A route whose content is shown only with the PRO subscription. */
private fun NavGraphBuilder.gatedComposable(route: String, content: @Composable () -> Unit) {
    composable(route) {
        WearProGate(viewModel = hiltViewModel<WearProViewModel>()) {
            content()
        }
    }
}

@Composable
private fun WearMenuRoute(menuViewModel: WearMenuViewModel, navigator: Navigator) {
    val menuState by menuViewModel.uiState.collectAsStateWithLifecycle()
    WearMenuScreen(
        showAi = menuState.showAi,
        onNavigateToNotifications = { navigator.navigate(WearNotifications::class.java.name) },
        onNavigateToAi = { navigator.navigate(WearAi::class.java.name) },
        onNavigateToHistory = { navigator.navigate(WearHistory::class.java.name) },
        onNavigateToSettings = { navigator.navigate(Profile) }
    )
}
