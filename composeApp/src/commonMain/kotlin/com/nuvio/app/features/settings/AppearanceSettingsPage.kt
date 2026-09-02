package com.nuvio.app.features.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.toThemeHex
import kotlin.math.max
import kotlin.math.min
import com.nuvio.app.core.ui.AppTheme
import com.nuvio.app.core.ui.NuvioBottomSheetActionRow
import com.nuvio.app.core.ui.NuvioBottomSheetDivider
import com.nuvio.app.core.ui.NuvioModalBottomSheet
import com.nuvio.app.core.ui.dismissNuvioBottomSheet
import com.nuvio.app.core.ui.labelRes
import com.nuvio.app.core.ui.ThemeColors
import com.nuvio.app.core.ui.ThemeAccentColor
import com.nuvio.app.core.ui.isEnhanced
import com.nuvio.app.core.ui.rememberAnimatedAccentBrush
import com.nuvio.app.isIos
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.cd_selected
import nuvio.composeapp.generated.resources.collections_header
import nuvio.composeapp.generated.resources.compose_settings_page_continue_watching
import nuvio.composeapp.generated.resources.compose_settings_page_homescreen
import nuvio.composeapp.generated.resources.compose_settings_page_meta_screen
import nuvio.composeapp.generated.resources.compose_settings_page_poster_customization
import nuvio.composeapp.generated.resources.compose_settings_page_streams
import nuvio.composeapp.generated.resources.settings_appearance_app_language
import nuvio.composeapp.generated.resources.settings_appearance_app_language_sheet_title
import nuvio.composeapp.generated.resources.settings_appearance_amoled_black
import nuvio.composeapp.generated.resources.settings_appearance_amoled_description
import nuvio.composeapp.generated.resources.settings_appearance_continue_watching_description
import nuvio.composeapp.generated.resources.settings_appearance_liquid_glass
import nuvio.composeapp.generated.resources.settings_appearance_liquid_glass_auto_hide
import nuvio.composeapp.generated.resources.settings_appearance_liquid_glass_auto_hide_description
import nuvio.composeapp.generated.resources.settings_appearance_liquid_glass_description
import nuvio.composeapp.generated.resources.settings_appearance_nav_bar_style
import nuvio.composeapp.generated.resources.settings_appearance_nav_bar_style_sheet_title
import nuvio.composeapp.generated.resources.settings_appearance_poster_customization_description
import nuvio.composeapp.generated.resources.settings_appearance_section_detail_page
import nuvio.composeapp.generated.resources.settings_appearance_section_display
import nuvio.composeapp.generated.resources.settings_appearance_section_home
import nuvio.composeapp.generated.resources.settings_appearance_section_streams
import nuvio.composeapp.generated.resources.settings_appearance_section_theme
import nuvio.composeapp.generated.resources.settings_appearance_theme_classic
import nuvio.composeapp.generated.resources.settings_appearance_theme_custom_first
import nuvio.composeapp.generated.resources.settings_appearance_theme_custom_second
import nuvio.composeapp.generated.resources.settings_appearance_theme_enhanced
import nuvio.composeapp.generated.resources.settings_content_discovery_collections_description
import nuvio.composeapp.generated.resources.settings_content_discovery_homescreen_description
import nuvio.composeapp.generated.resources.settings_content_discovery_meta_screen_description
import nuvio.composeapp.generated.resources.compose_settings_root_streams_description
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState

