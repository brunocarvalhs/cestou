package br.com.brunocarvalhs.howmuch.feature.profile

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavGraphBuilder
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.profile.navigation.profileGraph
import javax.inject.Inject

internal class ProfileInitializerImpl @Inject constructor() : ProfileInitializer {
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        navigator: Navigator,
        windowSizeClass: WindowSizeClass
    ) {
        navGraphBuilder.profileGraph(navigator)
    }
}
