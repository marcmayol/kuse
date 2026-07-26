package com.marcm.cadencia.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.marcm.cadencia.settings.ThemeMode

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = AccentInk,
    primaryContainer = AccentSoft,
    onPrimaryContainer = Accent,
    secondary = Accent,
    onSecondary = AccentInk,
    background = DarkScreen,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceLow,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkBorder,
    error = Overdue,
    onError = AccentInk,
    errorContainer = OverdueContainer,
    onErrorContainer = Overdue
)

private val LightColors = lightColorScheme(
    primary = AccentDark,
    onPrimary = Color.White,
    primaryContainer = AccentSoft,
    onPrimaryContainer = AccentDark,
    background = LightScreen,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceLow,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightBorder,
    outlineVariant = LightBorder,
    error = Overdue,
    onError = AccentInk,
    errorContainer = OverdueContainer,
    onErrorContainer = AccentDark
)

@Composable
fun KuseTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val colors = if (dark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = KuseTypography,
        content = content
    )
}
