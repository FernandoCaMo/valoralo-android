package com.valoralo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Azul40,
    onPrimary = Neutro99,
    primaryContainer = Azul90,
    onPrimaryContainer = Azul10,
    secondary = Verde40,
    onSecondary = Neutro99,
    secondaryContainer = Verde90,
    onSecondaryContainer = Verde10,
    background = Neutro99,
    onBackground = Neutro10,
    surface = Neutro99,
    onSurface = Neutro10,
    surfaceVariant = NeutroVar90,
    onSurfaceVariant = NeutroVar30,
    outline = NeutroVar50,
    outlineVariant = NeutroVar80,
)

private val DarkColors = darkColorScheme(
    primary = Azul80,
    onPrimary = Azul20,
    primaryContainer = Azul30,
    onPrimaryContainer = Azul90,
    secondary = Verde80,
    onSecondary = Verde10,
    secondaryContainer = Verde40,
    onSecondaryContainer = Verde90,
    background = Neutro10,
    onBackground = Neutro90,
    surface = Neutro10,
    onSurface = Neutro90,
    surfaceVariant = NeutroVar30,
    onSurfaceVariant = NeutroVar80,
    outline = NeutroVar50,
    outlineVariant = NeutroVar30,
)

/**
 * Fixed brand colours (no dynamic colour) so the app looks the same on every device.
 */
@Composable
fun ValoraloTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
