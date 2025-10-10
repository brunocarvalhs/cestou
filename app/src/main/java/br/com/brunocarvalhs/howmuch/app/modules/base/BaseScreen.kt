package br.com.brunocarvalhs.howmuch.app.modules.base

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.brunocarvalhs.howmuch.app.foundation.annotations.DevicesPreview
import br.com.brunocarvalhs.howmuch.app.foundation.navigation.NavBarItem

@Composable
fun BaseScreen(
    tabs: LinkedHashMap<NavBarItem, @Composable (PaddingValues) -> Unit>
) {
    var selectedTab by remember { mutableStateOf(tabs.entries.first().key) }
    val tabsNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.keys.forEach { item ->
                    NavigationBarItem(
                        selected = item == selectedTab,
                        onClick = {
                            selectedTab = item
                            tabsNavController.navigate(item.route) {
                                popUpTo(tabsNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(item.label),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        icon = {
                            Icon(
                                painter = painterResource(item.icon),
                                contentDescription = stringResource(item.label),
                                tint = if (item == selectedTab) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = tabsNavController,
            startDestination = tabs.entries.first().key.route,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            tabs.forEach { (item, content) ->
                composable(item.route) {
                    content(padding)
                }
            }
        }
    }
}

@Composable
@DevicesPreview
private fun BaseScreenPreview() {
    BaseScreen(
        tabs = linkedMapOf(
            NavBarItem.HOME to { },
            NavBarItem.HISTORY to { },
            NavBarItem.MENU to { }
        )
    )
}
