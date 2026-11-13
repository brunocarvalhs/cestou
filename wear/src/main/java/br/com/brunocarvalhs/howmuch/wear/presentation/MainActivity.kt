package br.com.brunocarvalhs.howmuch.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.LinkPhone
import br.com.brunocarvalhs.howmuch.wear.presentation.session.WearSessionCleaner
import br.com.brunocarvalhs.howmuch.wear.presentation.theme.CestouTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var featureNavGraphs: Set<@JvmSuppressWildcards FeatureNavGraph>

    @Inject
    lateinit var sessionCleaner: WearSessionCleaner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startDestination = if (authService.currentUser != null) {
            ShoppingList::class.java.name
        } else {
            LinkPhone::class.java.name
        }

        val signedIn = authService.authState.map { it != null }

        setContent {
            WearApp(
                navigator = navigator,
                featureNavGraphs = featureNavGraphs,
                startDestination = startDestination,
                signedIn = signedIn,
                onSignedOut = { lifecycleScope.launch { sessionCleaner.clear() } }
            )
        }
    }
}

@Composable
fun WearApp(
    navigator: Navigator,
    featureNavGraphs: Set<FeatureNavGraph>,
    startDestination: String,
    signedIn: Flow<Boolean>,
    onSignedOut: () -> Unit
) {
    CestouTheme {
        AppScaffold {
            val navController = rememberSwipeDismissableNavController()
            navigator.bind(navController)

            // Sair (no relógio ou no celular) leva à pareamento, sem voltar para telas de conteúdo.
            val currentOnSignedOut by rememberUpdatedState(onSignedOut)
            LaunchedEffect(signedIn) {
                var wasSignedIn = false
                signedIn.distinctUntilChanged().collect { isSignedIn ->
                    if (wasSignedIn && !isSignedIn) {
                        currentOnSignedOut()
                        navigator.navigate(LinkPhone::class.java.name) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                    wasSignedIn = isSignedIn
                }
            }

            DisposableEffect(navController) {
                onDispose { navigator.unbind(navController) }
            }

            SwipeDismissableNavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                featureNavGraphs.forEach { navGraph ->
                    navGraph.registerWearGraph(this, navigator)
                }
            }
        }
    }
}
