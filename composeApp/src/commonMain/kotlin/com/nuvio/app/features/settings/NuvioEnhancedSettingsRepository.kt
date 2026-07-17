package com.nuvio.app.features.settings

import com.nuvio.app.features.home.HomeReleaseRadarCategory
import com.nuvio.app.features.home.HomeReleaseRadarItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal data class NuvioEnhancedSettingsUiState(
    val enhancedHomeFeaturesEnabled: Boolean = true,
    val nuvioConciergeEnabled: Boolean = true,
    val smartResumeEnabled: Boolean = true,
    val releaseRadarHomeSignalsEnabled: Boolean = true,
    val profileStatsEnabled: Boolean = true,
    val liveTvEnabled: Boolean = true,
    val streamSourcePinningEnabled: Boolean = false,
    val heroDisplayMode: NuvioHeroDisplayMode = NuvioHeroDisplayMode.Balanced,
    val heroArtworkSource: NuvioHeroArtworkSource = NuvioHeroArtworkSource.Backdrop,
    val posterArtHeroEnabled: Boolean = false,
    val streamingShowcaseHeroEnabled: Boolean = false,
    val streamingShowcaseVideoPreviewEnabled: Boolean = true,
    val streamingShowcaseVideoPreviewSoundEnabled: Boolean = true,
    val compactHeroMetadata: Boolean = true,
    val showHeroRatings: Boolean = true,
    val showHeroOverview: Boolean = false,
    val heroRefreshHapticsEnabled: Boolean = true,
    val smartShelvesEnabled: Boolean = false,
    val releaseRadarDigestEnabled: Boolean = false,
    val quietHomeModeEnabled: Boolean = false,
    val libraryHealthEnabled: Boolean = false,
    val statusBarVisible: Boolean = true,
    val playerStatusOverlayEnabled: Boolean = false,
    val playerClockEndTimeEnabled: Boolean = false,
    val showContinueWatchingReadyBadge: Boolean = true,
    val selectedAppIconId: String = NuvioAppIconOption.Default.id,
    val releaseRadarLibraryOnly: Boolean = true,
    val releaseRadarWindowDays: Int = 30,
    val releaseRadarContentFilter: NuvioReleaseRadarContentFilter = NuvioReleaseRadarContentFilter.All,
    val featureHighlightsEnabled: Boolean = true,
    val discordWelcomeSeen: Boolean = false,
    val seenFeatureIds: Set<String> = emptySet(),
) {
    fun isNew(feature: NuvioEnhancedFeature): Boolean =
        featureHighlightsEnabled && feature.id !in seenFeatureIds

    val hasNewFeatures: Boolean
        get() = featureHighlightsEnabled && NuvioEnhancedFeature.entries.any { it.id !in seenFeatureIds }
}

internal enum class NuvioHeroDisplayMode {
    Cinematic,
    Balanced,
    InfoRich,
}

internal enum class NuvioHeroArtworkSource {
    Backdrop,
    Poster,
}

internal enum class NuvioReleaseRadarContentFilter {
    All,
    Episodes,
    Movies,
}

internal enum class NuvioEnhancedFeature(val id: String) {
    HomeExperienceControls("home_experience_controls"),
    SmartResume2("smart_resume_2"),
    BackupImport("backup_import"),
    FeatureHighlights("feature_highlights"),
    LiveTvControls("live_tv_controls"),
    ContactSupport("contact_support"),
    HeroExperienceControls("hero_experience_controls"),
    ReleaseRadarFilters("release_radar_filters"),
    DetailExperienceControls("detail_experience_controls"),
    PlayerStatusOverlay("player_status_overlay"),
    PlayerTimeOverlay("player_time_overlay_v2"),
    StatusBarVisibility("status_bar_visibility"),
    AppIconPicker("app_icon_picker"),
    NetworkControls("network_controls"),
    CommunityLinks("community_links"),
    PremiumLabs("premium_labs"),
    SmartShelfComposer("smart_shelf_composer"),
    ReleaseRadarDigest("release_radar_digest"),
    QuietHomeMode("quiet_home_mode"),
    LibraryHealth("library_health"),
    StreamSourcePinning("stream_source_pinning"),
}

