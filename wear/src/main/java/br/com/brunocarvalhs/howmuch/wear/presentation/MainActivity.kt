package br.com.brunocarvalhs.howmuch.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureInitializer
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.PairingCode
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.LinkPhone
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.wear.presentation.theme.CestouTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var featureInitializers: Set<@JvmSuppressWildcards FeatureInitializer>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startDestination = if (authService.currentUser != null) {
            ShoppingList::class.java.name
        } else {
            LinkPhone::class.java.name
        }

        setContent {
            WearApp(
                navigator = navigator,
                featureInitializers = featureInitializers,
                startDestination = startDestination
            )
        }
    }
}

@Composable
fun WearApp(
    navigator: Navigator,
    featureInitializers: Set<FeatureInitializer>,
    startDestination: String
) {
    CestouTheme {
        AppScaffold {
            val navController = rememberSwipeDismissableNavController()
            
            LaunchedEffect(navController) {
                navigator.bind(navController)
            }

            SwipeDismissableNavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                featureInitializers.forEach { initializer ->
                    initializer.registerWearGraph(this, navigator)
                }
            }
        }
    }
}
