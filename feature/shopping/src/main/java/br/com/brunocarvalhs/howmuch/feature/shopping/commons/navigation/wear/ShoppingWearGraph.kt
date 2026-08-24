package br.com.brunocarvalhs.howmuch.feature.shopping.commons.navigation.wear

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.wear.compose.navigation3.composable
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.viewmodel.ShoppingListViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.wear.screen.ShoppingWearScreen

fun NavGraphBuilder.shoppingWearGraph(navigator: Navigator) {
    composable<ShoppingList> {
        val viewModel: ShoppingListViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        ShoppingWearScreen(viewModel = viewModel)
    }
}
