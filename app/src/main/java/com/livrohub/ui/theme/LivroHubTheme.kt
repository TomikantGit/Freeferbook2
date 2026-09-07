package com.livrohub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livrohub.domain.model.AppDensity
import com.livrohub.domain.model.AppSettings
import com.livrohub.domain.model.AppTheme
import com.livrohub.domain.model.FontFamilyPreference
import com.livrohub.domain.model.FontScaling
import com.livrohub.domain.model.FontWeightPreference

private fun resolveColors(settings: AppSettings, isSystemDark: Boolean): LivroHubColors {
    val baseColors = when (settings.theme) {
        AppTheme.MODERN_LIGHT -> ThemePalettes.ModernLight
        AppTheme.MODERN_DARK -> ThemePalettes.ModernDark
        AppTheme.AMOLED -> ThemePalettes.Amoled
        AppTheme.MINIMAL -> ThemePalettes.Minimal
        AppTheme.GLASSMORPHISM -> ThemePalettes.Glassmorphism
        AppTheme.NEUMORPHISM -> ThemePalettes.Neumorphism
        AppTheme.MATERIAL_INSPIRED -> ThemePalettes.MaterialInspired
        AppTheme.FLUENT_INSPIRED -> ThemePalettes.FluentInspired
        AppTheme.CORPORATE -> ThemePalettes.Corporate
        AppTheme.CYBERPUNK -> ThemePalettes.Cyberpunk
        AppTheme.SYNTHWAVE -> ThemePalettes.Synthwave
        AppTheme.NORD -> ThemePalettes.Nord
        AppTheme.DRACULA -> ThemePalettes.Dracula
        AppTheme.CATPPUCCIN -> ThemePalettes.Catppuccin
        AppTheme.SOLARIZED -> ThemePalettes.Solarized
        AppTheme.FOREST -> ThemePalettes.Forest
        AppTheme.OCEAN -> ThemePalettes.Ocean
        AppTheme.SUNSET -> ThemePalettes.Sunset
        AppTheme.MIDNIGHT -> ThemePalettes.Midnight
        AppTheme.HIGH_CONTRAST -> ThemePalettes.HighContrast
    }

    // Override colors if user customized them
    return baseColors.copy(
        primary = settings.primaryColorHex?.let { parseColor(it) } ?: baseColors.primary,
        secondary = settings.secondaryColorHex?.let { parseColor(it) } ?: baseColors.secondary,
        accent = settings.accentColorHex?.let { parseColor(it) } ?: baseColors.accent,
    )
}

private fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Transparent // Fallback
    }
}

private fun resolveTypography(settings: AppSettings): LivroHubTypography {
    val fontFamily = when (settings.fontFamily) {
        FontFamilyPreference.INTER, FontFamilyPreference.ROBOTO, FontFamilyPreference.GEIST -> FontFamily.SansSerif
        FontFamilyPreference.SF_PRO -> FontFamily.Default
        FontFamilyPreference.IBM_PLEX_SANS -> FontFamily.SansSerif
        FontFamilyPreference.NUNITO, FontFamilyPreference.POPPINS -> FontFamily.SansSerif
        FontFamilyPreference.OPENDYSLEXIC -> FontFamily.Serif
    }

    val scalingFactor = when (settings.fontScaling) {
        FontScaling.SMALL -> 0.85f
        FontScaling.DEFAULT -> 1.0f
        FontScaling.LARGE -> 1.2f
        FontScaling.EXTRA_LARGE -> 1.5f
    }

    val weight = when (settings.fontWeight) {
        FontWeightPreference.LIGHT -> FontWeight.Light
        FontWeightPreference.NORMAL -> FontWeight.Normal
        FontWeightPreference.MEDIUM -> FontWeight.Medium
        FontWeightPreference.BOLD -> FontWeight.Bold
    }

    val ls = settings.letterSpacingSp.sp
    val lh = settings.lineHeightMultiplier

    fun buildStyle(baseSize: Int) = TextStyle(
        fontFamily = fontFamily,
        fontWeight = weight,
        fontSize = (baseSize * scalingFactor).sp,
        letterSpacing = ls,
        lineHeight = (baseSize * scalingFactor * lh).sp
    )

    return LivroHubTypography(
        displayLarge = buildStyle(57),
        displayMedium = buildStyle(45),
        displaySmall = buildStyle(36),
        headlineLarge = buildStyle(32),
        headlineMedium = buildStyle(28),
        headlineSmall = buildStyle(24),
        titleLarge = buildStyle(22),
        titleMedium = buildStyle(16),
        titleSmall = buildStyle(14),
        bodyLarge = buildStyle(16),
        bodyMedium = buildStyle(14),
        bodySmall = buildStyle(12),
        labelLarge = buildStyle(14),
        labelMedium = buildStyle(12),
        labelSmall = buildStyle(11)
    )
}

