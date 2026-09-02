package com.nuvio.app.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.build.TrailerPlaybackMode
import com.nuvio.app.core.diagnostics.CrashDiagnostics
import com.nuvio.app.core.sync.ProfileSettingsSync
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.appIconPainter
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import com.nuvio.app.features.home.CatalogPosterLayout
import com.nuvio.app.features.home.CatalogPosterSize
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.player.AndroidPlaybackEngine
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.isIos
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.*
import nuvio.composeapp.generated.resources.settings_advanced_doh_selected
import nuvio.composeapp.generated.resources.settings_advanced_hero_auto_scroll
import nuvio.composeapp.generated.resources.settings_advanced_hero_auto_scroll_description
import nuvio.composeapp.generated.resources.settings_advanced_hero_motion_preview
import nuvio.composeapp.generated.resources.settings_advanced_hero_motion_preview_description
import nuvio.composeapp.generated.resources.settings_meta_random_episode_button
import nuvio.composeapp.generated.resources.settings_meta_random_episode_button_description
import nuvio.composeapp.generated.resources.settings_meta_show_episode_ratings
import nuvio.composeapp.generated.resources.settings_meta_show_episode_ratings_description
import nuvio.composeapp.generated.resources.settings_nuvio_enhanced_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource

private const val NuvioEnhancedGithubUrl = "https://github.com/yesnt10/NuvioMobile-Enhanced"
private const val NuvioEnhancedDiscordUrl = "https://discord.gg/at8xffxuRU"

internal fun LazyListScope.nuvioEnhancedSettingsContent(
    isTablet: Boolean,
) {
    item {
        NuvioEnhancedSettingsPageContent(isTablet = isTablet)
    }
}

