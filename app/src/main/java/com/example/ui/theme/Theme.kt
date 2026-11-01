package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SacredGoldPrimaryDark,
    onPrimary = SacredGoldOnPrimaryDark,
    primaryContainer = SacredGoldContainerDark,
    onPrimaryContainer = SacredGoldOnContainerDark,
    secondary = SacredGoldBadge,
    onSecondary = SacredGoldOnPrimaryDark,
    background = SacredBgDark,
    onBackground = SacredOnSurfaceDark,
    surface = SacredSurfaceDark,
    onSurface = SacredOnSurfaceDark,
    surfaceVariant = SacredSurfaceVariantDark,
    onSurfaceVariant = SacredOnSurfaceVariantDark,
    outline = SacredOutlineDark,
)

private val LightColorScheme = lightColorScheme(
    primary = SacredGoldPrimaryLight,
    onPrimary = SacredGoldOnPrimaryLight,
    primaryContainer = SacredGoldContainerLight,
    onPrimaryContainer = SacredGoldOnContainerLight,
    secondary = SacredGoldBadge,
    onSecondary = SacredGoldOnPrimaryLight,
    background = SacredBgLight,
    onBackground = SacredOnSurfaceLight,
    surface = SacredSurfaceLight,
    onSurface = SacredOnSurfaceLight,
    surfaceVariant = SacredSurfaceVariantLight,
    onSurfaceVariant = SacredOnSurfaceVariantLight,
    outline = SacredOutlineLight,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
