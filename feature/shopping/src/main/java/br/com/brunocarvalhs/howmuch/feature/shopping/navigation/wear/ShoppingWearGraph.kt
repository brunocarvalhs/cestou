package br.com.brunocarvalhs.howmuch.feature.shopping.navigation.wear

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.wear.compose.navigation.composable
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.navigation.wear.ShoppingDetail
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel.ShoppingListViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen.ShoppingDetailScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen.ShoppingWearScreen
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.LastListViewModel
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel.ShoppingDetailViewModel

fun NavGraphBuilder.shoppingWearGraph(navigator: Navigator) {
    composable(ShoppingList::class.java.name) {
        val viewModel: ShoppingListViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        val lastListViewModel: LastListViewModel = hiltViewModel()
        // Once per launch: jump into the last list; swiping back lands on all lists.
        var openedLastList by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!openedLastList) {
                openedLastList = true
                lastListViewModel.lastListId()?.let { navigator.navigate(ShoppingDetail(it)) }
            }
        }
        ShoppingWearScreen(
            viewModel = viewModel,
            onNavigateToDetail = { id -> navigator.navigate(ShoppingDetail(id)) }
        )
    }

    composable(ShoppingDetail::class.java.name) {
        val viewModel: ShoppingDetailViewModel = hiltViewModel()
        viewModel.setNavigator(navigator)
        ShoppingDetailScreen(viewModel = viewModel)
    }
}
