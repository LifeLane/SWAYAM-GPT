package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppThemeMode(val title: String, val description: String) {
    LIGHT("Google Light", "Default clean, high-contrast Material 3 canvas"),
    GOOGLY_DARK("Googly Dark", "Authentic Google dark theme (#1F1F1F, #28292A, #8AB4F8)"),
    SYSTEM("System Default", "Follows system-wide display preference")
}

val LocalThemeMode = compositionLocalOf { AppThemeMode.LIGHT }
val LocalThemeUpdater = compositionLocalOf<(AppThemeMode) -> Unit> { {} }

// Google Signature Light Color Scheme (Default)
val GoogleLightColorScheme: ColorScheme = lightColorScheme(
    primary = GoogleBlue,
    onPrimary = Color.White,
    primaryContainer = GoogleBlueContainerLight,
    onPrimaryContainer = GoogleBlueOnContainerLight,
    secondary = GoogleLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = GoogleLightSecondaryContainer,
    onSecondaryContainer = GoogleLightOnSecondaryContainer,
    tertiary = GoogleLightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = GoogleLightTertiaryContainer,
    onTertiaryContainer = Color(0xFF221B00),
    background = GoogleLightBackground,
    onBackground = GoogleLightTextPrimary,
    surface = GoogleLightSurface,
    onSurface = GoogleLightTextPrimary,
    surfaceVariant = GoogleLightSurfaceVariant,
    onSurfaceVariant = GoogleLightTextSecondary,
    outline = GoogleLightOutline,
    outlineVariant = GoogleLightOutlineVariant,
    error = GoogleRed,
    onError = Color.White,
    errorContainer = GoogleRedContainer,
    onErrorContainer = Color(0xFF410002)
)

// Authentic Googly Dark Color Scheme
val GoogleDarkColorScheme: ColorScheme = darkColorScheme(
    primary = GoogleBlueDark,
    onPrimary = Color(0xFF062E6F),
    primaryContainer = GoogleBlueContainerDark,
    onPrimaryContainer = GoogleBlueOnContainerDark,
    secondary = GoogleDarkSecondary,
    onSecondary = Color(0xFF003453),
    secondaryContainer = GoogleDarkSecondaryContainer,
    onSecondaryContainer = GoogleDarkOnSecondaryContainer,
    tertiary = GoogleDarkTertiary,
    onTertiary = Color(0xFF383000),
    tertiaryContainer = GoogleDarkTertiaryContainer,
    onTertiaryContainer = Color(0xFFFFE16E),
    background = GoogleDarkBackground,
    onBackground = GoogleDarkTextPrimary,
    surface = GoogleDarkSurface,
    onSurface = GoogleDarkTextPrimary,
    surfaceVariant = GoogleDarkSurfaceVariant,
    onSurfaceVariant = GoogleDarkTextSecondary,
    outline = GoogleDarkOutline,
    outlineVariant = GoogleDarkOutlineVariant,
    error = GoogleRedDark,
    onError = Color(0xFF601410),
    errorContainer = GoogleRedContainerDark,
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun EdgeAITheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.GOOGLY_DARK -> true
        AppThemeMode.SYSTEM -> systemDark
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> GoogleDarkColorScheme
        else -> GoogleLightColorScheme
    }

    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalThemeUpdater provides onThemeModeChange
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

