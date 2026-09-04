package br.com.brunocarvalhs.howmuch.feature.products.presentation.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.screen.AiChatScreen
import br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel.AiChatViewModel
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.common.Options
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.ProductHeader
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.ProductPhotoForm
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.ProductSearchForm
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.QuickAddForm
import br.com.brunocarvalhs.howmuch.feature.products.presentation.components.product.SuggestionsAndCommonForm
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.CommonProductViewModel
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.ProductPhotoViewModel
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.ProductSearchViewModel
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.ProductSuggestionViewModel
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.QuickAddViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductScreen(
    shopping: Shopping,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedOption = Options.entries.find { it.name == currentRoute } ?: Options.QUICK_ADD

    val viewModelStoreOwner = LocalViewModelStoreOwner.current!!
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            ProductHeader(
                shoppingTitle = shopping.title,
                selectedOption = selectedOption,
                onOptionSelected = { option ->
                    navController.navigate(option.name) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Options.QUICK_ADD.name,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(Options.QUICK_ADD.name) {
                val viewModel: QuickAddViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val commonViewModel: CommonProductViewModel =
                    hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
                val commonUiState by commonViewModel.uiState.collectAsStateWithLifecycle()
                QuickAddForm(
                    uiState = uiState,
                    intent = viewModel.intent,
                    commonUiState = commonUiState,
                    commonIntent = commonViewModel.intent,
                    snackbarHostState = snackbarHostState,
                    onNavigateToPhoto = {
                        navController.navigate(Options.PHOTO.name) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Options.SEARCH.name) {
                val viewModel: ProductSearchViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ProductSearchForm(
                    uiState = uiState,
                    intent = viewModel.intent,
                    snackbarHostState = snackbarHostState
                )
            }
            composable(Options.PHOTO.name) {
                val viewModel: ProductPhotoViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                ProductPhotoForm(
                    uiState = uiState,
                    intent = viewModel.intent,
                    snackbarHostState = snackbarHostState
                )
            }
            composable(Options.SUGGESTIONS.name) {
                val suggestionViewModel: ProductSuggestionViewModel =
                    hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
                val suggestionUiState by suggestionViewModel.uiState.collectAsStateWithLifecycle()
                SuggestionsAndCommonForm(
                    suggestionUiState = suggestionUiState,
                    suggestionIntent = suggestionViewModel.intent,
                    suggestionEvents = suggestionViewModel.events,
                    snackbarHostState = snackbarHostState,
                    onBack = onBack
                )
            }
            composable(Options.AI.name) {
                val viewModel: AiChatViewModel = hiltViewModel(viewModelStoreOwner = viewModelStoreOwner)
                viewModel.setShoppingContext(shopping.id)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                AiChatScreen(
                    state = uiState,
                    intent = viewModel.intent
                )
            }
        }
    }
}
