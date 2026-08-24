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
    MONOCHROME_RESEARCH("Monochrome Light", "High-contrast black & white paper canvas for focused researchers"),
    MONOCHROME_DARK("Monochrome Noir", "Ultra-pure dark noir monochrome interface with zero color distraction"),
    SYSTEM("System Default", "Follows system-wide display preference")
}

val LocalThemeMode = compositionLocalOf { AppThemeMode.LIGHT }
val LocalThemeUpdater = compositionLocalOf<(AppThemeMode) -> Unit> { {} }

// Monochrome Research Light Color Scheme (Paper Minimalist B&W)
val MonochromeLightColorScheme: ColorScheme = lightColorScheme(
    primary = MonoBlack,
    onPrimary = MonoPureWhite,
    primaryContainer = MonoPaperVariant,
    onPrimaryContainer = MonoBlack,
    secondary = MonoTextSecondary,
    onSecondary = MonoPureWhite,
    secondaryContainer = MonoPaperVariant,
    onSecondaryContainer = MonoTextPrimary,
    tertiary = MonoTextSecondary,
    onTertiary = MonoPureWhite,
    tertiaryContainer = MonoPaperVariant,
    onTertiaryContainer = MonoBlack,
    background = MonoPureWhite,
    onBackground = MonoTextPrimary,
    surface = MonoPureWhite,
    onSurface = MonoTextPrimary,
    surfaceVariant = MonoPaperVariant,
    onSurfaceVariant = MonoTextSecondary,
    outline = MonoSlateBorder,
    outlineVariant = MonoLightBorder,
    error = Color(0xFF222222),
    onError = MonoPureWhite,
    errorContainer = MonoPaperVariant,
    onErrorContainer = MonoBlack
)

// Monochrome Research Dark Color Scheme (Noir Pure B&W)
val MonochromeDarkColorScheme: ColorScheme = darkColorScheme(
    primary = MonoPureWhite,
    onPrimary = MonoBlack,
    primaryContainer = MonoDarkCard,
    onPrimaryContainer = MonoPureWhite,
    secondary = MonoTextInvertedSecondary,
    onSecondary = MonoBlack,
    secondaryContainer = MonoDarkCard,
    onSecondaryContainer = MonoTextInvertedPrimary,
    tertiary = MonoTextInvertedSecondary,
    onTertiary = MonoBlack,
    tertiaryContainer = MonoDarkCard,
    onTertiaryContainer = MonoPureWhite,
    background = MonoBlack,
    onBackground = MonoTextInvertedPrimary,
    surface = MonoOffBlack,
    onSurface = MonoTextInvertedPrimary,
    surfaceVariant = MonoDarkCard,
    onSurfaceVariant = MonoTextInvertedSecondary,
    outline = MonoSlateBorder,
    outlineVariant = MonoCharcoalBorder,
    error = MonoPureWhite,
    onError = MonoBlack,
    errorContainer = MonoDarkCard,
    onErrorContainer = MonoPureWhite
)

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
        AppThemeMode.MONOCHROME_RESEARCH -> false
        AppThemeMode.MONOCHROME_DARK -> true
        AppThemeMode.SYSTEM -> systemDark
    }

    val context = LocalContext.current
    val colorScheme = when (themeMode) {
        AppThemeMode.MONOCHROME_RESEARCH -> MonochromeLightColorScheme
        AppThemeMode.MONOCHROME_DARK -> MonochromeDarkColorScheme
        AppThemeMode.GOOGLY_DARK -> GoogleDarkColorScheme
        AppThemeMode.LIGHT -> GoogleLightColorScheme
        AppThemeMode.SYSTEM -> {
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDark) GoogleDarkColorScheme else GoogleLightColorScheme
            }
        }
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

