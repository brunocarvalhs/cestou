package br.com.brunocarvalhs.howmuch.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureInitializer
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.wear.presentation.theme.CestouTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigator: Navigator

    @Inject
    lateinit var featureInitializers: Set<@JvmSuppressWildcards FeatureInitializer>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp(
                navigator = navigator,
                featureInitializers = featureInitializers
            )
        }
    }
}

@Composable
fun WearApp(
    navigator: Navigator,
    featureInitializers: Set<FeatureInitializer>
) {
    CestouTheme {
        AppScaffold {
            val navController = rememberSwipeDismissableNavController()
            
            LaunchedEffect(navController) {
                navigator.bind(navController)
            }

            SwipeDismissableNavHost(
                navController = navController,
                startDestination = ShoppingList::class.java.name
            ) {
                featureInitializers.forEach { initializer ->
                    initializer.registerWearGraph(this, navigator)
                }
            }
        }
    }
}