@Composable
private fun NuvioEnhancedSettingsPageContent(
    isTablet: Boolean,
) {
    val settings by remember {
        NuvioEnhancedSettingsRepository.ensureLoaded()
        NuvioEnhancedSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val homeSettings by remember {
        HomeCatalogSettingsRepository.snapshot()
        HomeCatalogSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val detailSettings by remember {
        MetaScreenSettingsRepository.ensureLoaded()
        MetaScreenSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val playerSettings by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val lastCrashReport by remember {
        CrashDiagnostics.lastReport
    }.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    var backupPayload by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showAppIconPicker by remember { mutableStateOf(false) }
    var importPayload by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    val backupImportedMessage = stringResource(Res.string.nuvio_enhanced_toast_backup_imported)
    val invalidBackupPayloadMessage = stringResource(Res.string.nuvio_enhanced_toast_invalid_backup)
    val backupFileReadyMessage = stringResource(Res.string.nuvio_enhanced_toast_backup_ready)
    val backupExportFailedMessage = stringResource(Res.string.nuvio_enhanced_toast_backup_export_failed)
    val backupImportFailedMessage = stringResource(Res.string.nuvio_enhanced_toast_backup_import_failed)
    val backupCopiedMessage = stringResource(Res.string.nuvio_enhanced_toast_backup_copied)
    val crashCopiedMessage = stringResource(Res.string.nuvio_enhanced_toast_crash_copied)
    val homeHeroVideoPreviewSupported = AppFeaturePolicy.heroTrailerPlaybackSupported &&
        AppFeaturePolicy.trailerPlaybackMode == TrailerPlaybackMode.IN_APP
    val detailHeroTrailerPlaybackSupported = AppFeaturePolicy.heroTrailerPlaybackSupported &&
        AppFeaturePolicy.trailerPlaybackMode == TrailerPlaybackMode.IN_APP

    fun isNew(feature: NuvioEnhancedFeature): Boolean = settings.isNew(feature)
    fun markSeen(feature: NuvioEnhancedFeature) {
        NuvioEnhancedSettingsRepository.markFeatureSeen(feature)
    }
    fun importBackupPayload(payload: String) {
        ProfileSettingsSync.importBackupJson(payload)
            .onSuccess {
                NuvioToastController.show(backupImportedMessage)
                showImportDialog = false
                importPayload = ""
                importError = null
            }
            .onFailure { error ->
                importError = error.message ?: invalidBackupPayloadMessage
                NuvioToastController.show(importError ?: invalidBackupPayloadMessage)
            }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.nuvio.spacing.listGap),
    ) {
        SettingsSection(
            title = stringResource(Res.string.settings_nuvio_enhanced_title),
            isTablet = isTablet,
        ) {
            EnhancedIntroCard(
                isTablet = isTablet,
                hasNewFeatures = settings.hasNewFeatures,
                onMarkAllSeen = NuvioEnhancedSettingsRepository::markAllFeaturesSeen,
            )
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_nuvio_experience),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_home_features_title),
                    description = stringResource(Res.string.nuvio_enhanced_home_features_desc),
                    checked = settings.enhancedHomeFeaturesEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        NuvioEnhancedSettingsRepository.setEnhancedHomeFeaturesEnabled(it)
                    },
                )
            }
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_app_experience),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_live_tv_title),
                    description = stringResource(Res.string.nuvio_enhanced_live_tv_desc),
                    checked = settings.liveTvEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.LiveTvControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.LiveTvControls)
                        NuvioEnhancedSettingsRepository.setLiveTvEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_playback_parental_guide),
                    description = stringResource(Res.string.settings_playback_parental_guide_description),
                    checked = playerSettings.showParentalGuide,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.ContentWarnings),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.ContentWarnings)
                        PlayerSettingsRepository.setShowParentalGuide(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_source_pinning_title),
                    description = stringResource(Res.string.nuvio_enhanced_source_pinning_desc),
                    checked = settings.streamSourcePinningEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.StreamSourcePinning),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.StreamSourcePinning)
                        NuvioEnhancedSettingsRepository.setStreamSourcePinningEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_background_stream_prefetch_title),
                    description = stringResource(Res.string.nuvio_enhanced_background_stream_prefetch_desc),
                    checked = settings.backgroundStreamPrefetchEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.BackgroundStreamPrefetch),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.BackgroundStreamPrefetch)
                        NuvioEnhancedSettingsRepository.setBackgroundStreamPrefetchEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_player_status_overlay_title),
                    description = stringResource(Res.string.nuvio_enhanced_player_status_overlay_desc),
                    checked = settings.playerStatusOverlayEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.PlayerStatusOverlay),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.PlayerStatusOverlay)
                        NuvioEnhancedSettingsRepository.setPlayerStatusOverlayEnabled(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_status_bar_title),
                    description = stringResource(Res.string.nuvio_enhanced_status_bar_desc),
                    checked = settings.statusBarVisible,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.StatusBarVisibility),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.StatusBarVisibility)
                        NuvioEnhancedSettingsRepository.setStatusBarVisible(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                val selectedAppIcon = NuvioAppIconOption.entries.firstOrNull { it.id == settings.selectedAppIconId }
                    ?: NuvioAppIconOption.Default
                SettingsNavigationRow(
                    title = stringResource(Res.string.nuvio_enhanced_app_icon_title),
                    description = stringResource(Res.string.nuvio_enhanced_app_icon_desc),
                    iconPainter = appIconPreviewPainter(selectedAppIcon),
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.AppIconPicker),
                    onClick = {
                        markSeen(NuvioEnhancedFeature.AppIconPicker)
                        showAppIconPicker = true
                    },
                )
                if (!isIos) {
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_playback_android_memory_safe_buffer),
                        description = stringResource(Res.string.settings_playback_android_memory_safe_buffer_description),
                        checked = playerSettings.androidMemorySafeBufferEnabled,
                        enabled = !playerSettings.externalPlayerEnabled &&
                            playerSettings.androidPlaybackEngine != AndroidPlaybackEngine.Libmpv,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.PlayerStatusOverlay),
                        onCheckedChange = PlayerSettingsRepository::setAndroidMemorySafeBufferEnabled,
                    )
                }
            }
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_home_catalog),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.layout_catalog_type),
                    description = stringResource(Res.string.layout_catalog_type_sub),
                    checked = homeSettings.showCatalogType,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        HomeCatalogSettingsRepository.setShowCatalogType(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.layout_hide_unreleased),
                    description = stringResource(Res.string.layout_hide_unreleased_sub),
                    checked = homeSettings.hideUnreleasedContent,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        HomeCatalogSettingsRepository.setHideUnreleasedContent(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_homescreen_hide_catalog_underline),
                    description = stringResource(Res.string.settings_homescreen_hide_catalog_underline_description),
                    checked = homeSettings.hideCatalogUnderline,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        HomeCatalogSettingsRepository.setHideCatalogUnderline(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_hide_home_release_dates_title),
                    description = stringResource(Res.string.nuvio_enhanced_hide_home_release_dates_desc),
                    checked = settings.hideHomeReleaseDates,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        NuvioEnhancedSettingsRepository.setHideHomeReleaseDates(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                EnhancedChoiceRow(
                    title = stringResource(Res.string.settings_homescreen_catalog_columns),
                    description = stringResource(Res.string.nuvio_enhanced_catalog_columns_desc),
                    selected = homeSettings.catalogColumnCount,
                    options = listOf(2, 3, 4, 5, 6).map { count ->
                        EnhancedChoiceOption(count, count.toString())
                    },
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onSelected = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        HomeCatalogSettingsRepository.setCatalogColumnCount(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                EnhancedChoiceRow(
                    title = stringResource(Res.string.settings_homescreen_catalog_size),
                    description = stringResource(Res.string.nuvio_enhanced_catalog_size_desc),
                    selected = homeSettings.catalogPosterSize,
                    options = listOf(
                        EnhancedChoiceOption(
                            CatalogPosterSize.Compact,
                            stringResource(Res.string.settings_homescreen_catalog_size_compact),
                        ),
                        EnhancedChoiceOption(
                            CatalogPosterSize.Regular,
                            stringResource(Res.string.settings_homescreen_catalog_size_regular),
                        ),
                        EnhancedChoiceOption(
                            CatalogPosterSize.Large,
                            stringResource(Res.string.settings_homescreen_catalog_size_large),
                        ),
                    ),
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onSelected = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        HomeCatalogSettingsRepository.setCatalogPosterSize(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                EnhancedChoiceRow(
                    title = stringResource(Res.string.settings_homescreen_catalog_layout),
                    description = stringResource(Res.string.nuvio_enhanced_catalog_layout_desc),
                    selected = homeSettings.catalogPosterLayout,
                    options = listOf(
                        EnhancedChoiceOption(
                            CatalogPosterLayout.Portrait,
                            stringResource(Res.string.settings_homescreen_catalog_layout_portrait),
                        ),
                        EnhancedChoiceOption(
                            CatalogPosterLayout.Landscape,
                            stringResource(Res.string.settings_homescreen_catalog_layout_landscape),
                        ),
                    ),
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onSelected = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        HomeCatalogSettingsRepository.setCatalogPosterLayout(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                EnhancedChoiceRow(
                    title = stringResource(Res.string.nuvio_enhanced_home_catalog_rows_title),
                    description = stringResource(Res.string.nuvio_enhanced_home_catalog_rows_desc),
                    selected = homeSettings.homeCatalogRowCount,
                    options = listOf(
                        EnhancedChoiceOption(1, stringResource(Res.string.nuvio_enhanced_home_catalog_rows_option, 1)),
                        EnhancedChoiceOption(2, stringResource(Res.string.nuvio_enhanced_home_catalog_rows_option, 2)),
                        EnhancedChoiceOption(3, stringResource(Res.string.nuvio_enhanced_home_catalog_rows_option, 3)),
                        EnhancedChoiceOption(4, stringResource(Res.string.nuvio_enhanced_home_catalog_rows_option, 4)),
                    ),
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HomeExperienceControls),
                    onSelected = {
                        markSeen(NuvioEnhancedFeature.HomeExperienceControls)
                        HomeCatalogSettingsRepository.setHomeCatalogRowCount(it)
                    },
                )
            }
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_hero_experience),
            isTablet = isTablet,
        ) {
            val infoRichHeroEnabled = settings.heroDisplayMode == NuvioHeroDisplayMode.InfoRich
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_hero_display_title),
                    description = stringResource(Res.string.nuvio_enhanced_hero_display_desc),
                    checked = infoRichHeroEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                    onCheckedChange = { enabled ->
                        markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                        NuvioEnhancedSettingsRepository.setHeroDisplayMode(
                            if (enabled) NuvioHeroDisplayMode.InfoRich else NuvioHeroDisplayMode.Balanced,
                        )
                    },
                )
                if (infoRichHeroEnabled) {
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.nuvio_enhanced_showcase_video_preview_title),
                        description = stringResource(Res.string.nuvio_enhanced_showcase_video_preview_desc),
                        checked = settings.streamingShowcaseVideoPreviewEnabled,
                        enabled = homeHeroVideoPreviewSupported,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                        onCheckedChange = {
                            markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                            NuvioEnhancedSettingsRepository.setStreamingShowcaseVideoPreviewEnabled(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.nuvio_enhanced_showcase_video_preview_sound_title),
                        description = stringResource(Res.string.nuvio_enhanced_showcase_video_preview_sound_desc),
                        checked = settings.streamingShowcaseVideoPreviewSoundEnabled,
                        enabled = settings.streamingShowcaseVideoPreviewEnabled &&
                            homeHeroVideoPreviewSupported,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                        onCheckedChange = {
                            markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                            NuvioEnhancedSettingsRepository.setStreamingShowcaseVideoPreviewSoundEnabled(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.nuvio_enhanced_compact_hero_title),
                        description = stringResource(Res.string.nuvio_enhanced_compact_hero_desc),
                        checked = settings.compactHeroMetadata,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                        onCheckedChange = {
                            markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                            NuvioEnhancedSettingsRepository.setCompactHeroMetadata(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.nuvio_enhanced_hero_ratings_title),
                        description = stringResource(Res.string.nuvio_enhanced_hero_ratings_desc),
                        checked = settings.showHeroRatings,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                        onCheckedChange = {
                            markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                            NuvioEnhancedSettingsRepository.setShowHeroRatings(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.nuvio_enhanced_hero_overview_title),
                        description = stringResource(Res.string.nuvio_enhanced_hero_overview_desc),
                        checked = settings.showHeroOverview,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                        onCheckedChange = {
                            markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                            NuvioEnhancedSettingsRepository.setShowHeroOverview(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.nuvio_enhanced_hero_refresh_haptics_title),
                        description = stringResource(Res.string.nuvio_enhanced_hero_refresh_haptics_desc),
                        checked = settings.heroRefreshHapticsEnabled,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                        onCheckedChange = {
                            markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                            NuvioEnhancedSettingsRepository.setHeroRefreshHapticsEnabled(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_advanced_hero_auto_scroll),
                        description = stringResource(Res.string.settings_advanced_hero_auto_scroll_description),
                        checked = homeSettings.heroAutoScrollEnabled,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                        onCheckedChange = {
                            markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                            HomeCatalogSettingsRepository.setHeroAutoScrollEnabled(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_advanced_hero_motion_preview),
                        description = stringResource(Res.string.settings_advanced_hero_motion_preview_description),
                        checked = homeSettings.heroMotionPreviewEnabled,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.HeroExperienceControls),
                        onCheckedChange = {
                            markSeen(NuvioEnhancedFeature.HeroExperienceControls)
                            HomeCatalogSettingsRepository.setHeroMotionPreviewEnabled(it)
                        },
                    )
                }
            }
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_details_experience),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                if (detailHeroTrailerPlaybackSupported) {
                    SettingsSwitchRow(
                        title = stringResource(Res.string.settings_meta_hero_trailer_playback),
                        description = stringResource(Res.string.settings_meta_hero_trailer_playback_description),
                        checked = detailSettings.heroTrailerPlayback,
                        isTablet = isTablet,
                        highlighted = isNew(NuvioEnhancedFeature.DetailExperienceControls),
                        onCheckedChange = {
                            markSeen(NuvioEnhancedFeature.DetailExperienceControls)
                            MetaScreenSettingsRepository.setHeroTrailerPlayback(it)
                        },
                    )
                    SettingsGroupDivider(isTablet = isTablet)
                }
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_meta_random_episode_button),
                    description = stringResource(Res.string.settings_meta_random_episode_button_description),
                    checked = detailSettings.randomEpisodeButton,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.DetailExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.DetailExperienceControls)
                        MetaScreenSettingsRepository.setRandomEpisodeButton(it)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsSwitchRow(
                    title = stringResource(Res.string.settings_meta_show_episode_ratings),
                    description = stringResource(Res.string.settings_meta_show_episode_ratings_description),
                    checked = detailSettings.showEpisodeRatings,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.DetailExperienceControls),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.DetailExperienceControls)
                        MetaScreenSettingsRepository.setShowEpisodeRatings(it)
                    },
                )
            }
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_backup_import),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.nuvio_enhanced_backup_download_title),
                    description = stringResource(Res.string.nuvio_enhanced_backup_download_desc),
                    icon = Icons.Rounded.Backup,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.BackupImport),
                    onClick = {
                        markSeen(NuvioEnhancedFeature.BackupImport)
                        val payload = ProfileSettingsSync.exportBackupJson()
                        NuvioEnhancedBackupFileBridge.exportBackup(
                            fileName = "nuvio-backup.json",
                            payload = payload,
                        ) { result ->
                            result
                                .onSuccess { NuvioToastController.show(backupFileReadyMessage) }
                                .onFailure { error -> NuvioToastController.show(error.message ?: backupExportFailedMessage) }
                        }
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.nuvio_enhanced_backup_import_file_title),
                    description = stringResource(Res.string.nuvio_enhanced_backup_import_file_desc),
                    icon = Icons.Rounded.Restore,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.BackupImport),
                    onClick = {
                        markSeen(NuvioEnhancedFeature.BackupImport)
                        importError = null
                        NuvioEnhancedBackupFileBridge.importBackup { result ->
                            result
                                .onSuccess(::importBackupPayload)
                                .onFailure { error -> NuvioToastController.show(error.message ?: backupImportFailedMessage) }
                        }
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.nuvio_enhanced_backup_copy_title),
                    description = stringResource(Res.string.nuvio_enhanced_backup_copy_desc),
                    icon = Icons.Rounded.ContentCopy,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.BackupImport),
                    onClick = {
                        markSeen(NuvioEnhancedFeature.BackupImport)
                        val payload = ProfileSettingsSync.exportBackupJson()
                        backupPayload = payload
                        clipboardManager.setText(AnnotatedString(payload))
                        NuvioToastController.show(backupCopiedMessage)
                    },
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.nuvio_enhanced_backup_paste_title),
                    description = stringResource(Res.string.nuvio_enhanced_backup_paste_desc),
                    icon = Icons.Rounded.Download,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.BackupImport),
                    onClick = {
                        markSeen(NuvioEnhancedFeature.BackupImport)
                        importError = null
                        showImportDialog = true
                    },
                )
            }
        }

        SettingsSection(
            title = stringResource(Res.string.nuvio_enhanced_section_discovery),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = stringResource(Res.string.nuvio_enhanced_highlight_title),
                    description = stringResource(Res.string.nuvio_enhanced_highlight_desc),
                    checked = settings.featureHighlightsEnabled,
                    isTablet = isTablet,
                    highlighted = isNew(NuvioEnhancedFeature.FeatureHighlights),
                    onCheckedChange = {
                        markSeen(NuvioEnhancedFeature.FeatureHighlights)
                        NuvioEnhancedSettingsRepository.setFeatureHighlightsEnabled(it)
                    },
                )
            }
        }

        EnhancedCommunityFooter(
            isTablet = isTablet,
            onGithubClick = {
                markSeen(NuvioEnhancedFeature.CommunityLinks)
                uriHandler.openUri(NuvioEnhancedGithubUrl)
            },
            onDiscordClick = {
                markSeen(NuvioEnhancedFeature.CommunityLinks)
                uriHandler.openUri(NuvioEnhancedDiscordUrl)
            },
        )

        if (CrashDiagnostics.reportsSupported) {
            SettingsSection(
                title = stringResource(Res.string.nuvio_enhanced_section_diagnostics),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsNavigationRow(
                        title = stringResource(Res.string.nuvio_enhanced_copy_last_crash_title),
                        description = stringResource(
                            if (lastCrashReport == null) {
                                Res.string.nuvio_enhanced_copy_last_crash_empty_desc
                            } else {
                                Res.string.nuvio_enhanced_copy_last_crash_desc
                            },
                        ),
                        icon = Icons.Rounded.ContentCopy,
                        enabled = lastCrashReport != null,
                        isTablet = isTablet,
                        onClick = {
                            val report = lastCrashReport ?: return@SettingsNavigationRow
                            clipboardManager.setText(AnnotatedString(report.details))
                            NuvioToastController.show(crashCopiedMessage)
                        },
                    )
                }
            }
        }
    }

    if (showAppIconPicker) {
        AppIconPickerDialog(
            selected = settings.selectedAppIconId,
            onDismiss = { showAppIconPicker = false },
            onSelected = { option ->
                val applied = NuvioEnhancedSettingsRepository.setSelectedAppIcon(option)
                showAppIconPicker = false
                if (applied) {
                    NuvioAppIconSwitcher.closeAfterApply()
                }
            },
        )
    }

    backupPayload?.let { payload ->
        BackupPayloadDialog(
            payload = payload,
            onDismiss = { backupPayload = null },
            onCopy = {
                clipboardManager.setText(AnnotatedString(payload))
                NuvioToastController.show(backupCopiedMessage)
            },
        )
    }

    if (showImportDialog) {
        ImportBackupDialog(
            payload = importPayload,
            error = importError,
            onPayloadChange = {
                importPayload = it
                importError = null
            },
            onDismiss = {
                showImportDialog = false
                importPayload = ""
                importError = null
            },
            onImport = {
                importBackupPayload(importPayload)
            },
        )
    }
}