internal fun LazyListScope.appearanceSettingsContent(
    isTablet: Boolean,
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    amoledEnabled: Boolean,
    onAmoledToggle: (Boolean) -> Unit,
    liquidGlassNativeTabBarSupported: Boolean,
    liquidGlassNativeTabBarEnabled: Boolean,
    onLiquidGlassNativeTabBarToggle: (Boolean) -> Unit,
    liquidGlassAutoHideOnScrollEnabled: Boolean,
    onLiquidGlassAutoHideOnScrollToggle: (Boolean) -> Unit,
    selectedAppLanguage: AppLanguage,
    onAppLanguageSelected: (AppLanguage) -> Unit,
    selectedNavBarStyle: NavBarStyle,
    onNavBarStyleSelected: (NavBarStyle) -> Unit,
    onHomescreenClick: () -> Unit,
    onMetaScreenClick: () -> Unit,
    onStreamsClick: () -> Unit,
    onCollectionsClick: () -> Unit,
    onContinueWatchingClick: () -> Unit,
    onPosterCustomizationClick: () -> Unit,
) {
    item {
        val customFirst by remember { ThemeSettingsRepository.customThemeFirstColor }.collectAsState()
        val customSecond by remember { ThemeSettingsRepository.customThemeSecondColor }.collectAsState()
        SettingsSection(
            title = stringResource(Res.string.settings_appearance_section_theme),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                val horizontalPadding = if (isTablet) 20.dp else 16.dp
                val verticalPadding = if (isTablet) 18.dp else 14.dp
                val themeSpacing = if (isTablet) 16.dp else 12.dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = horizontalPadding,
                            vertical = verticalPadding,
                        ),
                    verticalArrangement = Arrangement.spacedBy(themeSpacing),
                ) {
                    ThemeSectionLabel(stringResource(Res.string.settings_appearance_theme_classic))
                    ThemeGrid(
                        themes = listOf(
                            AppTheme.WHITE,
                            AppTheme.CRIMSON,
                            AppTheme.OCEAN,
                            AppTheme.VIOLET,
                            AppTheme.EMERALD,
                            AppTheme.AMBER,
                            AppTheme.ROSE,
                        ),
                        selectedTheme = selectedTheme,
                        customFirst = customFirst,
                        customSecond = customSecond,
                        isTablet = isTablet,
                        spacing = themeSpacing,
                        onThemeSelected = onThemeSelected,
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f))

                    ThemeSectionLabel(stringResource(Res.string.settings_appearance_theme_enhanced))
                    ThemeGrid(
                        themes = listOf(
                            AppTheme.MESSENGER,
                            AppTheme.AMETHYST,
                            AppTheme.BLOSSOM,
                            AppTheme.LAGOON,
                            AppTheme.SUNSET,
                            AppTheme.CUSTOM,
                        ),
                        selectedTheme = selectedTheme,
                        customFirst = customFirst,
                        customSecond = customSecond,
                        isTablet = isTablet,
                        spacing = themeSpacing,
                        onThemeSelected = onThemeSelected,
                    )

                    if (selectedTheme == AppTheme.CUSTOM) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f))
                        CustomThemeColorPicker(
                            label = stringResource(Res.string.settings_appearance_theme_custom_first),
                            selectedColor = customFirst,
                            onColorSelected = ThemeSettingsRepository::setCustomThemeFirstColor,
                        )
                        CustomThemeColorPicker(
                            label = stringResource(Res.string.settings_appearance_theme_custom_second),
                            selectedColor = customSecond,
                            onColorSelected = ThemeSettingsRepository::setCustomThemeSecondColor,
                        )
                    }
                }
            }
        }
    }
    item {
        var showLanguageSheet by remember { mutableStateOf(false) }
        var showNavBarStyleSheet by remember { mutableStateOf(false) }
        SettingsSection(
            title = stringResource(Res.string.settings_appearance_section_display),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_appearance_amoled_black),
                    description = stringResource(Res.string.settings_appearance_amoled_description),
                    checked = amoledEnabled,
                    isTablet = isTablet,
                    onCheckedChange = onAmoledToggle,
                )
                if (liquidGlassNativeTabBarSupported) {
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_appearance_liquid_glass),
                        description = stringResource(Res.string.settings_appearance_liquid_glass_description),
                        checked = liquidGlassNativeTabBarEnabled,
                        isTablet = isTablet,
                        onCheckedChange = onLiquidGlassNativeTabBarToggle,
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_appearance_liquid_glass_auto_hide),
                        description = stringResource(Res.string.settings_appearance_liquid_glass_auto_hide_description),
                        checked = liquidGlassAutoHideOnScrollEnabled,
                        enabled = liquidGlassNativeTabBarEnabled,
                        isTablet = isTablet,
                        onCheckedChange = onLiquidGlassAutoHideOnScrollToggle,
                    )
                }
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.settings_appearance_app_language),
                    description = stringResource(selectedAppLanguage.labelRes),
                    isTablet = isTablet,
                    onClick = { showLanguageSheet = true },
                )
                if (!isIos) {
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsNavigationRow(
                        title = stringResource(Res.string.settings_appearance_nav_bar_style),
                        description = stringResource(selectedNavBarStyle.labelRes),
                        isTablet = isTablet,
                        onClick = { showNavBarStyleSheet = true },
                    )
                }
            }
        }

        if (showLanguageSheet) {
            AppearanceLanguageBottomSheet(
                selectedLanguage = selectedAppLanguage,
                onLanguageSelected = {
                    onAppLanguageSelected(it)
                    showLanguageSheet = false
                },
                onDismiss = { showLanguageSheet = false },
            )
        }

        if (showNavBarStyleSheet) {
            NavBarStyleBottomSheet(
                selectedStyle = selectedNavBarStyle,
                onStyleSelected = {
                    onNavBarStyleSelected(it)
                    showNavBarStyleSheet = false
                },
                onDismiss = { showNavBarStyleSheet = false },
            )
        }
    }

    item {
        SettingsSection(
            title = stringResource(Res.string.settings_appearance_section_home),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_homescreen),
                    description = stringResource(Res.string.settings_content_discovery_homescreen_description),
                    isTablet = isTablet,
                    onClick = onHomescreenClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.collections_header),
                    description = stringResource(Res.string.settings_content_discovery_collections_description),
                    isTablet = isTablet,
                    onClick = onCollectionsClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_continue_watching),
                    description = stringResource(Res.string.settings_appearance_continue_watching_description),
                    isTablet = isTablet,
                    onClick = onContinueWatchingClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_poster_customization),
                    description = stringResource(Res.string.settings_appearance_poster_customization_description),
                    isTablet = isTablet,
                    onClick = onPosterCustomizationClick,
                )
            }
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_appearance_section_streams),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_streams),
                    description = stringResource(Res.string.compose_settings_root_streams_description),
                    isTablet = isTablet,
                    onClick = onStreamsClick,
                )
            }
        }
    }
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_appearance_section_detail_page),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_meta_screen),
                    description = stringResource(Res.string.settings_content_discovery_meta_screen_description),
                    isTablet = isTablet,
                    onClick = onMetaScreenClick,
                )
            }
        }
    }
}

