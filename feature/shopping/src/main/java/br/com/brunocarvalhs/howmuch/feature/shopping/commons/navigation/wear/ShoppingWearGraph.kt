package br.com.brunocarvalhs.howmuch.feature.shopping.commons.navigation.wear

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.wear.compose.navigation.composable
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.navigation.wear.ShoppingDetail
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.viewmodel.ShoppingListViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.wear.screen.ShoppingDetailWearScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.wear.screen.ShoppingWearScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.wear.viewmodel.ShoppingDetailViewModel

fun NavGraphBuilder.shoppingWearGraph(navigator: Navigator) {
    composable(ShoppingList::class.java.name) {
        val viewModel: ShoppingListViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        ShoppingWearScreen(
            viewModel = viewModel,
            onNavigateToDetail = { id -> navigator.navigate(ShoppingDetail(id)) }
        )
    }

    composable(ShoppingDetail::class.java.name) {
        val viewModel: ShoppingDetailViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        ShoppingDetailWearScreen(viewModel = viewModel)
    }
}
