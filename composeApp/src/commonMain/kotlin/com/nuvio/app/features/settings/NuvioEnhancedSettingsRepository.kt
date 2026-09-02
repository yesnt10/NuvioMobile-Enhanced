package com.nuvio.app.features.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal data class NuvioEnhancedSettingsUiState(
    val enhancedHomeFeaturesEnabled: Boolean = true,
    val liveTvEnabled: Boolean = true,
    val streamSourcePinningEnabled: Boolean = false,
    val backgroundStreamPrefetchEnabled: Boolean = false,
    val heroDisplayMode: NuvioHeroDisplayMode = NuvioHeroDisplayMode.Balanced,
    val heroArtworkSource: NuvioHeroArtworkSource = NuvioHeroArtworkSource.Backdrop,
    val posterArtHeroEnabled: Boolean = false,
    val streamingShowcaseHeroEnabled: Boolean = false,
    val streamingShowcaseVideoPreviewEnabled: Boolean = true,
    val streamingShowcaseVideoPreviewSoundEnabled: Boolean = true,
    val compactHeroMetadata: Boolean = true,
    val showHeroRatings: Boolean = true,
    val showHeroOverview: Boolean = true,
    val hideHomeReleaseDates: Boolean = false,
    val heroRefreshHapticsEnabled: Boolean = true,
    val statusBarVisible: Boolean = true,
    val playerStatusOverlayEnabled: Boolean = false,
    val selectedAppIconId: String = NuvioAppIconOption.Default.id,
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

internal enum class NuvioEnhancedFeature(val id: String) {
    HomeExperienceControls("home_experience_controls"),
    BackupImport("backup_import"),
    FeatureHighlights("feature_highlights"),
    LiveTvControls("live_tv_controls"),
    HeroExperienceControls("hero_experience_controls"),
    DetailExperienceControls("detail_experience_controls"),
    PlayerStatusOverlay("player_status_overlay"),
    StatusBarVisibility("status_bar_visibility"),
    AppIconPicker("app_icon_picker"),
    CommunityLinks("community_links"),
    StreamSourcePinning("stream_source_pinning"),
    BackgroundStreamPrefetch("background_stream_prefetch"),
    ContentWarnings("content_warnings"),
}

@Serializable
private data class StoredNuvioEnhancedSettings(
    val enhancedHomeFeaturesEnabled: Boolean = true,
    val liveTvEnabled: Boolean = true,
    val streamSourcePinningEnabled: Boolean = false,
    val backgroundStreamPrefetchEnabled: Boolean = false,
    val heroDisplayMode: NuvioHeroDisplayMode = NuvioHeroDisplayMode.Balanced,
    val heroArtworkSource: NuvioHeroArtworkSource = NuvioHeroArtworkSource.Backdrop,
    val posterArtHeroEnabled: Boolean = false,
    val streamingShowcaseHeroEnabled: Boolean = false,
    val streamingShowcaseVideoPreviewEnabled: Boolean = true,
    val streamingShowcaseVideoPreviewSoundEnabled: Boolean = true,
    val compactHeroMetadata: Boolean = true,
    val showHeroRatings: Boolean = true,
    val showHeroOverview: Boolean = true,
    val hideHomeReleaseDates: Boolean = false,
    val heroOverviewUserConfigured: Boolean = false,
    val heroRefreshHapticsEnabled: Boolean = true,
    val statusBarVisible: Boolean = true,
    val playerStatusOverlayEnabled: Boolean = false,
    val selectedAppIconId: String = NuvioAppIconOption.Default.id,
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
                        decoded.copy(showHeroOverview = true)
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

    fun setLiveTvEnabled(enabled: Boolean) = update {
        copy(liveTvEnabled = enabled)
    }

    fun setStreamSourcePinningEnabled(enabled: Boolean) = update {
        copy(streamSourcePinningEnabled = enabled)
    }

    fun setBackgroundStreamPrefetchEnabled(enabled: Boolean) = update {
        copy(backgroundStreamPrefetchEnabled = enabled)
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

    fun setHideHomeReleaseDates(enabled: Boolean) = update {
        copy(hideHomeReleaseDates = enabled)
    }

    fun setHeroRefreshHapticsEnabled(enabled: Boolean) = update {
        copy(heroRefreshHapticsEnabled = enabled)
    }

    fun setPlayerStatusOverlayEnabled(enabled: Boolean) = update {
        copy(playerStatusOverlayEnabled = enabled)
    }

    fun setStatusBarVisible(visible: Boolean) = update {
        copy(statusBarVisible = visible)
    }

    fun setSelectedAppIcon(option: NuvioAppIconOption): Boolean {
        val applied = NuvioAppIconSwitcher.apply(option.id)
        update {
            copy(selectedAppIconId = option.id)
        }
        return applied
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
            liveTvEnabled = stored.liveTvEnabled,
            streamSourcePinningEnabled = stored.streamSourcePinningEnabled,
            backgroundStreamPrefetchEnabled = stored.backgroundStreamPrefetchEnabled,
            heroDisplayMode = stored.heroDisplayMode,
            heroArtworkSource = stored.heroArtworkSource,
            posterArtHeroEnabled = stored.posterArtHeroEnabled,
            streamingShowcaseHeroEnabled = stored.streamingShowcaseHeroEnabled,
            streamingShowcaseVideoPreviewEnabled = stored.streamingShowcaseVideoPreviewEnabled,
            streamingShowcaseVideoPreviewSoundEnabled = stored.streamingShowcaseVideoPreviewSoundEnabled,
            compactHeroMetadata = stored.compactHeroMetadata,
            showHeroRatings = stored.showHeroRatings,
            showHeroOverview = stored.showHeroOverview,
            hideHomeReleaseDates = stored.hideHomeReleaseDates,
            heroRefreshHapticsEnabled = stored.heroRefreshHapticsEnabled,
            statusBarVisible = stored.statusBarVisible,
            playerStatusOverlayEnabled = stored.playerStatusOverlayEnabled,
            selectedAppIconId = stored.selectedAppIconId,
            featureHighlightsEnabled = stored.featureHighlightsEnabled,
            discordWelcomeSeen = stored.discordWelcomeSeen,
            seenFeatureIds = stored.seenFeatureIds,
        )
    }

    private fun persist() {
        NuvioEnhancedSettingsStorage.savePayload(json.encodeToString(stored))
    }
}
