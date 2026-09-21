package br.com.brunocarvalhs.howmuch.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import br.com.brunocarvalhs.howmuch.core.theme.BackgroundDark
import br.com.brunocarvalhs.howmuch.core.theme.BorderDark
import br.com.brunocarvalhs.howmuch.core.theme.CestouBrightGreen
import br.com.brunocarvalhs.howmuch.core.theme.CestouDeepGreen
import br.com.brunocarvalhs.howmuch.core.theme.CestouOrange
import br.com.brunocarvalhs.howmuch.core.theme.CestouSoftGreen
import br.com.brunocarvalhs.howmuch.core.theme.CestouSoftOrange
import br.com.brunocarvalhs.howmuch.core.theme.CestouTextTertiary
import br.com.brunocarvalhs.howmuch.core.theme.Error as CestouError
import br.com.brunocarvalhs.howmuch.core.theme.SurfaceDark

private val WearDarkColorScheme = ColorScheme(
    primary = CestouBrightGreen,
    onPrimary = Color.White,
    primaryContainer = CestouDeepGreen,
    onPrimaryContainer = CestouSoftGreen,
    secondary = CestouOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF7C2D12),
    onSecondaryContainer = CestouSoftOrange,
    tertiary = CestouDeepGreen,
    onTertiary = Color.White,
    error = CestouError,
    onError = Color.White,
    background = BackgroundDark,
    onBackground = Color.White,
    surfaceContainer = SurfaceDark,
    onSurface = Color.White,
    onSurfaceVariant = CestouTextTertiary,
    outline = BorderDark
)

@Composable
fun CestouTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WearDarkColorScheme,
        typography = Typography(),
        content = content
    )
}
