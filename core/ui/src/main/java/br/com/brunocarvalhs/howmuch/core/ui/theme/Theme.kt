package br.com.brunocarvalhs.howmuch.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
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
    
    error = Error,
    onError = Color.White,
    
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1F2937),
    onSurfaceVariant = CestouTextTertiary,
    outline = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = CestouBrightGreen,
    onPrimary = Color.White,
    primaryContainer = CestouSoftGreen,
    onPrimaryContainer = CestouDeepGreen,
    
    secondary = CestouOrange,
    onSecondary = Color.White,
    secondaryContainer = CestouSoftOrange,
    onSecondaryContainer = Color(0xFF7C2D12),
    
    tertiary = CestouSoftGreen,
    onTertiary = CestouDeepGreen,
    
    error = Error,
    onError = Color.White,
    
    background = BackgroundLight,
    onBackground = CestouTextPrimary,
    surface = SurfaceLight,
    onSurface = CestouTextPrimary,
    surfaceVariant = CestouSoftGreen,
    onSurfaceVariant = CestouDeepGreen,
    outline = BorderLight
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
        content = content
    )
}
