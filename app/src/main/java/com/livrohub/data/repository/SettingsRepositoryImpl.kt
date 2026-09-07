package com.livrohub.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.livrohub.domain.model.*
import com.livrohub.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val PRIMARY_COLOR = stringPreferencesKey("primary_color")
        val SECONDARY_COLOR = stringPreferencesKey("secondary_color")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val BORDER_RADIUS_FACTOR = floatPreferencesKey("border_radius_factor")
        val ANIMATION_SPEED_FACTOR = floatPreferencesKey("animation_speed_factor")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val DENSITY = stringPreferencesKey("density")
        val SIDEBAR_WIDTH = intPreferencesKey("sidebar_width")
        val SIDEBAR_POSITION = stringPreferencesKey("sidebar_position")
        val NAVIGATION_STYLE = stringPreferencesKey("navigation_style")
        val CARD_STYLE = stringPreferencesKey("card_style")
        val BUTTON_STYLE = stringPreferencesKey("button_style")
        val SHADOW_INTENSITY = floatPreferencesKey("shadow_intensity")
        val TRANSPARENCY_ENABLED = booleanPreferencesKey("transparency_enabled")
        val BLUR_INTENSITY = floatPreferencesKey("blur_intensity")
        val ICON_PACK = stringPreferencesKey("icon_pack")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val FONT_SCALING = stringPreferencesKey("font_scaling")
        val FONT_SIZE_SP = intPreferencesKey("font_size_sp")
        val FONT_WEIGHT = stringPreferencesKey("font_weight")
        val LETTER_SPACING = floatPreferencesKey("letter_spacing")
        val LINE_HEIGHT = floatPreferencesKey("line_height")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val SHOW_CHARACTERS_TAB = booleanPreferencesKey("show_characters_tab")
        val SHOW_LOCATIONS_TAB = booleanPreferencesKey("show_locations_tab")
        val SHOW_WORKSPACE_TUTORIAL = booleanPreferencesKey("show_workspace_tutorial")
        val SHOW_EDITOR_TUTORIAL = booleanPreferencesKey("show_editor_tutorial")
        val SHOW_HISTORY_TUTORIAL = booleanPreferencesKey("show_history_tutorial")
    }

    private fun mapPreferencesToSettings(preferences: Preferences): AppSettings {
        return AppSettings(
            theme = tryEnumValue(preferences[PreferencesKeys.THEME], AppTheme.MODERN_LIGHT),
            primaryColorHex = preferences[PreferencesKeys.PRIMARY_COLOR],
            secondaryColorHex = preferences[PreferencesKeys.SECONDARY_COLOR],
            accentColorHex = preferences[PreferencesKeys.ACCENT_COLOR],
            borderRadiusFactor = preferences[PreferencesKeys.BORDER_RADIUS_FACTOR] ?: 1f,
            animationSpeedFactor = preferences[PreferencesKeys.ANIMATION_SPEED_FACTOR] ?: 1f,
            animationsEnabled = preferences[PreferencesKeys.ANIMATIONS_ENABLED] ?: true,
            density = tryEnumValue(preferences[PreferencesKeys.DENSITY], AppDensity.NORMAL),
            sidebarWidthDp = preferences[PreferencesKeys.SIDEBAR_WIDTH] ?: 250,
            sidebarPosition = tryEnumValue(preferences[PreferencesKeys.SIDEBAR_POSITION], SidebarPosition.LEFT),
            navigationStyle = tryEnumValue(preferences[PreferencesKeys.NAVIGATION_STYLE], NavigationStyle.SIDEBAR),
            cardStyle = tryEnumValue(preferences[PreferencesKeys.CARD_STYLE], CardStyle.ELEVATED),
            buttonStyle = tryEnumValue(preferences[PreferencesKeys.BUTTON_STYLE], ButtonStyle.FILLED),
            shadowIntensity = preferences[PreferencesKeys.SHADOW_INTENSITY] ?: 1f,
            transparencyEnabled = preferences[PreferencesKeys.TRANSPARENCY_ENABLED] ?: true,
            blurIntensity = preferences[PreferencesKeys.BLUR_INTENSITY] ?: 1f,
            iconPack = tryEnumValue(preferences[PreferencesKeys.ICON_PACK], IconPack.DEFAULT),
            fontFamily = tryEnumValue(preferences[PreferencesKeys.FONT_FAMILY], FontFamilyPreference.INTER),
            fontScaling = tryEnumValue(preferences[PreferencesKeys.FONT_SCALING], FontScaling.DEFAULT),
            fontSizeSp = preferences[PreferencesKeys.FONT_SIZE_SP] ?: 16,
            fontWeight = tryEnumValue(preferences[PreferencesKeys.FONT_WEIGHT], FontWeightPreference.NORMAL),
            letterSpacingSp = preferences[PreferencesKeys.LETTER_SPACING] ?: 0f,
            lineHeightMultiplier = preferences[PreferencesKeys.LINE_HEIGHT] ?: 1.2f,
            showLineNumbers = preferences[PreferencesKeys.SHOW_LINE_NUMBERS] ?: true,
            reducedMotion = preferences[PreferencesKeys.REDUCED_MOTION] ?: false,
            showCharactersTab = preferences[PreferencesKeys.SHOW_CHARACTERS_TAB] ?: true,
            showLocationsTab = preferences[PreferencesKeys.SHOW_LOCATIONS_TAB] ?: true,
            showWorkspaceTutorial = preferences[PreferencesKeys.SHOW_WORKSPACE_TUTORIAL] ?: true,
            showEditorTutorial = preferences[PreferencesKeys.SHOW_EDITOR_TUTORIAL] ?: true,
            showHistoryTutorial = preferences[PreferencesKeys.SHOW_HISTORY_TUTORIAL] ?: true
        )
    }

    private inline fun <reified T : Enum<T>> tryEnumValue(name: String?, default: T): T {
        if (name == null) return default
        return try {
            java.lang.Enum.valueOf(T::class.java, name)
        } catch (e: Exception) {
            default
        }
    }

    private fun mapSettingsToPreferences(settings: AppSettings, preferences: MutablePreferences) {
        preferences[PreferencesKeys.THEME] = settings.theme.name
        settings.primaryColorHex?.let { preferences[PreferencesKeys.PRIMARY_COLOR] = it } ?: preferences.remove(PreferencesKeys.PRIMARY_COLOR)
        settings.secondaryColorHex?.let { preferences[PreferencesKeys.SECONDARY_COLOR] = it } ?: preferences.remove(PreferencesKeys.SECONDARY_COLOR)
        settings.accentColorHex?.let { preferences[PreferencesKeys.ACCENT_COLOR] = it } ?: preferences.remove(PreferencesKeys.ACCENT_COLOR)
        preferences[PreferencesKeys.BORDER_RADIUS_FACTOR] = settings.borderRadiusFactor
        preferences[PreferencesKeys.ANIMATION_SPEED_FACTOR] = settings.animationSpeedFactor
        preferences[PreferencesKeys.ANIMATIONS_ENABLED] = settings.animationsEnabled
        preferences[PreferencesKeys.DENSITY] = settings.density.name
        preferences[PreferencesKeys.SIDEBAR_WIDTH] = settings.sidebarWidthDp
        preferences[PreferencesKeys.SIDEBAR_POSITION] = settings.sidebarPosition.name
        preferences[PreferencesKeys.NAVIGATION_STYLE] = settings.navigationStyle.name
        preferences[PreferencesKeys.CARD_STYLE] = settings.cardStyle.name
        preferences[PreferencesKeys.BUTTON_STYLE] = settings.buttonStyle.name
        preferences[PreferencesKeys.SHADOW_INTENSITY] = settings.shadowIntensity
        preferences[PreferencesKeys.TRANSPARENCY_ENABLED] = settings.transparencyEnabled
        preferences[PreferencesKeys.BLUR_INTENSITY] = settings.blurIntensity
        preferences[PreferencesKeys.ICON_PACK] = settings.iconPack.name
        preferences[PreferencesKeys.FONT_FAMILY] = settings.fontFamily.name
        preferences[PreferencesKeys.FONT_SCALING] = settings.fontScaling.name
        preferences[PreferencesKeys.FONT_SIZE_SP] = settings.fontSizeSp
        preferences[PreferencesKeys.FONT_WEIGHT] = settings.fontWeight.name
        preferences[PreferencesKeys.LETTER_SPACING] = settings.letterSpacingSp
        preferences[PreferencesKeys.LINE_HEIGHT] = settings.lineHeightMultiplier
        preferences[PreferencesKeys.SHOW_LINE_NUMBERS] = settings.showLineNumbers
        preferences[PreferencesKeys.REDUCED_MOTION] = settings.reducedMotion
        preferences[PreferencesKeys.SHOW_CHARACTERS_TAB] = settings.showCharactersTab
        preferences[PreferencesKeys.SHOW_LOCATIONS_TAB] = settings.showLocationsTab
        preferences[PreferencesKeys.SHOW_WORKSPACE_TUTORIAL] = settings.showWorkspaceTutorial
        preferences[PreferencesKeys.SHOW_EDITOR_TUTORIAL] = settings.showEditorTutorial
        preferences[PreferencesKeys.SHOW_HISTORY_TUTORIAL] = settings.showHistoryTutorial
    }

    override val settings: Flow<AppSettings> = dataStore.data.map { mapPreferencesToSettings(it) }

    override suspend fun updateTheme(theme: AppTheme) = updateSettings { it.copy(theme = theme) }
    override suspend fun updateFontSize(fontSizeSp: Int) = updateSettings { it.copy(fontSizeSp = fontSizeSp) }
    override suspend fun updateShowLineNumbers(show: Boolean) = updateSettings { it.copy(showLineNumbers = show) }

    override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            val currentSettings = mapPreferencesToSettings(preferences)
            val newSettings = transform(currentSettings)
            mapSettingsToPreferences(newSettings, preferences)
        }
    }
}