private data class AppLanguageSheetOption(
    val language: AppLanguage,
    val labelRes: StringResource,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceLanguageBottomSheet(
    selectedLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val options = remember {
        AppLanguage.entries.map { language ->
            AppLanguageSheetOption(
                language = language,
                labelRes = language.labelRes,
            )
        }
    }

    NuvioModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch {
                dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
            }
        },
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.settings_appearance_app_language_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }

            itemsIndexed(options) { index, option ->
                if (index > 0) {
                    NuvioBottomSheetDivider()
                }
                NuvioBottomSheetActionRow(
                    title = stringResource(option.labelRes),
                    onClick = {
                        onLanguageSelected(option.language)
                        coroutineScope.launch {
                            dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                        }
                    },
                    trailingContent = {
                        if (option.language == selectedLanguage) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(Res.string.cd_selected),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavBarStyleBottomSheet(
    selectedStyle: NavBarStyle,
    onStyleSelected: (NavBarStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    NuvioModalBottomSheet(
        onDismissRequest = {
            coroutineScope.launch {
                dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
            }
        },
        sheetState = sheetState,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.settings_appearance_nav_bar_style_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }

            itemsIndexed(NavBarStyle.entries.toList()) { index, style ->
                if (index > 0) {
                    NuvioBottomSheetDivider()
                }
                NuvioBottomSheetActionRow(
                    title = stringResource(style.labelRes),
                    onClick = {
                        onStyleSelected(style)
                        coroutineScope.launch {
                            dismissNuvioBottomSheet(sheetState = sheetState, onDismiss = onDismiss)
                        }
                    },
                    trailingContent = {
                        if (style == selectedStyle) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = stringResource(Res.string.cd_selected),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeChip(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    customFirst: Color = ThemeAccentColor.PINK.color,
    customSecond: Color = ThemeAccentColor.CYAN.color,
) {
    val palette = ThemeColors.getColorPalette(theme, customFirst, customSecond)
    val previewBrush = theme.previewBrush(palette, customFirst, customSecond)
    val selectedPreviewBrush = rememberAnimatedAccentBrush()
        .takeIf { isSelected && theme.isEnhanced }
    val checkColor = if (theme.isEnhanced) Color.White else palette.onSecondary

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 1.5.dp,
                            color = palette.focusRing,
                            shape = RoundedCornerShape(14.dp),
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (theme == AppTheme.CUSTOM) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(customThemeRingBrush())
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(Res.string.cd_selected),
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(previewBrush),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(Res.string.cd_selected),
                            tint = checkColor,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(theme.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (theme.isEnhanced) {
                        selectedPreviewBrush ?: previewBrush
                    } else {
                        Brush.linearGradient(listOf(palette.focusRing, palette.secondary))
                    },
                ),
        )
    }
}

private fun AppTheme.previewBrush(
    palette: com.nuvio.app.core.ui.ThemeColorPalette,
    customFirst: Color,
    customSecond: Color,
): Brush {
    if (!isEnhanced) {
        return Brush.linearGradient(listOf(palette.secondary, palette.focusRing))
    }

    val colors = ThemeColors.animatedColors(this, customFirst, customSecond)
        .takeIf { it.isNotEmpty() }
        ?: listOf(palette.secondary, palette.secondaryVariant, palette.focusRing)
    return Brush.sweepGradient(colors + colors.first())
}

private fun customThemeRingBrush(): Brush =
    Brush.sweepGradient(
        listOf(
            Color(0xFFFF004D),
            Color(0xFFFFD23F),
            Color(0xFF00E676),
            Color(0xFF00D9FF),
            Color(0xFF3D5AFE),
            Color(0xFFFF4FD8),
            Color(0xFFFF004D),
        ),
    )

@Composable
private fun ThemeSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ThemeGrid(
    themes: List<AppTheme>,
    selectedTheme: AppTheme,
    customFirst: Color,
    customSecond: Color,
    isTablet: Boolean,
    spacing: androidx.compose.ui.unit.Dp,
    onThemeSelected: (AppTheme) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val preferredColumns = if (isTablet) 4 else 3
        val minThemeCellWidth = if (isTablet) 92.dp else 78.dp
        val columns = ((maxWidth + spacing) / (minThemeCellWidth + spacing))
            .toInt()
            .coerceAtLeast(1)
            .coerceAtMost(preferredColumns)

        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            themes.chunked(columns).forEach { rowThemes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    rowThemes.forEach { theme ->
                        ThemeChip(
                            theme = theme,
                            isSelected = theme == selectedTheme,
                            customFirst = customFirst,
                            customSecond = customSecond,
                            onClick = { onThemeSelected(theme) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(columns - rowThemes.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomThemeColorPicker(
    label: String,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var hexValue by remember(selectedColor) { mutableStateOf(selectedColor.toThemeHex()) }
    var hsv by remember(selectedColor) { mutableStateOf(selectedColor.toHsv()) }
    val pickerShape = RoundedCornerShape(8.dp)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(pickerShape)
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = selectedColor.toThemeHex(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(selectedColor),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ThemeAccentColor.entries.forEach { option ->
                val colorName = stringResource(option.labelRes)
                val selected = option.color == selectedColor
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .semantics { contentDescription = colorName }
                        .then(
                            if (selected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            },
                        )
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(option.color)
                        .clickable { onColorSelected(option.color) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (option.color.luminance() > 0.55f) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(pickerShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SaturationValuePicker(
                    hsv = hsv,
                    onHsvChanged = { updated ->
                        hsv = updated
                        onColorSelected(updated.toColor())
                    },
                )
                HuePicker(
                    hue = hsv.hue,
                    onHueChanged = { hue ->
                        val updated = hsv.copy(hue = hue)
                        hsv = updated
                        onColorSelected(updated.toColor())
                    },
                )
                OutlinedTextField(
                    value = hexValue,
                    onValueChange = { input ->
                        val normalized = input.take(7).uppercase()
                        hexValue = normalized
                        normalized.toColorOrNull()?.let(onColorSelected)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("#RRGGBB") },
                )
            }
        }
    }
}

@Composable
private fun SaturationValuePicker(
    hsv: HsvColor,
    onHsvChanged: (HsvColor) -> Unit,
) {
    fun update(position: androidx.compose.ui.geometry.Offset, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        onHsvChanged(
            hsv.copy(
                saturation = (position.x / width).coerceIn(0f, 1f),
                value = (1f - position.y / height).coerceIn(0f, 1f),
            ),
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(6.dp))
            .pointerInput(hsv.hue) {
                detectTapGestures { update(it, size.width.toFloat(), size.height.toFloat()) }
            }
            .pointerInput(hsv.hue) {
                detectDragGestures(
                    onDragStart = { update(it, size.width.toFloat(), size.height.toFloat()) },
                    onDrag = { change, _ ->
                        update(change.position, size.width.toFloat(), size.height.toFloat())
                    },
                )
            },
    ) {
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(Color.White, HsvColor(hsv.hue, 1f, 1f).toColor()),
            ),
        )
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        drawCircle(
            color = Color.Black.copy(alpha = 0.62f),
            radius = 8.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(
                hsv.saturation * size.width,
                (1f - hsv.value) * size.height,
            ),
        )
        drawCircle(
            color = Color.White,
            radius = 7.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(
                hsv.saturation * size.width,
                (1f - hsv.value) * size.height,
            ),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

@Composable
private fun HuePicker(
    hue: Float,
    onHueChanged: (Float) -> Unit,
) {
    fun update(x: Float, width: Float) {
        if (width <= 0f) return
        onHueChanged((x / width).coerceIn(0f, 1f) * 360f)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(RoundedCornerShape(6.dp))
            .pointerInput(Unit) {
                detectTapGestures { update(it.x, size.width.toFloat()) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { update(it.x, size.width.toFloat()) },
                    onDrag = { change, _ -> update(change.position.x, size.width.toFloat()) },
                )
            },
    ) {
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red,
                ),
            ),
        )
        val markerX = (hue / 360f).coerceIn(0f, 1f) * size.width
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(markerX, size.height / 2f),
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}

private data class HsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float,
) {
    fun toColor(): Color {
        val normalizedHue = ((hue % 360f) + 360f) % 360f
        val chroma = value * saturation
        val section = normalizedHue / 60f
        val secondary = chroma * (1f - kotlin.math.abs(section % 2f - 1f))
        val match = value - chroma
        val (red, green, blue) = when (section.toInt()) {
            0 -> Triple(chroma, secondary, 0f)
            1 -> Triple(secondary, chroma, 0f)
            2 -> Triple(0f, chroma, secondary)
            3 -> Triple(0f, secondary, chroma)
            4 -> Triple(secondary, 0f, chroma)
            else -> Triple(chroma, 0f, secondary)
        }
        return Color(red + match, green + match, blue + match)
    }
}

private fun Color.toHsv(): HsvColor {
    val highest = max(red, max(green, blue))
    val lowest = min(red, min(green, blue))
    val delta = highest - lowest
    val hue = when {
        delta == 0f -> 0f
        highest == red -> 60f * (((green - blue) / delta) % 6f)
        highest == green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }
    return HsvColor(
        hue = if (hue < 0f) hue + 360f else hue,
        saturation = if (highest == 0f) 0f else delta / highest,
        value = highest,
    )
}

private fun String.toColorOrNull(): Color? {
    val hex = trim().removePrefix("#")
    if (hex.length != 6 || hex.any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) return null
    return Color(
        red = hex.substring(0, 2).toInt(16) / 255f,
        green = hex.substring(2, 4).toInt(16) / 255f,
        blue = hex.substring(4, 6).toInt(16) / 255f,
    )
}