@Serializable
private data class StoredNuvioEnhancedSettings(
    val enhancedHomeFeaturesEnabled: Boolean = true,
    val nuvioConciergeEnabled: Boolean = true,
    val smartResumeEnabled: Boolean = true,
    val releaseRadarHomeSignalsEnabled: Boolean = true,
    val profileStatsEnabled: Boolean = true,
    val liveTvEnabled: Boolean = true,
    val streamSourcePinningEnabled: Boolean = false,
    val heroDisplayMode: NuvioHeroDisplayMode = NuvioHeroDisplayMode.Balanced,
    val heroArtworkSource: NuvioHeroArtworkSource = NuvioHeroArtworkSource.Backdrop,
    val posterArtHeroEnabled: Boolean = false,
    val streamingShowcaseHeroEnabled: Boolean = false,
    val streamingShowcaseVideoPreviewEnabled: Boolean = true,
    val streamingShowcaseVideoPreviewSoundEnabled: Boolean = true,
    val compactHeroMetadata: Boolean = true,
    val showHeroRatings: Boolean = true,
    val showHeroOverview: Boolean = false,
    val heroOverviewUserConfigured: Boolean = false,
    val heroRefreshHapticsEnabled: Boolean = true,
    val smartShelvesEnabled: Boolean = false,
    val releaseRadarDigestEnabled: Boolean = false,
    val quietHomeModeEnabled: Boolean = false,
    val libraryHealthEnabled: Boolean = false,
    val statusBarVisible: Boolean = true,
    val playerStatusOverlayEnabled: Boolean = false,
    val playerClockEndTimeEnabled: Boolean = false,
    val showContinueWatchingReadyBadge: Boolean = true,
    val selectedAppIconId: String = NuvioAppIconOption.Default.id,
    val releaseRadarLibraryOnly: Boolean = true,
    val releaseRadarWindowDays: Int = 30,
    val releaseRadarContentFilter: NuvioReleaseRadarContentFilter = NuvioReleaseRadarContentFilter.All,
    val featureHighlightsEnabled: Boolean = true,
    val discordWelcomeSeen: Boolean = false,
    val seenFeatureIds: Set<String> = emptySet(),
)

internal object NuvioEnhancedSettingsRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _uiState = MutableStateFlow(NuvioEnhancedSettingsUiState())
    val uiState: StateFlow<NuvioEnhancedSettingsUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var stored = StoredNuvioEnhancedSettings()

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        val payload = NuvioEnhancedSettingsStorage.loadPayload().orEmpty().trim()
        stored = if (payload.isNotEmpty()) {
            runCatching { json.decodeFromString<StoredNuvioEnhancedSettings>(payload) }
                .getOrDefault(StoredNuvioEnhancedSettings())
                .let { decoded ->
                    if (decoded.heroOverviewUserConfigured) {
                        decoded
                    } else {
                        decoded.copy(showHeroOverview = false)
                    }
                }
        } else {
            StoredNuvioEnhancedSettings()
        }
        publish()
    }

    fun onProfileChanged() {
        hasLoaded = false
        stored = StoredNuvioEnhancedSettings()
        ensureLoaded()
    }

    fun exportPayload(): String {
        ensureLoaded()
        return json.encodeToString(stored)
    }

    fun replacePayload(payload: String) {
        val decoded = payload
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { json.decodeFromString<StoredNuvioEnhancedSettings>(it) }.getOrNull() }
            ?: return
        stored = decoded
        hasLoaded = true
        publish()
        persist()
    }

    fun setEnhancedHomeFeaturesEnabled(enabled: Boolean) = update {
        copy(enhancedHomeFeaturesEnabled = enabled)
    }

    fun setNuvioConciergeEnabled(enabled: Boolean) = update {
        copy(nuvioConciergeEnabled = enabled)
    }

    fun setSmartResumeEnabled(enabled: Boolean) = update {
        copy(smartResumeEnabled = enabled)
    }

    fun setReleaseRadarHomeSignalsEnabled(enabled: Boolean) = update {
        copy(releaseRadarHomeSignalsEnabled = enabled)
    }

    fun setProfileStatsEnabled(enabled: Boolean) = update {
        copy(profileStatsEnabled = enabled)
    }

    fun setLiveTvEnabled(enabled: Boolean) = update {
        copy(liveTvEnabled = enabled)
    }

    fun setStreamSourcePinningEnabled(enabled: Boolean) = update {
        copy(streamSourcePinningEnabled = enabled)
    }

    fun setHeroDisplayMode(mode: NuvioHeroDisplayMode) = update {
        copy(heroDisplayMode = mode)
    }

    fun setHeroArtworkSource(source: NuvioHeroArtworkSource) = update {
        copy(heroArtworkSource = source)
    }

    fun setPosterArtHeroEnabled(enabled: Boolean) = update {
        copy(
            posterArtHeroEnabled = enabled,
            streamingShowcaseHeroEnabled = if (enabled) false else streamingShowcaseHeroEnabled,
        )
    }

    fun setStreamingShowcaseHeroEnabled(enabled: Boolean) = update {
        copy(
            streamingShowcaseHeroEnabled = enabled,
            posterArtHeroEnabled = if (enabled) false else posterArtHeroEnabled,
        )
    }

    fun setStreamingShowcaseVideoPreviewEnabled(enabled: Boolean) = update {
        copy(streamingShowcaseVideoPreviewEnabled = enabled)
    }

    fun setStreamingShowcaseVideoPreviewSoundEnabled(enabled: Boolean) = update {
        copy(streamingShowcaseVideoPreviewSoundEnabled = enabled)
    }

    fun setCompactHeroMetadata(enabled: Boolean) = update {
        copy(compactHeroMetadata = enabled)
    }

    fun setShowHeroRatings(enabled: Boolean) = update {
        copy(showHeroRatings = enabled)
    }

    fun setShowHeroOverview(enabled: Boolean) = update {
        copy(
            showHeroOverview = enabled,
            heroOverviewUserConfigured = true,
        )
    }

    fun setHeroRefreshHapticsEnabled(enabled: Boolean) = update {
        copy(heroRefreshHapticsEnabled = enabled)
    }

    fun setSmartShelvesEnabled(enabled: Boolean) = update {
        copy(smartShelvesEnabled = enabled)
    }

    fun setReleaseRadarDigestEnabled(enabled: Boolean) = update {
        copy(releaseRadarDigestEnabled = enabled)
    }

    fun setQuietHomeModeEnabled(enabled: Boolean) = update {
        copy(quietHomeModeEnabled = enabled)
    }

    fun setLibraryHealthEnabled(enabled: Boolean) = update {
        copy(libraryHealthEnabled = enabled)
    }

    fun setPlayerStatusOverlayEnabled(enabled: Boolean) = update {
        copy(playerStatusOverlayEnabled = enabled)
    }

    fun setPlayerClockEndTimeEnabled(enabled: Boolean) = update {
        copy(playerClockEndTimeEnabled = enabled)
    }

    fun setStatusBarVisible(visible: Boolean) = update {
        copy(statusBarVisible = visible)
    }

    fun setShowContinueWatchingReadyBadge(enabled: Boolean) = update {
        copy(showContinueWatchingReadyBadge = enabled)
    }

    fun setSelectedAppIcon(option: NuvioAppIconOption): Boolean {
        val applied = NuvioAppIconSwitcher.apply(option.id)
        update {
            copy(selectedAppIconId = option.id)
        }
        return applied
    }

    fun setReleaseRadarLibraryOnly(enabled: Boolean) = update {
        copy(releaseRadarLibraryOnly = enabled)
    }

    fun setReleaseRadarWindowDays(days: Int) = update {
        copy(releaseRadarWindowDays = days.coerceIn(7, 45))
    }

    fun setReleaseRadarContentFilter(filter: NuvioReleaseRadarContentFilter) = update {
        copy(releaseRadarContentFilter = filter)
    }

    fun setFeatureHighlightsEnabled(enabled: Boolean) = update {
        copy(featureHighlightsEnabled = enabled)
    }

    fun markDiscordWelcomeSeen() = update {
        copy(discordWelcomeSeen = true)
    }

    fun markFeatureSeen(feature: NuvioEnhancedFeature) {
        ensureLoaded()
        if (feature.id in stored.seenFeatureIds) return
        stored = stored.copy(seenFeatureIds = stored.seenFeatureIds + feature.id)
        publish()
        persist()
    }

    fun markAllFeaturesSeen() {
        ensureLoaded()
        stored = stored.copy(seenFeatureIds = NuvioEnhancedFeature.entries.mapTo(mutableSetOf()) { it.id })
        publish()
        persist()
    }

    private fun update(transform: StoredNuvioEnhancedSettings.() -> StoredNuvioEnhancedSettings) {
        ensureLoaded()
        val updated = stored.transform()
        if (updated == stored) return
        stored = updated
        publish()
        persist()
    }

    private fun publish() {
        _uiState.value = NuvioEnhancedSettingsUiState(
            enhancedHomeFeaturesEnabled = stored.enhancedHomeFeaturesEnabled,
            nuvioConciergeEnabled = stored.nuvioConciergeEnabled,
            smartResumeEnabled = stored.smartResumeEnabled,
            releaseRadarHomeSignalsEnabled = stored.releaseRadarHomeSignalsEnabled,
            profileStatsEnabled = stored.profileStatsEnabled,
            liveTvEnabled = stored.liveTvEnabled,
            streamSourcePinningEnabled = stored.streamSourcePinningEnabled,
            heroDisplayMode = stored.heroDisplayMode,
            heroArtworkSource = stored.heroArtworkSource,
            posterArtHeroEnabled = stored.posterArtHeroEnabled,
            streamingShowcaseHeroEnabled = stored.streamingShowcaseHeroEnabled,
            streamingShowcaseVideoPreviewEnabled = stored.streamingShowcaseVideoPreviewEnabled,
            streamingShowcaseVideoPreviewSoundEnabled = stored.streamingShowcaseVideoPreviewSoundEnabled,
            compactHeroMetadata = stored.compactHeroMetadata,
            showHeroRatings = stored.showHeroRatings,
            showHeroOverview = stored.showHeroOverview,
            heroRefreshHapticsEnabled = stored.heroRefreshHapticsEnabled,
            smartShelvesEnabled = stored.smartShelvesEnabled,
            releaseRadarDigestEnabled = stored.releaseRadarDigestEnabled,
            quietHomeModeEnabled = stored.quietHomeModeEnabled,
            libraryHealthEnabled = stored.libraryHealthEnabled,
            statusBarVisible = stored.statusBarVisible,
            playerStatusOverlayEnabled = stored.playerStatusOverlayEnabled,
            playerClockEndTimeEnabled = stored.playerClockEndTimeEnabled,
            showContinueWatchingReadyBadge = stored.showContinueWatchingReadyBadge,
            selectedAppIconId = stored.selectedAppIconId,
            releaseRadarLibraryOnly = stored.releaseRadarLibraryOnly,
            releaseRadarWindowDays = stored.releaseRadarWindowDays.coerceIn(7, 45),
            releaseRadarContentFilter = stored.releaseRadarContentFilter,
            featureHighlightsEnabled = stored.featureHighlightsEnabled,
            discordWelcomeSeen = stored.discordWelcomeSeen,
            seenFeatureIds = stored.seenFeatureIds,
        )
    }

    private fun persist() {
        NuvioEnhancedSettingsStorage.savePayload(json.encodeToString(stored))
    }
}

internal fun List<HomeReleaseRadarItem>.filteredByNuvioEnhancedReleaseRadar(
    settings: NuvioEnhancedSettingsUiState,
): List<HomeReleaseRadarItem> =
    asSequence()
        .filter { item ->
            val days = item.daysFromToday ?: return@filter true
            days in 0..settings.releaseRadarWindowDays.coerceIn(7, 45)
        }
        .filter { item ->
            !settings.releaseRadarLibraryOnly ||
                item.category != HomeReleaseRadarCategory.Catalog
        }
        .filter { item ->
            when (settings.releaseRadarContentFilter) {
                NuvioReleaseRadarContentFilter.All -> true
                NuvioReleaseRadarContentFilter.Episodes ->
                    item.category == HomeReleaseRadarCategory.Episode ||
                        item.category == HomeReleaseRadarCategory.NextUp
                NuvioReleaseRadarContentFilter.Movies -> item.category == HomeReleaseRadarCategory.Movie
            }
        }
        .toList()
