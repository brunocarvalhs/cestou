package br.com.brunocarvalhs.howmuch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.core.domain.entity.ThemeMode
import br.com.brunocarvalhs.howmuch.core.navigation.rememberNavigator
import br.com.brunocarvalhs.howmuch.core.ui.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.products.navigation.productsGraph
import br.com.brunocarvalhs.howmuch.feature.settings.navigation.settingsGraph
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.JoinList
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.shoppingGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()

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
                Surface {
                    val navController = rememberNavController()
                    val navigator = rememberNavigator(navController)

                    LaunchedEffect(intent) {
                        if (intent?.action == Intent.ACTION_VIEW) {
                            val data = intent.data
                            if (data?.host == "cestou.app" && data.path?.startsWith("/join") == true) {
                                // Redireciona para a tela de entrar na lista (pode passar o token via query ou path)
                                // Por simplicidade, vamos apenas abrir o dialog de Join
                                navigator.navigate(JoinList)
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ShoppingList
                    ) {
                        shoppingGraph(navigator, windowSizeClass)
                        productsGraph(navigator, windowSizeClass)
                        settingsGraph(navigator)
                    }
                }
            }
        }
    }
}
