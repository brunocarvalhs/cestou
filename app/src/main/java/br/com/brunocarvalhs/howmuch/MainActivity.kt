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
import androidx.compose.runtime.DisposableEffect
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
import br.com.brunocarvalhs.howmuch.analytics.ScreenTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.ThemeMode
import br.com.brunocarvalhs.howmuch.core.navigation.FeatureNavGraph
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.navigation.isProtectedRoute
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.AiChat
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.History
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.JoinList
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Profile
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.SpendingInsights
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouBottomNavigation
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.CompleteName
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.Welcome
import br.com.brunocarvalhs.howmuch.feature.auth.navigation.isAuthFlow
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var featureNavGraphs: Set<@JvmSuppressWildcards FeatureNavGraph>

    @Inject
    lateinit var navigator: Navigator

    @Inject
    internal lateinit var screenTracker: ScreenTracker

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeDeepLinkIntent()
        setContent {
            CestouApp(windowSizeClass = calculateWindowSizeClass(this))
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onForeground()
    }

    /** Rotação e troca de tema recriam a Activity sem o app ir para segundo plano. */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) viewModel.onBackground()
    }

    /**
     * With `launchMode="singleTask"` (AndroidManifest.xml), an invite link opened while the app
     * is already running reuses this Activity instead of creating a second instance - which used
     * to leave the resumed, original instance's [Navigator] unbound once the second instance's
     * composition disposed (crash fixed by this change, see Navigator.navigate/goBack). This is
     * where that reused instance receives the new intent.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeDeepLinkIntent()
    }

    /**
     * Reads a pending invite token out of [getIntent] into the ViewModel, then replaces this
     * Activity's intent with a blank one. Without this, [getIntent] keeps returning the SAME
     * invite intent across every future recreation (rotation, dark mode, locale change - the
     * Activity isn't destroyed by the OS for those, just recomposed), so `onCreate` would read it
     * again on every recreation and resurrect an already-consumed token (B-14).
     */
    private fun consumeDeepLinkIntent() {
        viewModel.onDeepLink(intent)
        setIntent(Intent())
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

            // Bind síncrono, direto no corpo da composição: precisa estar concluído ANTES do
            // NavHost (mais abaixo, na mesma composição) compor seus destinos - inclusive os
            // restaurados do back stack no primeiro frame. Um LaunchedEffect aqui rodaria depois
            // desse primeiro frame, deixando `navigator.navController` desvinculado exatamente
            // quando um destino restaurado (ex.: CartGraph) tenta lê-lo, causando o crash fatal
            // "Navigator not bound to a NavController" visto no Crashlytics. bind() é apenas uma
            // atribuição de var, então chamá-lo a cada (re)composição é seguro e barato.
            navigator.bind(navController)

            // Navigator é singleton do Hilt (sobrevive à recriação da Activity). Ao destruir esta
            // composição (rotação, dark mode, troca de idioma, "não manter atividades" etc.) é
            // preciso desvincular o NavController desta Activity - senão o singleton prende a
            // referência antiga (vazamento) e, se uma nova Activity já tiver feito bind antes
            // deste onDispose rodar, unbind() (condicional por identidade) não apaga o bind novo.
            DisposableEffect(navController) {
                onDispose { navigator.unbind(navController) }
            }

            val navBackStackEntry by navController.currentBackStackEntryAsState()

            /**
             * Usa a entrada atual do back stack (dialogs inclusos), não a `screenBackStackEntry`
             * abaixo, que ignora dialogs só para o layout da barra inferior.
             */
            LaunchedEffect(navBackStackEntry) {
                navBackStackEntry?.let { screenTracker.onDestinationChanged(it.id, it.destination) }
            }

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
            val rootRoutes = remember { listOf(ShoppingList, History, AiChat(shoppingId = ""), Profile) }

            val currentRoute = rootRoutes.find { route ->
                currentDestination?.hierarchy?.any { it.hasRoute(route::class) } == true
            }

            val isOnProtectedRoute = currentDestination?.isProtectedRoute(rootRoutes + SpendingInsights) == true

            LaunchedEffect(isOnProtectedRoute, isAuthenticated) {
                if (!isAuthenticated && isOnProtectedRoute) {
                    navigator.navigate(Welcome) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            }

            /**
             * An invite token read by onDeepLink/onNewIntent is held in the ViewModel (not
             * navigated to immediately) because JoinList isn't a protected route (MobileRoutes.kt)
             * and would otherwise open over Welcome and fail to join while signed out (B-30). Also
             * waits for `!requiresName` so it never races the required-name gate below: both would
             * otherwise navigate on the same isAuthenticated flip, and whichever runs last wins -
             * dropping the invite silently since the token is already consumed by then.
             */
            val pendingJoinToken by viewModel.pendingJoinToken.collectAsStateWithLifecycle()
            LaunchedEffect(pendingJoinToken, isAuthenticated, requiresName) {
                val token = pendingJoinToken
                if (token != null && isAuthenticated && !requiresName) {
                    navigator.navigate(JoinList(token = token))
                    viewModel.consumePendingJoinToken()
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
                            screenTracker.onTabSelected(route)
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
                        featureNavGraphs.forEach {
                            it.registerGraph(this, navigator, windowSizeClass)
                        }
                    }
                }
            }
        }
    }
}
