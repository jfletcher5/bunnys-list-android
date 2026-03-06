package com.bunnyslist.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BunnyPrimary,
    onPrimary = BunnyOnPrimary,
    primaryContainer = BunnyPrimaryContainer,
    onPrimaryContainer = BunnyOnPrimaryContainer,
    secondary = BunnySecondary,
    onSecondary = BunnyOnSecondary,
    secondaryContainer = BunnySecondaryContainer,
    onSecondaryContainer = BunnyOnSecondaryContainer,
    surface = BunnySurface,
    surfaceVariant = BunnySurfaceVariant,
    surfaceContainer = BunnySurfaceContainer,
    onSurface = BunnyOnSurface,
    onSurfaceVariant = BunnyOnSurfaceVariant,
    outline = BunnyOutline,
    error = BunnyError,
)

private val DarkColors = darkColorScheme(
    primary = BunnyDarkPrimary,
    onPrimary = BunnyDarkOnPrimary,
    primaryContainer = BunnyDarkPrimaryContainer,
    onPrimaryContainer = BunnyDarkOnPrimaryContainer,
    secondary = BunnyDarkSecondary,
    onSecondary = BunnyDarkOnSecondary,
    secondaryContainer = BunnyDarkSecondaryContainer,
    onSecondaryContainer = BunnyDarkOnSecondaryContainer,
    surface = BunnyDarkSurface,
    surfaceVariant = BunnyDarkSurfaceVariant,
    surfaceContainer = BunnyDarkSurfaceContainer,
    onSurface = BunnyDarkOnSurface,
    onSurfaceVariant = BunnyDarkOnSurfaceVariant,
    outline = BunnyDarkOutline,
)

/**
 * App-wide Material theme for the native Android implementation.
 */
@Composable
fun BunnysListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // The app theme centralizes brand look-and-feel for all screens and widgets.
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
