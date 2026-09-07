package com.livrohub.ui.theme

import androidx.compose.ui.graphics.Color

// Default Palettes Helper
private fun lightColors(
    primary: Color, secondary: Color, accent: Color,
    background: Color = Color(0xFFFAFAFA),
    surface: Color = Color(0xFFFFFFFF),
    border: Color = Color(0xFFE0E0E0),
    isHighContrast: Boolean = false
) = LivroHubColors(
    primary = primary, onPrimary = Color.White,
    secondary = secondary, onSecondary = Color.White,
    accent = accent, onAccent = Color.White,
    background = background, onBackground = if (isHighContrast) Color.Black else Color(0xFF1C1C1E),
    surface = surface, onSurface = if (isHighContrast) Color.Black else Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFF0F0F0), onSurfaceVariant = Color(0xFF49454F),
    border = if (isHighContrast) Color.Black else border,
    error = Color(0xFFB3261E), onError = Color.White,
    success = Color(0xFF4CAF50), onSuccess = Color.White,
    warning = Color(0xFFFF9800), onWarning = Color.White,
    info = Color(0xFF2196F3), onInfo = Color.White,
    isLight = true
)

private fun darkColors(
    primary: Color, secondary: Color, accent: Color,
    background: Color = Color(0xFF121212),
    surface: Color = Color(0xFF1E1E1E),
    border: Color = Color(0xFF333333),
    isHighContrast: Boolean = false
) = LivroHubColors(
    primary = primary, onPrimary = Color.White,
    secondary = secondary, onSecondary = Color.White,
    accent = accent, onAccent = Color.White,
    background = background, onBackground = if (isHighContrast) Color.White else Color(0xFFE3E3E3),
    surface = surface, onSurface = if (isHighContrast) Color.White else Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF2C2C2C), onSurfaceVariant = Color(0xFFCACACA),
    border = if (isHighContrast) Color.White else border,
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    success = Color(0xFF81C784), onSuccess = Color(0xFF003300),
    warning = Color(0xFFFFB74D), onWarning = Color(0xFF4D2C00),
    info = Color(0xFF64B5F6), onInfo = Color(0xFF002244),
    isLight = false
)

object ThemePalettes {
    val ModernLight = lightColors(
        primary = Color(0xFF0F172A), secondary = Color(0xFF334155), accent = Color(0xFF3B82F6)
    )
    val ModernDark = darkColors(
        primary = Color(0xFFF8FAFC), secondary = Color(0xFF94A3B8), accent = Color(0xFF3B82F6)
    )
    val Amoled = darkColors(
        primary = Color(0xFFFFFFFF), secondary = Color(0xFFAAAAAA), accent = Color(0xFF00E5FF),
        background = Color.Black, surface = Color(0xFF0A0A0A), border = Color(0xFF1A1A1A)
    )
    val Minimal = lightColors(
        primary = Color(0xFF000000), secondary = Color(0xFF666666), accent = Color(0xFF000000),
        background = Color.White, surface = Color.White, border = Color(0xFFF0F0F0)
    )
    val Glassmorphism = darkColors(
        primary = Color(0xFFFFFFFF), secondary = Color(0xFFB3E5FC), accent = Color(0xFFE040FB),
        background = Color(0xFF0F172A), surface = Color(0x33FFFFFF), border = Color(0x33FFFFFF)
    )
    val Neumorphism = lightColors(
        primary = Color(0xFF555555), secondary = Color(0xFF888888), accent = Color(0xFF3B82F6),
        background = Color(0xFFE0E5EC), surface = Color(0xFFE0E5EC), border = Color(0xFFD1D9E6)
    )
    val MaterialInspired = lightColors(
        primary = Color(0xFF6750A4), secondary = Color(0xFF625B71), accent = Color(0xFF7D5260),
        background = Color(0xFFFFFBFE), surface = Color(0xFFFFFBFE)
    )
    val FluentInspired = lightColors(
        primary = Color(0xFF0078D4), secondary = Color(0xFF2B88D8), accent = Color(0xFF005A9E),
        background = Color(0xFFF3F2F1), surface = Color.White
    )
    val Corporate = lightColors(
        primary = Color(0xFF002050), secondary = Color(0xFF4B535E), accent = Color(0xFF0072C6),
        background = Color(0xFFF8F9FA), surface = Color.White
    )
    val Cyberpunk = darkColors(
        primary = Color(0xFFFCE205), secondary = Color(0xFFFF003C), accent = Color(0xFF00FFFF),
        background = Color(0xFF0B0A10), surface = Color(0xFF14121F), border = Color(0xFF2E294E)
    )
    val Synthwave = darkColors(
        primary = Color(0xFFFF71CE), secondary = Color(0xFF01CDFE), accent = Color(0xFF05FFA1),
        background = Color(0xFF2B0F4C), surface = Color(0xFF3E1A6D), border = Color(0xFF522888)
    )
    val Nord = darkColors(
        primary = Color(0xFF88C0D0), secondary = Color(0xFF81A1C1), accent = Color(0xFF5E81AC),
        background = Color(0xFF2E3440), surface = Color(0xFF3B4252), border = Color(0xFF4C566A)
    )
    val Dracula = darkColors(
        primary = Color(0xFFBD93F9), secondary = Color(0xFFFF79C6), accent = Color(0xFF50FA7B),
        background = Color(0xFF282A36), surface = Color(0xFF44475A), border = Color(0xFF6272A4)
    )
    val Catppuccin = darkColors(
        primary = Color(0xFFCBA6F7), secondary = Color(0xFF89B4FA), accent = Color(0xFFF38BA8),
        background = Color(0xFF1E1E2E), surface = Color(0xFF313244), border = Color(0xFF45475A)
    )
    val Solarized = lightColors(
        primary = Color(0xFF268BD2), secondary = Color(0xFF2AA198), accent = Color(0xFFD33682),
        background = Color(0xFFFDF6E3), surface = Color(0xFFEEE8D5), border = Color(0xFF93A1A1)
    )
    val Forest = darkColors(
        primary = Color(0xFFA3BE8C), secondary = Color(0xFF8FBCBB), accent = Color(0xFFD08770),
        background = Color(0xFF1E2E24), surface = Color(0xFF2B4033), border = Color(0xFF3C5645)
    )
    val Ocean = darkColors(
        primary = Color(0xFF48CAE4), secondary = Color(0xFF90E0EF), accent = Color(0xFF0077B6),
        background = Color(0xFF03045E), surface = Color(0xFF023E8A), border = Color(0xFF0077B6)
    )
    val Sunset = lightColors(
        primary = Color(0xFFF4A261), secondary = Color(0xFFE76F51), accent = Color(0xFFE9C46A),
        background = Color(0xFFFDF8F5), surface = Color.White, border = Color(0xFFF4E1D2)
    )
    val Midnight = darkColors(
        primary = Color(0xFF7B2CBF), secondary = Color(0xFF9D4EDD), accent = Color(0xFFC77DFF),
        background = Color(0xFF10002B), surface = Color(0xFF240046), border = Color(0xFF3C096C)
    )
    val HighContrast = darkColors(
        primary = Color.White, secondary = Color(0xFFFFFF00), accent = Color(0xFF00FFFF),
        background = Color.Black, surface = Color.Black, border = Color.White, isHighContrast = true
    )
}
