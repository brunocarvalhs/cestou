package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.navigation.ShoppingList
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Notifications
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.Profile
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.SpendingInsights
import br.com.brunocarvalhs.howmuch.core.ui.R

/**
 * Itens da barra inferior, no padrão do canvas "Subtotal — nova identidade": Listas, Gastos,
 * Avisos e Perfil. Os ícones são desenhados a partir dos mesmos caminhos do canvas.
 */
sealed class BottomNavItem(
    val route: Any,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Lists : BottomNavItem(
        route = ShoppingList,
        titleRes = R.string.nav_lists,
        selectedIcon = navIcon("NavListsFilled") {
            fillPath(BAG_BODY)
            strokePath(BAG_BODY)
            strokePath(BAG_HANDLE)
        },
        unselectedIcon = navIcon("NavListsOutlined") {
            strokePath(BAG_BODY)
            strokePath(BAG_HANDLE)
        }
    )

    object SpendingItem : BottomNavItem(
        route = SpendingInsights,
        titleRes = R.string.nav_spending,
        selectedIcon = navIcon("NavSpending") { strokePath(BARS, width = 2f) },
        unselectedIcon = navIcon("NavSpending") { strokePath(BARS, width = 2f) }
    )

    object NotificationsItem : BottomNavItem(
        route = Notifications,
        titleRes = R.string.nav_notifications,
        selectedIcon = navIcon("NavNotifications") {
            strokePath(BELL_BODY)
            strokePath(BELL_CLAPPER)
        },
        unselectedIcon = navIcon("NavNotifications") {
            strokePath(BELL_BODY)
            strokePath(BELL_CLAPPER)
        }
    )

    object ProfileItem : BottomNavItem(
        route = Profile,
        titleRes = R.string.nav_profile,
        selectedIcon = navIcon("NavProfile") {
            strokePath(PROFILE_HEAD)
            strokePath(PROFILE_BODY)
        },
        unselectedIcon = navIcon("NavProfile") {
            strokePath(PROFILE_HEAD)
            strokePath(PROFILE_BODY)
        }
    )
}

@Composable
fun CestouBottomNavigation(
    currentRoute: Any?,
    onNavigate: (Any) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    if (!visible) return
    val items = listOf(
        BottomNavItem.Lists,
        BottomNavItem.SpendingItem,
        BottomNavItem.NotificationsItem,
        BottomNavItem.ProfileItem
    )
    Column(modifier = modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val selected = currentRoute != null && currentRoute::class == item.route::class
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    modifier = Modifier.testTag("bottom_nav_${item.route::class.simpleName}"),
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = null
                        )
                    },
                    label = { Text(text = stringResource(item.titleRes)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent
                    ),
                    alwaysShowLabel = true
                )
            }
        }
    }
}

private fun navIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(block).build()

private fun ImageVector.Builder.fillPath(data: String) {
    addPath(pathData = PathParser().parsePathString(data).toNodes(), fill = SolidColor(Color.Black))
}

private fun ImageVector.Builder.strokePath(data: String, width: Float = 1.8f) {
    addPath(
        pathData = PathParser().parsePathString(data).toNodes(),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    )
}

private const val BAG_BODY = "M4 8H20L18.2 19H5.8Z"
private const val BAG_HANDLE = "M9 8V6.5A3 3 0 0 1 15 6.5V8"
private const val BARS = "M5 19V11M12 19V5M19 19V14"
private const val BELL_BODY = "M6 16V11A6 6 0 0 1 18 11V16L19.5 18H4.5Z"
private const val BELL_CLAPPER = "M10 21A2 2 0 0 0 14 21"
private const val PROFILE_HEAD = "M16 9A4 4 0 1 1 8 9A4 4 0 1 1 16 9Z"
private const val PROFILE_BODY = "M4.5 20C5.8 16.5 8.6 15 12 15S18.2 16.5 19.5 20"
