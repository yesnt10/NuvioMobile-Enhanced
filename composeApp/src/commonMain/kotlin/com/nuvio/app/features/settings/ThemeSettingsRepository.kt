package com.nuvio.app.features.settings

import com.nuvio.app.core.ui.AppTheme
import com.nuvio.app.core.ui.NativeTabBridge
import com.nuvio.app.core.ui.ThemeColors
import com.nuvio.app.core.ui.ThemeAccentColor
import com.nuvio.app.core.ui.toThemeColor
import com.nuvio.app.core.ui.toThemeHex
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeSettingsRepository {
    private val _selectedTheme = MutableStateFlow(AppTheme.WHITE)
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme.asStateFlow()

    private val _customThemeFirstColor = MutableStateFlow(ThemeAccentColor.PINK.color)
    val customThemeFirstColor: StateFlow<Color> = _customThemeFirstColor.asStateFlow()

    private val _customThemeSecondColor = MutableStateFlow(ThemeAccentColor.CYAN.color)
    val customThemeSecondColor: StateFlow<Color> = _customThemeSecondColor.asStateFlow()

    private val _amoledEnabled = MutableStateFlow(false)
    val amoledEnabled: StateFlow<Boolean> = _amoledEnabled.asStateFlow()

    private val _liquidGlassNativeTabBarEnabled = MutableStateFlow(false)
    val liquidGlassNativeTabBarEnabled: StateFlow<Boolean> = _liquidGlassNativeTabBarEnabled.asStateFlow()

    private val _liquidGlassAutoHideOnScrollEnabled = MutableStateFlow(false)
    val liquidGlassAutoHideOnScrollEnabled: StateFlow<Boolean> = _liquidGlassAutoHideOnScrollEnabled.asStateFlow()

    private val _selectedAppLanguage = MutableStateFlow(AppLanguage.DEVICE)
    val selectedAppLanguage: StateFlow<AppLanguage> = _selectedAppLanguage.asStateFlow()

    private val _navBarStyle = MutableStateFlow(NavBarStyle.ADAPTIVE)
    val navBarStyle: StateFlow<NavBarStyle> = _navBarStyle.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        _selectedTheme.value = AppTheme.WHITE
        _customThemeFirstColor.value = ThemeAccentColor.PINK.color
        _customThemeSecondColor.value = ThemeAccentColor.CYAN.color
        _amoledEnabled.value = false
        _liquidGlassNativeTabBarEnabled.value = false
        _liquidGlassAutoHideOnScrollEnabled.value = false
        NativeTabBridge.publishAccentColor(AppTheme.WHITE.nativeTabAccentHex())
        NativeTabBridge.publishLiquidGlassEnabled(false)
        _selectedAppLanguage.value = AppLanguage.DEVICE
        _navBarStyle.value = NavBarStyle.ADAPTIVE
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val stored = ThemeSettingsStorage.loadSelectedTheme()
        val theme = stored.toAppTheme()
        _selectedTheme.value = theme
        _customThemeFirstColor.value = ThemeSettingsStorage.loadCustomThemeFirstColor()
            .toThemeColor(ThemeAccentColor.PINK.color)
        _customThemeSecondColor.value = ThemeSettingsStorage.loadCustomThemeSecondColor()
            .toThemeColor(ThemeAccentColor.CYAN.color)
        NativeTabBridge.publishAccentColor(theme.nativeTabAccentHex(_customThemeFirstColor.value))
        _amoledEnabled.value = ThemeSettingsStorage.loadAmoledEnabled() ?: false
        val liquidGlassEnabled = ThemeSettingsStorage.loadLiquidGlassNativeTabBarEnabled() ?: false
        _liquidGlassNativeTabBarEnabled.value = liquidGlassEnabled
        _liquidGlassAutoHideOnScrollEnabled.value =
            ThemeSettingsStorage.loadLiquidGlassAutoHideOnScrollEnabled() ?: false
        NativeTabBridge.publishLiquidGlassEnabled(liquidGlassEnabled)
        val appLanguage = AppLanguage.fromCode(ThemeSettingsStorage.loadSelectedAppLanguage())
        ThemeSettingsStorage.applySelectedAppLanguage(appLanguage.code)
        _selectedAppLanguage.value = appLanguage
        _navBarStyle.value = NavBarStyle.fromKey(ThemeSettingsStorage.loadNavBarStyle())
    }

    fun setTheme(theme: AppTheme) {
        ensureLoaded()
        if (_selectedTheme.value == theme) return
        _selectedTheme.value = theme
        ThemeSettingsStorage.saveSelectedTheme(theme.name)
        NativeTabBridge.publishAccentColor(theme.nativeTabAccentHex(_customThemeFirstColor.value))
    }

    fun setCustomThemeFirstColor(color: Color) {
        ensureLoaded()
        if (_customThemeFirstColor.value == color) return
        _customThemeFirstColor.value = color
        ThemeSettingsStorage.saveCustomThemeFirstColor(color.toThemeHex())
        if (_selectedTheme.value == AppTheme.CUSTOM) {
            NativeTabBridge.publishAccentColor(AppTheme.CUSTOM.nativeTabAccentHex(color))
        }
    }

    fun setCustomThemeSecondColor(color: Color) {
        ensureLoaded()
        if (_customThemeSecondColor.value == color) return
        _customThemeSecondColor.value = color
        ThemeSettingsStorage.saveCustomThemeSecondColor(color.toThemeHex())
    }

    fun setAmoled(enabled: Boolean) {
        ensureLoaded()
        if (_amoledEnabled.value == enabled) return
        _amoledEnabled.value = enabled
        ThemeSettingsStorage.saveAmoledEnabled(enabled)
    }

    fun setLiquidGlassNativeTabBar(enabled: Boolean) {
        ensureLoaded()
        if (_liquidGlassNativeTabBarEnabled.value == enabled) return
        _liquidGlassNativeTabBarEnabled.value = enabled
        ThemeSettingsStorage.saveLiquidGlassNativeTabBarEnabled(enabled)
        NativeTabBridge.publishLiquidGlassEnabled(enabled)
    }

    fun setLiquidGlassAutoHideOnScroll(enabled: Boolean) {
        ensureLoaded()
        if (_liquidGlassAutoHideOnScrollEnabled.value == enabled) return
        _liquidGlassAutoHideOnScrollEnabled.value = enabled
        ThemeSettingsStorage.saveLiquidGlassAutoHideOnScrollEnabled(enabled)
    }

    fun setAppLanguage(language: AppLanguage) {
        ensureLoaded()
        if (_selectedAppLanguage.value == language) return
        ThemeSettingsStorage.saveSelectedAppLanguage(language.code)
        ThemeSettingsStorage.applySelectedAppLanguage(language.code)
        _selectedAppLanguage.value = language
    }

    fun setNavBarStyle(style: NavBarStyle) {
        ensureLoaded()
        if (_navBarStyle.value == style) return
        _navBarStyle.value = style
        ThemeSettingsStorage.saveNavBarStyle(style.key)
    }
}

private fun AppTheme.nativeTabAccentHex(customFirst: Color = ThemeAccentColor.PINK.color): String =
    ThemeColors.getColorPalette(this, customFirst = customFirst).nativeAccentHex

private fun String?.toAppTheme(): AppTheme = when (this) {
    "AURORA", "LAVENDER" -> AppTheme.MESSENGER
    "PRISM" -> AppTheme.AMETHYST
    "NEBULA", "ORCHID" -> AppTheme.BLOSSOM
    "OPAL", "TWILIGHT" -> AppTheme.LAGOON
    "ULTRAVIOLET" -> AppTheme.SUNSET
    else -> this?.let { stored -> AppTheme.entries.firstOrNull { it.name == stored } } ?: AppTheme.WHITE
}
