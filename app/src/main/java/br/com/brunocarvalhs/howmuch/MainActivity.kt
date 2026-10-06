package br.com.brunocarvalhs.howmuch

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.core.common.util.InviteLink
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureInitializer
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.navigation.isProtectedRoute
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.JoinList
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Profile
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouBottomNavigation
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.CompleteName
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.Welcome
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.isAuthFlow
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var featureInitializers: Set<@JvmSuppressWildcards FeatureInitializer>

    @Inject
    lateinit var navigator: Navigator

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CestouApp(windowSizeClass = calculateWindowSizeClass(this))
        }
    }

    @Composable
    private fun CestouApp(windowSizeClass: WindowSizeClass) {
        val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
        val language by viewModel.language.collectAsStateWithLifecycle()
        val photoUrl by viewModel.photoUrl.collectAsStateWithLifecycle()

        LaunchedEffect(language) {
            val appLocales = LocaleListCompat.forLanguageTags(language)
            AppCompatDelegate.setApplicationLocales(appLocales)
        }

        val darkTheme = when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

        CestouTheme(darkTheme = darkTheme) {
            val navController = rememberNavController()

            LaunchedEffect(navController) {
                navigator.bind(navController)
            }

            val navBackStackEntry by navController.currentBackStackEntryAsState()

            var screenBackStackEntry by remember { mutableStateOf(navBackStackEntry) }
            LaunchedEffect(navBackStackEntry) {
                val destination = navBackStackEntry?.destination
                if (destination != null && destination !is DialogNavigator.Destination) {
                    screenBackStackEntry = navBackStackEntry
                }
            }
            val currentDestination = screenBackStackEntry?.destination
            val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

            val initialAuthenticated = remember { isAuthenticated }
            val requiresName by viewModel.requiresName.collectAsStateWithLifecycle()
            var wasAuthenticated by remember { mutableStateOf(isAuthenticated) }
            LaunchedEffect(isAuthenticated) {
                if (isAuthenticated) {
                    wasAuthenticated = true
                } else if (wasAuthenticated) {
                    wasAuthenticated = false
                    navigator.navigate(Welcome) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            // AiChat's shoppingId here is a throwaway placeholder: rootRoutes only matches by
            // route::class (see hasRoute/isProtectedRoute below), never by field value.
            val rootRoutes = remember { listOf(ShoppingList, AiChat(shoppingId = ""), Profile) }

            val currentRoute = rootRoutes.find { route ->
                currentDestination?.hierarchy?.any { it.hasRoute(route::class) } == true
            }

            val isOnProtectedRoute = currentDestination?.isProtectedRoute(rootRoutes) == true

            LaunchedEffect(isOnProtectedRoute, isAuthenticated) {
                if (!isAuthenticated && isOnProtectedRoute) {
                    navigator.navigate(Welcome) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            // Required-name gate (spec EPA-06): a signed-in account without a name never reaches the
            // app. Skipped inside the auth flow, where a just-created account has no name for a
            // moment; the decision re-reads the auth state so a name saved a moment ago is honored.
            LaunchedEffect(requiresName, currentDestination) {
                val destination = currentDestination ?: return@LaunchedEffect
                if (requiresName && !destination.isAuthFlow() && viewModel.requiresNameNow()) {
                    navigator.navigate(CompleteName) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            Scaffold(
                bottomBar = {
                    CestouBottomNavigation(
                        currentRoute = currentRoute,
                        photoUrl = photoUrl,
                        visible = currentRoute != null && currentRoute !is AiChat,
                        onNavigate = { route ->
                            navigator.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                },
            ) { padding ->
                Surface(modifier = Modifier.padding(padding)) {
                    LaunchedEffect(intent) {
                        if (intent?.action == Intent.ACTION_VIEW) {
                            val data = intent.data
                            if (data != null && InviteLink.matches(data)) {
                                navigator.navigate(JoinList(token = InviteLink.tokenFrom(data)))
                            }
                        }
                    }

                    val startDestination = remember {
                        when {
                            !initialAuthenticated -> Welcome
                            viewModel.requiresNameNow() -> CompleteName
                            else -> ShoppingList
                        }
                    }
                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        featureInitializers.forEach {
                            it.registerGraph(this, navigator, windowSizeClass)
                        }
                    }
                }
            }
        }
    }
}
