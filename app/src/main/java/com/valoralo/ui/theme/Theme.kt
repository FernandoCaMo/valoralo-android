package com.valoralo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * Full colour scheme (1.0.8), generated with Material's algorithm (HCT tonal palettes) from the brand
 * blue #1F5E8C, so no role falls back to Material's default lavender (bottom bar, carousel, sheets).
 *  - primary keeps the brand blue (tone ~38) in light and tone 80 in dark.
 *  - Neutral and neutral-variant palettes of the same hue for every surface and outline.
 *  - secondaryContainer = primaryContainer: one selection colour everywhere (chips, bottom bar indicator).
 *  - tertiary: warm amber, from the star colour family (Material's generated tertiary was lavender).
 * Role colours of reviews (Docente, Estudiante) live in BordeRol.kt.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF1F5E8C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCEE5FF),
    onPrimaryContainer = Color(0xFF001D32),
    inversePrimary = Color(0xFF96CCFF),
    secondary = Color(0xFF51606F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCEE5FF),
    onSecondaryContainer = Color(0xFF001D32),
    tertiary = Color(0xFF7D5000),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDDB8),
    onTertiaryContainer = Color(0xFF2A1800),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    background = Color(0xFFF8F9FE),
    onBackground = Color(0xFF191C1F),
    surface = Color(0xFFF8F9FE),
    onSurface = Color(0xFF191C1F),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF41474E),
    surfaceTint = Color(0xFF1F5E8C),
    inverseSurface = Color(0xFF2E3134),
    inverseOnSurface = Color(0xFFEFF0F5),
    outline = Color(0xFF72787F),
    outlineVariant = Color(0xFFC1C7CF),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF8F9FE),
    surfaceDim = Color(0xFFD9DADE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3F8),
    surfaceContainer = Color(0xFFEDEEF2),
    surfaceContainerHigh = Color(0xFFE7E8EC),
    surfaceContainerHighest = Color(0xFFE1E2E7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF96CCFF),
    onPrimary = Color(0xFF003353),
    primaryContainer = Color(0xFF004A76),
    onPrimaryContainer = Color(0xFFCEE5FF),
    inversePrimary = Color(0xFF1F5E8C),
    secondary = Color(0xFFB9C8DA),
    onSecondary = Color(0xFF233240),
    secondaryContainer = Color(0xFF004A76),
    onSecondaryContainer = Color(0xFFCEE5FF),
    tertiary = Color(0xFFF9BB68),
    onTertiary = Color(0xFF462B00),
    tertiaryContainer = Color(0xFF633F00),
    onTertiaryContainer = Color(0xFFFFDDB8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111417),
    onBackground = Color(0xFFE1E2E7),
    surface = Color(0xFF111417),
    onSurface = Color(0xFFE1E2E7),
    surfaceVariant = Color(0xFF41474E),
    onSurfaceVariant = Color(0xFFC1C7CF),
    surfaceTint = Color(0xFF96CCFF),
    inverseSurface = Color(0xFFE1E2E7),
    inverseOnSurface = Color(0xFF2E3134),
    outline = Color(0xFF8B9199),
    outlineVariant = Color(0xFF41474E),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF37393D),
    surfaceDim = Color(0xFF111417),
    surfaceContainerLowest = Color(0xFF0C0E12),
    surfaceContainerLow = Color(0xFF191C1F),
    surfaceContainer = Color(0xFF1D2023),
    surfaceContainerHigh = Color(0xFF272A2E),
    surfaceContainerHighest = Color(0xFF323538),
)

/** Fixed brand colours (no dynamic colour) so the app looks the same on every device. */
@Composable
fun ValoraloTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
