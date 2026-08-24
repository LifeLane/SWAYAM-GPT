package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================================================================
// SIGNATURE GOOGLY PALETTE (Google Blue, Red, Yellow, Green)
// ==============================================================================

// Google Primary Brand Tones
val GoogleBlue = Color(0xFF1A73E8)           // Google Blue Primary (Light)
val GoogleBlueDark = Color(0xFF8AB4F8)       // Google Blue Dark Primary
val GoogleBlueContainerLight = Color(0xFFD3E3FD) // Soft blue chip / container
val GoogleBlueOnContainerLight = Color(0xFF041E49)
val GoogleBlueContainerDark = Color(0xFF0842A0)
val GoogleBlueOnContainerDark = Color(0xFFD3E3FD)

// Google Accent Colors
val GoogleRed = Color(0xFFEA4335)            // Google Red
val GoogleRedDark = Color(0xFFF28B82)
val GoogleRedContainer = Color(0xFFFCE8E6)
val GoogleRedContainerDark = Color(0xFF601410)

val GoogleYellow = Color(0xFFFBBC04)         // Google Yellow / Amber
val GoogleYellowDark = Color(0xFFFDD663)
val GoogleYellowContainer = Color(0xFFFEF7E0)
val GoogleYellowContainerDark = Color(0xFF4C3E00)

val GoogleGreen = Color(0xFF34A853)          // Google Green
val GoogleGreenDark = Color(0xFF81C995)
val GoogleGreenContainer = Color(0xFFE6F4EA)
val GoogleGreenContainerDark = Color(0xFF0D522C)

// ==============================================================================
// LIGHT THEME SURFACES & TOKENS (Default)
// Clean, crisp Google-standard light canvas with high contrast typography
// ==============================================================================
val GoogleLightBackground = Color(0xFFF8F9FA)       // Signature Google Canvas Light
val GoogleLightSurface = Color(0xFFFFFFFF)          // Pure White Google Card
val GoogleLightSurfaceVariant = Color(0xFFF1F3F4)   // Google Tool/Chip Grey
val GoogleLightOutline = Color(0xFFDADCE0)          // Crisp 1dp Google Border
val GoogleLightOutlineVariant = Color(0xFFE8EAED)   // Subtle Dividers
val GoogleLightTextPrimary = Color(0xFF202124)      // Google Charcoal Heading
val GoogleLightTextSecondary = Color(0xFF5F6368)    // Google Secondary Label

// Secondary & Tertiary for Light
val GoogleLightSecondary = Color(0xFF00639B)
val GoogleLightSecondaryContainer = Color(0xFFC2E7FF)
val GoogleLightOnSecondaryContainer = Color(0xFF001D32)
val GoogleLightTertiary = Color(0xFF6C5E00)
val GoogleLightTertiaryContainer = Color(0xFFFFE16E)

// ==============================================================================
// GOOGLY DARK THEME SURFACES & TOKENS
// Authentic Google Dark Theme (#1F1F1F, #28292A, #3C4043, #8AB4F8)
// ==============================================================================
val GoogleDarkBackground = Color(0xFF1F1F1F)        // Google Dark Base Canvas
val GoogleDarkSurface = Color(0xFF28292A)           // Google Dark Card Surface
val GoogleDarkSurfaceVariant = Color(0xFF353739)    // Google Dark Variant
val GoogleDarkSurfaceHigh = Color(0xFF3C4043)       // Google Elevated Pill/Chip
val GoogleDarkOutline = Color(0xFF5F6368)           // Google Dark Card Outline
val GoogleDarkOutlineVariant = Color(0xFF3C4043)    // Subtle Dark Divider
val GoogleDarkTextPrimary = Color(0xFFE8EAED)       // Google Light Grey Text
val GoogleDarkTextSecondary = Color(0xFF9AA0A6)     // Google Muted Label

// Secondary & Tertiary for Dark
val GoogleDarkSecondary = Color(0xFF7FCFFF)
val GoogleDarkSecondaryContainer = Color(0xFF004B76)
val GoogleDarkOnSecondaryContainer = Color(0xFFC2E7FF)
val GoogleDarkTertiary = Color(0xFFE9C400)
val GoogleDarkTertiaryContainer = Color(0xFF514600)