@Composable
private fun AppIconPickerDialog(
    selected: String,
    onDismiss: () -> Unit,
    onSelected: (NuvioAppIconOption) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.nuvio_enhanced_app_icon_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NuvioAppIconOption.entries.forEach { option ->
                    val isSelected = option.id == selected
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(option) },
                        color = if (isSelected) {
                            tokens.colors.accent.copy(alpha = 0.14f)
                        } else {
                            tokens.colors.surfaceCard.copy(alpha = 0.76f)
                        },
                        shape = RoundedCornerShape(NuvioTokens.Radius.lg),
                        border = BorderStroke(
                            tokens.borders.hairline,
                            if (isSelected) {
                                tokens.colors.accent.copy(alpha = 0.82f)
                            } else {
                                tokens.colors.borderSubtle
                            },
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = appIconPreviewPainter(option),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                            )
                            Text(
                                text = appIconLabel(option),
                                style = MaterialTheme.typography.titleSmall,
                                color = tokens.colors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = stringResource(Res.string.settings_advanced_doh_selected),
                                    tint = tokens.colors.accent,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.nuvio_enhanced_cancel))
            }
        },
    )
}

@Composable
private fun appIconLabel(option: NuvioAppIconOption): String = when (option.id) {
    NuvioAppIconOption.Enhanced.id -> stringResource(Res.string.nuvio_enhanced_app_icon_enhanced)
    NuvioAppIconOption.Monochrome.id -> stringResource(Res.string.nuvio_enhanced_app_icon_monochrome)
    NuvioAppIconOption.Neon.id -> stringResource(Res.string.nuvio_enhanced_app_icon_neon)
    NuvioAppIconOption.Gear.id -> stringResource(Res.string.nuvio_enhanced_app_icon_gear)
    NuvioAppIconOption.Chrome.id -> stringResource(Res.string.nuvio_enhanced_app_icon_chrome)
    NuvioAppIconOption.Aurora.id -> stringResource(Res.string.nuvio_enhanced_app_icon_aurora)
    NuvioAppIconOption.Emerald.id -> stringResource(Res.string.nuvio_enhanced_app_icon_emerald)
    else -> stringResource(Res.string.nuvio_enhanced_app_icon_default)
}

