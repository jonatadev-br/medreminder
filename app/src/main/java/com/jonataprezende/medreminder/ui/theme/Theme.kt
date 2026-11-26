package com.jonataprezende.medreminder.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF14D4B4),
    primaryContainer = Color(0xFF003D35),
    onPrimary = Color(0xFF001F1B),
    onPrimaryContainer = Color(0xFFA7F5E8),
    secondary = Color(0xFF14D4B4),
    secondaryContainer = Color(0xFF003D35),
    onSecondary = Color(0xFF001F1B),
    onSecondaryContainer = Color(0xFFA7F5E8),
    tertiary = Color(0xFF60A5FA),
    tertiaryContainer = Color(0xFF001D3D),
    onTertiary = Color(0xFF001D3D),
    onTertiaryContainer = Color(0xFFE8F0FE),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF2E0F0F),
    onError = Color.Black,
    onErrorContainer = Color(0xFFFDECEA),
    background = Color(0xFF121214),
    onBackground = Color(0xFFF2F2F4),
    surface = Color(0xFF1E1E20),
    onSurface = Color(0xFFF2F2F4),
    surfaceVariant = Color(0xFF2A2A2E),
    onSurfaceVariant = Color(0xFFB4B4BB),
    outline = Color(0xFF2A2A2E),
    outlineVariant = Color(0xFF3D3D42),
    inverseSurface = Color(0xFFF2F2F4),
    inverseOnSurface = Color(0xFF1A1A1A),
    inversePrimary = Color(0xFF0E9F8A)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0E9F8A),
    primaryContainer = Color(0xFFE0F7F3),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF003D35),
    secondary = Color(0xFF0E9F8A),
    secondaryContainer = Color(0xFFE0F7F3),
    onSecondary = Color.White,
    onSecondaryContainer = Color(0xFF003D35),
    tertiary = Color(0xFF2563EB),
    tertiaryContainer = Color(0xFFE8F0FE),
    onTertiary = Color.White,
    onTertiaryContainer = Color(0xFF001D3D),
    error = Color(0xFFE0453A),
    errorContainer = Color(0xFFFDECEA),
    onError = Color.White,
    onErrorContainer = Color(0xFF690005),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF0F1F3),
    onSurfaceVariant = Color(0xFF4A4A4F),
    outline = Color(0xFFEFEFF0),
    outlineVariant = Color(0xFFE0E0E3),
    inverseSurface = Color(0xFF1E1E20),
    inverseOnSurface = Color(0xFFF2F2F4),
    inversePrimary = Color(0xFF14D4B4)
)

@Composable
fun Theme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = (view.context as? Activity) ?: return@SideEffect
            val windowInsetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
            activity.window.statusBarColor = colorScheme.background.toArgb()
            activity.window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MedReminderTypography,
        shapes = MedReminderShapes,
        content = content
    )
}