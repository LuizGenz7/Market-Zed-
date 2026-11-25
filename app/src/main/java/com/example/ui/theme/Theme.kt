package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

val LocalDarkTheme = staticCompositionLocalOf { false }

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

private val MarketZedLightColors = lightColorScheme(
    primary = MarketZedOrange,
    onPrimary = Color.White,
    primaryContainer = MarketZedOrangeContainerLight,
    onPrimaryContainer = MarketZedOrangeDark,
    secondary = Color(0xFF1F2937),
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = LightTextPrimary,
    tertiary = WantedBlue,
    onTertiary = Color.White,
    tertiaryContainer = WantedBlueContainerLight,
    onTertiaryContainer = Color(0xFF1E40AF),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder,
    outlineVariant = Color(0xFFF3F4F6),
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedContainerLight,
    onErrorContainer = Color(0xFF991B1B)
)

private val MarketZedDarkColors = darkColorScheme(
    primary = MarketZedOrangeLight,
    onPrimary = Color(0xFF1A0900),
    primaryContainer = MarketZedOrangeContainerDark,
    onPrimaryContainer = MarketZedOrangeContainerLight,
    secondary = Color(0xFFE5E7EB),
    onSecondary = Color(0xFF111827),
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF0B1F44),
    tertiaryContainer = WantedBlueContainerDark,
    onTertiaryContainer = WantedBlueContainerLight,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder,
    outlineVariant = Color(0xFF1E2430),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = ErrorRedContainerDark,
    onErrorContainer = ErrorRedContainerLight
)

val MarketZedShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun MarketZedTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) MarketZedDarkColors else MarketZedLightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            @Suppress("DEPRECATION")
            run {
                window.statusBarColor = AndroidColor.TRANSPARENT
                window.navigationBarColor = AndroidColor.TRANSPARENT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MarketZedTypography,
            shapes = MarketZedShapes,
            content = content
        )
    }
}