private fun resolveSpacing(settings: AppSettings): LivroHubSpacing {
    val multiplier = when (settings.density) {
        AppDensity.COMPACT -> 0.75f
        AppDensity.NORMAL -> 1.0f
        AppDensity.COMFORTABLE -> 1.5f
    }
    return LivroHubSpacing(
        xxs = (2 * multiplier).dp,
        xs = (4 * multiplier).dp,
        s = (8 * multiplier).dp,
        m = (16 * multiplier).dp,
        l = (24 * multiplier).dp,
        xl = (32 * multiplier).dp,
        xxl = (48 * multiplier).dp,
        xxxl = (64 * multiplier).dp
    )
}

private fun resolveRadius(settings: AppSettings): LivroHubRadius {
    val f = settings.borderRadiusFactor
    return LivroHubRadius(
        small = (4 * f).dp,
        medium = (8 * f).dp,
        large = (16 * f).dp,
        extraLarge = (24 * f).dp
    )
}

private fun resolveShadows(settings: AppSettings): LivroHubShadows {
    val f = settings.shadowIntensity
    return LivroHubShadows(
        elevationLow = (2 * f).dp,
        elevationMedium = (6 * f).dp,
        elevationHigh = (12 * f).dp
    )
}

private fun resolveAnimations(settings: AppSettings): LivroHubAnimations {
    if (!settings.animationsEnabled || settings.reducedMotion) {
        return LivroHubAnimations(0, 0, 0)
    }
    val f = settings.animationSpeedFactor
    // If f is > 1, speed is faster, meaning duration is shorter.
    val durationMultiplier = 1f / f
    return LivroHubAnimations(
        durationShort = (150 * durationMultiplier).toInt(),
        durationMedium = (300 * durationMultiplier).toInt(),
        durationLong = (500 * durationMultiplier).toInt()
    )
}

@Composable
fun LivroHubTheme(
    settings: AppSettings,
    isSystemDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = resolveColors(settings, isSystemDark)
    val typography = resolveTypography(settings)
    val spacing = resolveSpacing(settings)
    val radius = resolveRadius(settings)
    val shadows = resolveShadows(settings)
    val animations = resolveAnimations(settings)

    CompositionLocalProvider(
        LocalLivroHubColors provides colors,
        LocalLivroHubTypography provides typography,
        LocalLivroHubSpacing provides spacing,
        LocalLivroHubRadius provides radius,
        LocalLivroHubShadows provides shadows,
        LocalLivroHubAnimations provides animations
    ) {
        // Here we could wrap MaterialTheme if we still want to reuse some Material components.
        // For a full custom design system, we bypass it. But for ease of migration, we can provide Material3 defaults
        // mapping our tokens to MaterialTheme.
        androidx.compose.material3.MaterialTheme(
            colorScheme = androidx.compose.material3.ColorScheme(
                primary = colors.primary, onPrimary = colors.onPrimary,
                primaryContainer = colors.primary, onPrimaryContainer = colors.onPrimary,
                inversePrimary = colors.primary,
                secondary = colors.secondary, onSecondary = colors.onSecondary,
                secondaryContainer = colors.secondary, onSecondaryContainer = colors.onSecondary,
                tertiary = colors.accent, onTertiary = colors.onAccent,
                tertiaryContainer = colors.accent, onTertiaryContainer = colors.onAccent,
                background = colors.background, onBackground = colors.onBackground,
                surface = colors.surface, onSurface = colors.onSurface,
                surfaceVariant = colors.surfaceVariant, onSurfaceVariant = colors.onSurfaceVariant,
                surfaceTint = colors.primary, inverseSurface = colors.onSurface,
                inverseOnSurface = colors.surface, error = colors.error, onError = colors.onError,
                errorContainer = colors.error, onErrorContainer = colors.onError,
                outline = colors.border, outlineVariant = colors.border,
                scrim = Color.Black.copy(alpha = 0.3f)
            ),
            content = content
        )
    }
}
