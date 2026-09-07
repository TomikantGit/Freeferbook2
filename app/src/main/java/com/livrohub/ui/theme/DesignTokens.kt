package com.livrohub.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class LivroHubColors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val border: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val onInfo: Color,
    val isLight: Boolean
)

@Immutable
data class LivroHubTypography(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)

@Immutable
data class LivroHubSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 16.dp,
    val l: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    val xxxl: Dp = 64.dp
)

@Immutable
data class LivroHubRadius(
    val none: Dp = 0.dp,
    val small: Dp = 4.dp,
    val medium: Dp = 8.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val full: Dp = 9999.dp
)

@Immutable
data class LivroHubShadows(
    val elevationNone: Dp = 0.dp,
    val elevationLow: Dp = 2.dp,
    val elevationMedium: Dp = 6.dp,
    val elevationHigh: Dp = 12.dp
)

@Immutable
data class LivroHubAnimations(
    val durationShort: Int = 150,
    val durationMedium: Int = 300,
    val durationLong: Int = 500
)

val LocalLivroHubColors = staticCompositionLocalOf<LivroHubColors> {
    error("No LivroHubColors provided")
}

val LocalLivroHubTypography = staticCompositionLocalOf<LivroHubTypography> {
    error("No LivroHubTypography provided")
}

val LocalLivroHubSpacing = staticCompositionLocalOf { LivroHubSpacing() }
val LocalLivroHubRadius = staticCompositionLocalOf { LivroHubRadius() }
val LocalLivroHubShadows = staticCompositionLocalOf { LivroHubShadows() }
val LocalLivroHubAnimations = staticCompositionLocalOf { LivroHubAnimations() }

object LivroHubTheme {
    val colors: LivroHubColors
        @androidx.compose.runtime.Composable
        get() = LocalLivroHubColors.current

    val typography: LivroHubTypography
        @androidx.compose.runtime.Composable
        get() = LocalLivroHubTypography.current

    val spacing: LivroHubSpacing
        @androidx.compose.runtime.Composable
        get() = LocalLivroHubSpacing.current

    val radius: LivroHubRadius
        @androidx.compose.runtime.Composable
        get() = LocalLivroHubRadius.current

    val shadows: LivroHubShadows
        @androidx.compose.runtime.Composable
        get() = LocalLivroHubShadows.current
        
    val animations: LivroHubAnimations
        @androidx.compose.runtime.Composable
        get() = LocalLivroHubAnimations.current
}
