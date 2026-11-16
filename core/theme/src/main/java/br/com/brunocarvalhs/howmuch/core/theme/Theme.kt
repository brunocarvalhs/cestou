package br.com.brunocarvalhs.howmuch.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SubtotalDarkBlue,
    onPrimary = SubtotalNight,
    primaryContainer = SubtotalNavy,
    onPrimaryContainer = SubtotalMist,

    secondary = SubtotalCoral,
    onSecondary = SubtotalNight,
    secondaryContainer = SubtotalCoralDeep,
    onSecondaryContainer = SubtotalCoralSoft,

    tertiary = SubtotalNavy,
    onTertiary = Color.White,

    error = SubtotalCoral,
    onError = SubtotalNight,

    background = SubtotalDarkBackground,
    onBackground = Color.White,
    surface = SubtotalDarkSurface,
    onSurface = Color.White,
    surfaceVariant = SubtotalDarkSurfaceVariant,
    onSurfaceVariant = SubtotalDarkMuted,
    outline = SubtotalDarkLine
)

private val LightColorScheme = lightColorScheme(
    primary = SubtotalBlue,
    onPrimary = Color.White,
    primaryContainer = SubtotalMist,
    onPrimaryContainer = SubtotalNavy,

    secondary = SubtotalCoral,
    onSecondary = SubtotalNight,
    secondaryContainer = SubtotalCoralSoft,
    onSecondaryContainer = SubtotalCoralDeep,

    tertiary = SubtotalMist,
    onTertiary = SubtotalNavy,

    error = Error,
    onError = Color.White,

    background = BackgroundLight,
    onBackground = CestouTextPrimary,
    surface = SurfaceLight,
    onSurface = CestouTextPrimary,
    surfaceVariant = SubtotalMist,
    onSurfaceVariant = SubtotalSlate,
    outline = BorderLight
)

/**
 * Radii from the nova identidade: 14 dp for controls, 20 dp for cards.
 */
private val SubtotalShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CestouTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to keep brand identity consistency
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SubtotalShapes,
        content = content
    )
}
