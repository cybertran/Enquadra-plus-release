package com.cybertran.enquadraplus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AdministrativeNavy = Color(0xFF1F3A5F)
private val AdministrativeNavyDark = Color(0xFF17324D)
private val AdministrativeBlueGray = Color(0xFF4E6378)
private val AdministrativeSlate = Color(0xFF5B6B7A)

private val AppBackground = Color(0xFFF4F6F8)
private val AppSurface = Color(0xFFFFFFFF)
private val AppSurfaceVariant = Color(0xFFE9EEF3)
private val AppPrimaryContainer = Color(0xFFDCE8F2)
private val AppSecondaryContainer = Color(0xFFE2E8EF)
private val AppTertiaryContainer = Color(0xFFE6EBF0)

private val Ink = Color(0xFF1C2430)
private val MutedInk = Color(0xFF5E6B78)
private val Outline = Color(0xFFA9B4C0)
private val OutlineVariant = Color(0xFFD4DCE4)

private val LightColors = lightColorScheme(
    primary = AdministrativeNavy,
    onPrimary = Color.White,
    primaryContainer = AppPrimaryContainer,
    onPrimaryContainer = AdministrativeNavyDark,

    secondary = AdministrativeBlueGray,
    onSecondary = Color.White,
    secondaryContainer = AppSecondaryContainer,
    onSecondaryContainer = Color(0xFF253849),

    tertiary = AdministrativeSlate,
    onTertiary = Color.White,
    tertiaryContainer = AppTertiaryContainer,
    onTertiaryContainer = Color(0xFF293744),

    background = AppBackground,
    onBackground = Ink,

    surface = AppSurface,
    onSurface = Ink,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = MutedInk,
    surfaceTint = AdministrativeNavy,

    surfaceBright = Color(0xFFFAFBFC),
    surfaceDim = Color(0xFFE1E6EB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F9FB),
    surfaceContainer = Color(0xFFF1F4F7),
    surfaceContainerHigh = Color(0xFFEBEFF3),
    surfaceContainerHighest = Color(0xFFE5EAF0),

    inverseSurface = Color(0xFF2B313A),
    inverseOnSurface = Color(0xFFF1F4F7),
    inversePrimary = Color(0xFFB9D0E4),

    outline = Outline,
    outlineVariant = OutlineVariant,
    scrim = Color.Black,

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9D0E4),
    onPrimary = Color(0xFF0D2940),
    primaryContainer = Color(0xFF314C68),
    onPrimaryContainer = Color(0xFFE6F0F8),

    secondary = Color(0xFFBAC7D4),
    onSecondary = Color(0xFF243748),
    secondaryContainer = Color(0xFF34495C),
    onSecondaryContainer = Color(0xFFE1E9F0),

    tertiary = Color(0xFFC0CAD3),
    onTertiary = Color(0xFF293744),
    tertiaryContainer = Color(0xFF3A4855),
    onTertiaryContainer = Color(0xFFE5EAF0),

    background = Color(0xFF121820),
    onBackground = Color(0xFFE7EBF0),

    surface = Color(0xFF181F28),
    onSurface = Color(0xFFE7EBF0),
    surfaceVariant = Color(0xFF29323D),
    onSurfaceVariant = Color(0xFFC4CDD6),
    surfaceTint = Color(0xFFB9D0E4),

    surfaceBright = Color(0xFF373E47),
    surfaceDim = Color(0xFF10161D),
    surfaceContainerLowest = Color(0xFF0D131A),
    surfaceContainerLow = Color(0xFF171D25),
    surfaceContainer = Color(0xFF1C232C),
    surfaceContainerHigh = Color(0xFF262D36),
    surfaceContainerHighest = Color(0xFF313842),

    inverseSurface = Color(0xFFE7EBF0),
    inverseOnSurface = Color(0xFF2B313A),
    inversePrimary = AdministrativeNavy,

    outline = Color(0xFF8E99A6),
    outlineVariant = Color(0xFF444E59),
    scrim = Color.Black,

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun EnquadraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
