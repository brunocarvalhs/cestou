package br.com.brunocarvalhs.howmuch.feature.shopping.navigation.wear

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
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen.WearSearchScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.ShoppingDetailViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearAiViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearHistoryViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearHomeViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearMenuViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearNotificationsViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.WearSearchViewModel
import androidx.navigation.compose.composable as typedComposable

/**
 * Relógio (sdd-056). A home abre direto na lista de listas (Q2): o salto automático para a última
 * lista saiu. Pesquisar e Histórico são rotas internas; Configurações é o perfil já existente.
 */
fun NavGraphBuilder.shoppingWearGraph(navigator: Navigator) {
    composable(ShoppingList::class.java.name) {
        val viewModel: WearHomeViewModel = hiltViewModel()
        ShoppingWearScreen(
            viewModel = viewModel,
            onNavigateToDetail = { id -> navigator.navigate(ShoppingDetail(id)) },
            onNavigateToSearch = { navigator.navigate(WearSearch::class.java.name) },
            onNavigateToMenu = { navigator.navigate(WearMenu::class.java.name) },
            onNavigateToSettings = { navigator.navigate(Profile) }
        )
    }

    // String routes, like the home: typed object routes crash the watch NavHost at start (sdd-056).
    composable(WearSearch::class.java.name) {
        val viewModel: WearSearchViewModel = hiltViewModel()
        WearSearchScreen(
            viewModel = viewModel,
            onNavigateToDetail = { id -> navigator.navigate(ShoppingDetail(id)) }
        )
    }

    composable(WearMenu::class.java.name) {
        val menuViewModel: WearMenuViewModel = hiltViewModel()
        val menuState by menuViewModel.uiState.collectAsStateWithLifecycle()
        WearMenuScreen(
            showAi = menuState.showAi,
            onNavigateToNotifications = { navigator.navigate(WearNotifications::class.java.name) },
            onNavigateToAi = { navigator.navigate(WearAi::class.java.name) },
            onNavigateToHistory = { navigator.navigate(WearHistory::class.java.name) },
            onNavigateToSettings = { navigator.navigate(Profile) }
        )
    }

    composable(WearNotifications::class.java.name) {
        val viewModel: WearNotificationsViewModel = hiltViewModel()
        WearNotificationsScreen(viewModel = viewModel)
    }

    composable(WearAi::class.java.name) {
        val viewModel: WearAiViewModel = hiltViewModel()
        WearAiScreen(viewModel = viewModel)
    }

    composable(WearHistory::class.java.name) {
        val viewModel: WearHistoryViewModel = hiltViewModel()
        WearHistoryScreen(viewModel = viewModel)
    }

    // Typed, not by name: ShoppingDetailViewModel reads shoppingId with toRoute<ShoppingDetail>().
    typedComposable<ShoppingDetail> {
        val viewModel: ShoppingDetailViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        ShoppingDetailScreen(viewModel = viewModel)
    }
}
