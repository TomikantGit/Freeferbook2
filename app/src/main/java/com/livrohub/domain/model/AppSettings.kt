package com.livrohub.domain.model

/**
 * Enum defining the visual theme of the application.
 */
enum class AppTheme {
    MODERN_LIGHT, MODERN_DARK, AMOLED, MINIMAL, GLASSMORPHISM,
    NEUMORPHISM, MATERIAL_INSPIRED, FLUENT_INSPIRED, CORPORATE,
    CYBERPUNK, SYNTHWAVE, NORD, DRACULA, CATPPUCCIN, SOLARIZED,
    FOREST, OCEAN, SUNSET, MIDNIGHT, HIGH_CONTRAST
}

/**
 * Enum defining the spatial density of the UI components.
 */
enum class AppDensity {
    COMPACT, NORMAL, COMFORTABLE
}

/**
 * Enum defining the position of the sidebar (if used).
 */
enum class SidebarPosition {
    LEFT, RIGHT
}

/**
 * Enum defining the global navigation style.
 */
enum class NavigationStyle {
    SIDEBAR, BOTTOM_NAV, DRAWER
}

/**
 * Enum defining the visual style of cards.
 */
enum class CardStyle {
    ELEVATED, OUTLINED, FLAT, GLASS
}

/**
 * Enum defining the visual style of buttons.
 */
enum class ButtonStyle {
    FILLED, OUTLINED, TEXT, NEUMORPHIC
}

/**
 * Enum defining the icon set style.
 */
enum class IconPack {
    DEFAULT, ROUNDED, SHARP, TWO_TONE, MINIMAL
}

/**
 * Enum defining the preferred font family.
 */
enum class FontFamilyPreference {
    INTER, GEIST, ROBOTO, SF_PRO, IBM_PLEX_SANS, NUNITO, POPPINS, OPENDYSLEXIC
}

/**
 * Enum defining the scaling multiplier for fonts.
 */
enum class FontScaling {
    SMALL, DEFAULT, LARGE, EXTRA_LARGE
}

/**
 * Enum defining the global font weight preference.
 */
enum class FontWeightPreference {
    LIGHT, NORMAL, MEDIUM, BOLD
}

/**
 * Data class representing all user-configurable visual settings.
 *
 * @param theme The selected application theme.
 * @param primaryColorHex Optional custom primary color override.
 * @param secondaryColorHex Optional custom secondary color override.
 * @param accentColorHex Optional custom accent color override.
 * @param borderRadiusFactor Multiplier for all component border radii.
 * @param animationSpeedFactor Multiplier for transition and animation speeds.
 * @param animationsEnabled Whether animations are globally enabled.
 * @param density Global component padding and spacing density.
 * @param sidebarWidthDp Desired width for sidebars in DP.
 * @param sidebarPosition Preferred sidebar position.
 * @param navigationStyle Preferred navigation paradigm.
 * @param cardStyle Visual style for all Card elements.
 * @param buttonStyle Visual style for all Button elements.
 * @param shadowIntensity Multiplier for elevation shadows.
 * @param transparencyEnabled Whether glassmorphism effects are enabled.
 * @param blurIntensity Multiplier for background blur effects.
 * @param iconPack Global icon style choice.
 * @param fontFamily Selected font family.
 * @param fontScaling Selected font size multiplier.
 * @param fontSizeSp Base font size in SP.
 * @param fontWeight Base font weight.
 * @param letterSpacingSp Additional letter spacing.
 * @param lineHeightMultiplier Line height multiplier for readable text.
 * @param showLineNumbers Whether to show line numbers in the editor.
 * @param reducedMotion Whether to reduce complex motion for accessibility.
 * @param showCharactersTab Whether to show the characters tab in the workspace.
 * @param showLocationsTab Whether to show the locations tab in the workspace.
 */
data class AppSettings(
    val theme: AppTheme = AppTheme.MODERN_LIGHT,
    val primaryColorHex: String? = null,
    val secondaryColorHex: String? = null,
    val accentColorHex: String? = null,
    val borderRadiusFactor: Float = 1f, // Multiplier for border radius
    val animationSpeedFactor: Float = 1f,
    val animationsEnabled: Boolean = true,
    val density: AppDensity = AppDensity.NORMAL,
    val sidebarWidthDp: Int = 250,
    val sidebarPosition: SidebarPosition = SidebarPosition.LEFT,
    val navigationStyle: NavigationStyle = NavigationStyle.SIDEBAR,
    val cardStyle: CardStyle = CardStyle.ELEVATED,
    val buttonStyle: ButtonStyle = ButtonStyle.FILLED,
    val shadowIntensity: Float = 1f,
    val transparencyEnabled: Boolean = true,
    val blurIntensity: Float = 1f,
    val iconPack: IconPack = IconPack.DEFAULT,
    val fontFamily: FontFamilyPreference = FontFamilyPreference.INTER,
    val fontScaling: FontScaling = FontScaling.DEFAULT,
    val fontSizeSp: Int = 16, // Base font size
    val fontWeight: FontWeightPreference = FontWeightPreference.NORMAL,
    val letterSpacingSp: Float = 0f,
    val lineHeightMultiplier: Float = 1.2f,
    val showLineNumbers: Boolean = true,
    val reducedMotion: Boolean = false,
    val showCharactersTab: Boolean = true,
    val showLocationsTab: Boolean = true,
    val showWorkspaceTutorial: Boolean = true,
    val showEditorTutorial: Boolean = true,
    val showHistoryTutorial: Boolean = true
)