// ==============================================================================
// SEMANTIC STATUS & INTELLIGENCE EXECUTION COLORS
// ==============================================================================
val LocalAIGreen = Color(0xFF34A853)
val LocalAIGreenContainer = Color(0xFFE6F4EA)
val PrivateServerAmber = Color(0xFFFBBC04)
val PrivateServerAmberContainer = Color(0xFFFEF7E0)
val CloudAIPurple = Color(0xFF1A73E8)
val CloudAIPurpleContainer = Color(0xFFD3E3FD)
val OfflineGray = Color(0xFF5F6368)

val RiskLow = Color(0xFF34A853)
val RiskMedium = Color(0xFFFBBC04)
val RiskHigh = Color(0xFFEA4335)
val CloudAIBorder = Color(0xFF1A73E8)

// ==============================================================================
// BACKWARD COMPATIBILITY ALIASES
// ==============================================================================
val GeoPurplePrimary = GoogleBlue
val GeoPurpleDark = GoogleBlueDark
val GeoOnPrimary = Color.White
val GeoPrimaryContainer = GoogleBlueContainerLight
val GeoOnPrimaryContainer = GoogleBlueOnContainerLight
val GeoSecondary = GoogleLightSecondary
val GeoSecondaryContainer = GoogleLightSecondaryContainer
val GeoTertiary = GoogleLightTertiary
val GeoTertiaryContainer = GoogleLightTertiaryContainer

val GeoBackgroundLight = GoogleLightBackground
val GeoSurfaceLight = GoogleLightSurface
val GeoSurfaceVariantLight = GoogleLightSurfaceVariant
val GeoCardBorderLight = GoogleLightOutline
val GeoBorderSubtle = GoogleLightOutlineVariant
val GeoTextPrimaryLight = GoogleLightTextPrimary
val GeoTextSecondaryLight = GoogleLightTextSecondary

val GeoBackgroundDark = GoogleDarkBackground
val GeoSurfaceDark = GoogleDarkSurface
val GeoSurfaceVariantDark = GoogleDarkSurfaceVariant
val GeoCardBorderDark = GoogleDarkOutline
val GeoBorderSubtleDark = GoogleDarkOutlineVariant
val GeoTextPrimaryDark = GoogleDarkTextPrimary
val GeoTextSecondaryDark = GoogleDarkTextSecondary

val GeoTerminalDark = Color(0xFF1E1E1E)
val GeoTerminalPrompt = GoogleBlueDark
val GeoTerminalText = Color(0xFFE8EAED)
val GeoTerminalBorder = Color(0xFF3C4043)

val CobaltBluePrimary = GoogleBlue
val CobaltBlueDark = GoogleBlueDark
val ElectricIndigo = GoogleBlue
val CyanAccent = GoogleLightSecondary
val SlateBackgroundLight = GoogleLightBackground
val SlateSurfaceLight = GoogleLightSurface
val SlateSurfaceVariantLight = GoogleLightSurfaceVariant
val SlateCardBorderLight = GoogleLightOutline
val SlateTextPrimaryLight = GoogleLightTextPrimary
val SlateTextSecondaryLight = GoogleLightTextSecondary
val SlateBackgroundDark = GoogleDarkBackground
val SlateSurfaceDark = GoogleDarkSurface
val SlateSurfaceVariantDark = GoogleDarkSurfaceVariant
val SlateCardBorderDark = GoogleDarkOutline
val SlateTextPrimaryDark = GoogleDarkTextPrimary
val SlateTextSecondaryDark = GoogleDarkTextSecondary

// ==============================================================================
// MONOCHROME RESEARCH (BLACK & WHITE HIGH-CONTRAST FOR RESEARCHERS)
// ==============================================================================
val MonoBlack = Color(0xFF000000)
val MonoOffBlack = Color(0xFF121212)
val MonoDarkCard = Color(0xFF1C1C1C)
val MonoCharcoalBorder = Color(0xFF333333)
val MonoSlateBorder = Color(0xFF555555)
val MonoLightBorder = Color(0xFFD0D0D0)
val MonoPureWhite = Color(0xFFFFFFFF)
val MonoOffWhite = Color(0xFFF7F7F7)
val MonoLightCard = Color(0xFFFFFFFF)
val MonoPaperVariant = Color(0xFFEDEDED)
val MonoTextPrimary = Color(0xFF111111)
val MonoTextSecondary = Color(0xFF555555)
val MonoTextInvertedPrimary = Color(0xFFEEEEEE)
val MonoTextInvertedSecondary = Color(0xFFAAAAAA)
