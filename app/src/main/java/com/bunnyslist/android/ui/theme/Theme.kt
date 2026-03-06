package com.bunnyslist.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = BunnyGreen,
    secondary = BunnyGreenDark,
    surface = BunnySurface,
    outline = BunnyOutline,
)

private val DarkColors = darkColorScheme(
    primary = BunnyGreen,
    secondary = BunnyGreenDark,
)

/**
 * App-wide Material theme for the native Android implementation.
 */
@Composable
fun BunnysListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