@Composable
private fun appIconPreviewPainter(option: NuvioAppIconOption): Painter = painterResource(
    when (option.id) {
        NuvioAppIconOption.Enhanced.id -> Res.drawable.app_icon_enhanced_preview
        NuvioAppIconOption.Monochrome.id -> Res.drawable.app_icon_monochrome_preview
        NuvioAppIconOption.Neon.id -> Res.drawable.app_icon_neon_preview
        NuvioAppIconOption.Gear.id -> Res.drawable.app_icon_gear_preview
        NuvioAppIconOption.Chrome.id -> Res.drawable.app_icon_chrome_preview
        NuvioAppIconOption.Aurora.id -> Res.drawable.app_icon_aurora_preview
        NuvioAppIconOption.Emerald.id -> Res.drawable.app_icon_emerald_preview
        else -> Res.drawable.app_icon_default_preview
    },
)

private data class EnhancedChoiceOption<T>(
    val value: T,
    val label: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> EnhancedChoiceRow(
    title: String,
    description: String,
    selected: T,
    options: List<EnhancedChoiceOption<T>>,
    isTablet: Boolean,
    highlighted: Boolean,
    onSelected: (T) -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    val highlightShape = RoundedCornerShape(if (isTablet) NuvioTokens.Radius.lg else NuvioTokens.Radius.md)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier
                        .background(tokens.colors.accent.copy(alpha = 0.08f), highlightShape)
                        .border(tokens.borders.hairline, tokens.colors.accent.copy(alpha = 0.72f), highlightShape)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (highlighted) {
                    EnhancedNewBadge()
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val isSelected = option.value == selected
                Surface(
                    modifier = Modifier.clickable { onSelected(option.value) },
                    color = if (isSelected) {
                        tokens.colors.accent
                    } else {
                        tokens.colors.surfaceCard.copy(alpha = 0.72f)
                    },
                    contentColor = if (isSelected) {
                        tokens.colors.onAccent
                    } else {
                        tokens.colors.textPrimary
                    },
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(
                        tokens.borders.hairline,
                        if (isSelected) {
                            tokens.colors.accent.copy(alpha = 0.86f)
                        } else {
                            tokens.colors.borderSubtle
                        },
                    ),
                ) {
                    Text(
                        text = option.label,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) tokens.colors.onAccent else tokens.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedNewBadge() {
    val tokens = MaterialTheme.nuvio
    Surface(
        color = tokens.colors.accent.copy(alpha = 0.16f),
        contentColor = tokens.colors.accent,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(tokens.borders.hairline, tokens.colors.accent.copy(alpha = 0.42f)),
    ) {
        Text(
            text = stringResource(Res.string.settings_new_feature_badge),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun EnhancedCommunityFooter(
    isTablet: Boolean,
    onGithubClick: () -> Unit,
    onDiscordClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.colors.surface.copy(alpha = 0.72f),
        shape = if (isTablet) RoundedCornerShape(NuvioTokens.Radius.xl) else tokens.shapes.compactCard,
        border = BorderStroke(tokens.borders.hairline, tokens.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(Res.string.nuvio_enhanced_footer_title),
                style = MaterialTheme.typography.titleMedium,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.nuvio_enhanced_footer_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EnhancedFooterLink(
                    title = stringResource(Res.string.nuvio_enhanced_footer_github),
                    subtitle = stringResource(Res.string.nuvio_enhanced_footer_releases),
                    icon = appIconPainter(AppIconResource.GithubMark),
                    modifier = Modifier.weight(1f),
                    onClick = onGithubClick,
                )
                EnhancedFooterLink(
                    title = stringResource(Res.string.nuvio_enhanced_footer_discord),
                    subtitle = stringResource(Res.string.nuvio_enhanced_footer_community),
                    icon = appIconPainter(AppIconResource.DiscordMark),
                    modifier = Modifier.weight(1f),
                    onClick = onDiscordClick,
                )
            }
        }
    }
}

@Composable
private fun EnhancedFooterLink(
    title: String,
    subtitle: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick),
        color = tokens.colors.accent.copy(alpha = 0.10f),
        shape = RoundedCornerShape(NuvioTokens.Radius.lg),
        border = BorderStroke(tokens.borders.hairline, tokens.colors.accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                color = tokens.colors.accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(NuvioTokens.Radius.md),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = title,
                        tint = tokens.colors.accent,
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = tokens.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EnhancedIntroCard(
    isTablet: Boolean,
    hasNewFeatures: Boolean,
    onMarkAllSeen: () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = tokens.colors.surface,
        shape = if (isTablet) RoundedCornerShape(NuvioTokens.Radius.xl) else tokens.shapes.compactCard,
        border = BorderStroke(tokens.borders.hairline, tokens.colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier.padding(if (isTablet) 20.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = tokens.colors.accent,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.nuvio_enhanced_intro_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.nuvio_enhanced_intro_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = tokens.colors.textMuted,
                    )
                }
            }
            if (hasNewFeatures) {
                OutlinedButton(onClick = onMarkAllSeen) {
                    Icon(
                        imageVector = Icons.Rounded.NewReleases,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(stringResource(Res.string.nuvio_enhanced_mark_all_seen))
                }
            }
        }
    }
}

@Composable
private fun BackupPayloadDialog(
    payload: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.nuvio_enhanced_backup_ready_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(Res.string.nuvio_enhanced_backup_ready_desc))
                OutlinedTextField(
                    value = payload,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    maxLines = 8,
                )
            }
        },
        confirmButton = {
            Button(onClick = onCopy) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(stringResource(Res.string.nuvio_enhanced_backup_copy_again))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.nuvio_enhanced_close))
            }
        },
    )
}

@Composable
private fun ImportBackupDialog(
    payload: String,
    error: String?,
    onPayloadChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.nuvio_enhanced_import_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(stringResource(Res.string.nuvio_enhanced_import_desc))
                OutlinedTextField(
                    value = payload,
                    onValueChange = onPayloadChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                    placeholder = { Text(stringResource(Res.string.nuvio_enhanced_import_placeholder)) },
                    maxLines = 10,
                )
                if (!error.isNullOrBlank()) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onImport,
                enabled = payload.isNotBlank(),
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(stringResource(Res.string.nuvio_enhanced_import_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.nuvio_enhanced_cancel))
            }
        },
    )
}
