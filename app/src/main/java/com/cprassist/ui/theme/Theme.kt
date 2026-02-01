package com.cprassist.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * High-contrast color palette optimized for emergency use.
 * Colors are chosen for maximum visibility in bright outdoor conditions
 * and compatibility with color-blind users.
 */

// Primary colors - Red for emergency/critical
val EmergencyRed = Color(0xFFD32F2F)
val EmergencyRedDark = Color(0xFFB71C1C)
val EmergencyRedLight = Color(0xFFFF6659)

// Secondary colors - Blue for professional mode
val ProfessionalBlue = Color(0xFF1976D2)
val ProfessionalBlueDark = Color(0xFF0D47A1)
val ProfessionalBlueLight = Color(0xFF63A4FF)

// Success/ROSC - Green
val ROSCGreen = Color(0xFF388E3C)
val ROSCGreenLight = Color(0xFF4CAF50)

// Warning - Amber
val WarningAmber = Color(0xFFFFA000)
val WarningAmberLight = Color(0xFFFFB300)

// Background colors
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2D2D2D)

// Text colors
val HighContrastWhite = Color(0xFFFFFFFF)
val HighContrastBlack = Color(0xFF000000)
val TextOnDark = Color(0xFFFAFAFA)
val TextOnDarkSecondary = Color(0xFFB0B0B0)

// Button colors for quick identification
val ButtonAirway = Color(0xFF2196F3)      // Blue - airways
val ButtonRhythm = Color(0xFF9C27B0)       // Purple - rhythms
val ButtonIntervention = Color(0xFFFF9800) // Orange - interventions
val ButtonOutcome = Color(0xFF4CAF50)      // Green - outcomes

/**
 * Dark color scheme - primary for emergency use
 * High contrast for visibility in all conditions
 */
private val EmergencyDarkColorScheme = darkColorScheme(
    primary = EmergencyRed,
    onPrimary = HighContrastWhite,
    primaryContainer = EmergencyRedDark,
    onPrimaryContainer = HighContrastWhite,

    secondary = ProfessionalBlue,
    onSecondary = HighContrastWhite,
    secondaryContainer = ProfessionalBlueDark,
    onSecondaryContainer = HighContrastWhite,

    tertiary = ROSCGreen,
    onTertiary = HighContrastWhite,
    tertiaryContainer = ROSCGreen,
    onTertiaryContainer = HighContrastWhite,

    error = EmergencyRedLight,
    onError = HighContrastBlack,

    background = DarkBackground,
    onBackground = TextOnDark,

    surface = DarkSurface,
    onSurface = TextOnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextOnDarkSecondary,

    outline = Color(0xFF666666)
)

/**
 * Light color scheme - alternative for indoor use
 */
private val EmergencyLightColorScheme = lightColorScheme(
    primary = EmergencyRed,
    onPrimary = HighContrastWhite,
    primaryContainer = EmergencyRedLight,
    onPrimaryContainer = HighContrastBlack,

    secondary = ProfessionalBlue,
    onSecondary = HighContrastWhite,
    secondaryContainer = ProfessionalBlueLight,
    onSecondaryContainer = HighContrastBlack,

    tertiary = ROSCGreen,
    onTertiary = HighContrastWhite,
    tertiaryContainer = ROSCGreenLight,
    onTertiaryContainer = HighContrastBlack,

    error = EmergencyRed,
    onError = HighContrastWhite,

    background = HighContrastWhite,
    onBackground = HighContrastBlack,

    surface = Color(0xFFF5F5F5),
    onSurface = HighContrastBlack,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF424242),

    outline = Color(0xFF999999)
)

@Composable
fun CPRAssistTheme(
    darkTheme: Boolean = true, // Default to dark for better outdoor visibility
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) EmergencyDarkColorScheme else EmergencyLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
