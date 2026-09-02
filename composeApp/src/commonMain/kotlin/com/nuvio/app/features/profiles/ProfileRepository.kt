package com.nuvio.app.features.profiles

import co.touchlab.kermit.Logger
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.core.auth.isAnonymous
import com.nuvio.app.core.network.SupabaseProvider
import com.nuvio.app.core.sync.putSyncOriginClientId
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.collection.CollectionMobileSettingsRepository
import com.nuvio.app.features.collection.CollectionRepository
import com.nuvio.app.features.cloudstream.CloudStreamRepository
import com.nuvio.app.features.downloads.DownloadsRepository
import com.nuvio.app.features.details.FavoritePeopleRepository
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.home.HomeRepository
import com.nuvio.app.core.ui.PosterCardStyleRepository
import com.nuvio.app.features.library.LibraryRepository
import com.nuvio.app.features.livetv.LiveTvRepository
import com.nuvio.app.features.mdblist.MdbListSettingsRepository
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsRepository
import com.nuvio.app.features.p2p.P2pSettingsRepository
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.features.plugins.PluginRepository
import com.nuvio.app.features.search.SearchHistoryRepository
import com.nuvio.app.features.settings.NuvioEnhancedSettingsRepository
import com.nuvio.app.features.settings.ThemeSettingsRepository
import com.nuvio.app.features.streams.StreamBadgeSettingsRepository
import com.nuvio.app.features.streams.StreamSourcePreferencesRepository
import com.nuvio.app.features.trakt.TraktAuthRepository
import com.nuvio.app.features.trakt.TraktSettingsRepository
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesRepository
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

@Serializable
private data class StoredProfilePayload(
    val userId: String,
    val activeProfileIndex: Int = 1,
    val hasEverSelectedProfile: Boolean = false,
    val rememberLastProfileEnabled: Boolean = false,
    val profiles: List<NuvioProfile> = emptyList(),
)

object ProfileRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val log = Logger.withTag("ProfileRepository")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun localizedString(resource: StringResource): String = runBlocking { getString(resource) }

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private var activeProfileIndex: Int = 1
    private var loadedCacheForUserId: String? = null

    val activeProfileId: Int get() = activeProfileIndex

    fun setRememberLastProfileEnabled(enabled: Boolean) {
        if (_state.value.rememberLastProfileEnabled == enabled) return

        _state.value = _state.value.copy(rememberLastProfileEnabled = enabled)
        persist()
    }

    fun loadCachedProfiles(): Boolean {
        val stored = decodeStoredPayload() ?: return false
        loadedCacheForUserId = stored.userId
        applyStoredPayload(stored)
        ThemeSettingsRepository.onProfileChanged()
        return _state.value.profiles.isNotEmpty()
    }

    fun ensureLoaded(userId: String) {
        if (loadedCacheForUserId == userId && _state.value.isLoaded) return

        val stored = decodeStoredPayload()
        loadedCacheForUserId = userId
        if (stored == null) {
            _state.value = ProfileState()
            activeProfileIndex = 1
            return
        }

        if (stored.userId != userId) {
            _state.value = ProfileState()
            activeProfileIndex = 1
            return
        }

        applyStoredPayload(stored)
    }

    fun clearInMemory() {
        loadedCacheForUserId = null
        activeProfileIndex = 1
        _state.value = ProfileState()
    }

    suspend fun pullProfiles(
        backgroundOverrides: Map<Int, String?> = emptyMap(),
        avatarUrlOverrides: Map<Int, String?> = emptyMap(),
        avatarIdOverrides: Map<Int, String?> = emptyMap(),
    ): Boolean {
        if (AuthRepository.state.value.isAnonymous) {
            if (!_state.value.isLoaded) {
                _state.value = _state.value.copy(isLoaded = true)
            }
            return true
        }
        try {
            val result = SupabaseProvider.client.postgrest.rpc("sync_pull_profiles")
            val profiles = mergeLocalProfileOverrides(
                remoteProfiles = result.decodeList<NuvioProfile>(),
                backgroundOverrides = backgroundOverrides,
                avatarUrlOverrides = avatarUrlOverrides,
                avatarIdOverrides = avatarIdOverrides,
            )
            _state.value = _state.value.copy(
                profiles = profiles.sortedBy { it.profileIndex },
                isLoaded = true,
                activeProfile = profiles.find { it.profileIndex == activeProfileIndex }
                    ?: profiles.firstOrNull(),
            )
            if (_state.value.activeProfile != null) {
                activeProfileIndex = _state.value.activeProfile!!.profileIndex
            }
            persist()
            return true
        } catch (e: Throwable) {
            if (AuthRepository.signOutIfSessionInvalid(e, "Profile pull")) return false
            log.e(e) { "Failed to pull profiles" }
            if (!_state.value.isLoaded) {
                _state.value = _state.value.copy(isLoaded = true)
            }
            return false
        }
    }

    fun selectProfile(profileIndex: Int) {
        val selectedProfile = _state.value.profiles.find { it.profileIndex == profileIndex }
        if (selectedProfile == null) {
            log.w { "Ignoring profile selection for missing profile index $profileIndex" }
            return
        }

        val alreadyActive =
            _state.value.activeProfile?.profileIndex == profileIndex &&
                _state.value.hasEverSelectedProfile

        activeProfileIndex = profileIndex
        _state.value = _state.value.copy(
            activeProfile = selectedProfile,
            hasEverSelectedProfile = true,
        )
        persist()

        if (alreadyActive) return

        notifyProfileChanged(profileIndex)
    }

    private fun notifyProfileChanged(profileIndex: Int) {
        runProfileChangeStep("watched") {
            WatchedRepository.onProfileChanged(profileIndex)
        }
        runProfileChangeStep("trakt_settings") {
            TraktSettingsRepository.onProfileChanged()
        }
        runProfileChangeStep("trakt_auth") {
            TraktAuthRepository.onProfileChanged()
        }
        runProfileChangeStep("library") {
            LibraryRepository.onProfileChanged(profileIndex)
        }
        runProfileChangeStep("favorite_people") {
            FavoritePeopleRepository.onProfileChanged(profileIndex)
        }
        runProfileChangeStep("watch_progress") {
            WatchProgressRepository.onProfileChanged(profileIndex)
        }
        runProfileChangeStep("addons") {
            AddonRepository.onProfileChanged(profileIndex)
        }
        if (com.nuvio.app.core.build.AppFeaturePolicy.pluginsEnabled) {
            runProfileChangeStep("plugins") {
                PluginRepository.onProfileChanged(profileIndex)
            }
            runProfileChangeStep("cloudstream") {
                CloudStreamRepository.onProfileChanged(profileIndex)
            }
        }
        runProfileChangeStep("theme") {
            ThemeSettingsRepository.onProfileChanged()
        }
        runProfileChangeStep("poster_card_style") {
            PosterCardStyleRepository.onProfileChanged()
        }
        runProfileChangeStep("player_settings") {
            PlayerSettingsRepository.onProfileChanged()
        }
        runProfileChangeStep("stream_badges") {
            StreamBadgeSettingsRepository.onProfileChanged()
        }
        runProfileChangeStep("stream_source_preferences") {
            StreamSourcePreferencesRepository.onProfileChanged()
        }
        runProfileChangeStep("p2p") {
            P2pSettingsRepository.onProfileChanged()
        }
        runProfileChangeStep("home_catalog_settings") {
            HomeCatalogSettingsRepository.onProfileChanged()
        }
        runProfileChangeStep("nuvio_enhanced_settings") {
            NuvioEnhancedSettingsRepository.onProfileChanged()
        }
        runProfileChangeStep("home") {
            HomeRepository.clear()
        }
        runProfileChangeStep("meta_screen_settings") {
            MetaScreenSettingsRepository.onProfileChanged()
        }
        runProfileChangeStep("continue_watching_preferences") {
            ContinueWatchingPreferencesRepository.onProfileChanged()
        }
        runProfileChangeStep("continue_watching_enrichment") {
            com.nuvio.app.features.watchprogress.ContinueWatchingEnrichmentCache.onProfileChanged()
        }
        runProfileChangeStep("episode_release_notifications") {
            EpisodeReleaseNotificationsRepository.onProfileChanged()
        }
        runProfileChangeStep("tmdb_settings") {
            TmdbSettingsRepository.onProfileChanged()
        }
        runProfileChangeStep("mdblist_settings") {
            MdbListSettingsRepository.onProfileChanged()
        }
        runProfileChangeStep("search_history") {
            SearchHistoryRepository.onProfileChanged()
        }
        runProfileChangeStep("collections") {
            CollectionRepository.onProfileChanged()
        }
        runProfileChangeStep("collection_mobile_settings") {
            CollectionMobileSettingsRepository.onProfileChanged()
        }
        runProfileChangeStep("downloads") {
            DownloadsRepository.onProfileChanged()
        }
        runProfileChangeStep("live_tv") {
            LiveTvRepository.onProfileChanged()
        }
    }

    private fun runProfileChangeStep(label: String, block: () -> Unit) {
        runCatching(block).onFailure { error ->
            log.e(error) { "Profile change step failed: $label" }
        }
    }

    suspend fun pushProfiles(profiles: List<ProfilePushPayload>): ProfileMutationResult {
        if (AuthRepository.state.value.isAnonymous) {
            applyPayloadsLocally(profiles)
            return ProfileMutationResult(success = true)
        }
        try {
            val backgroundOverrides = profiles.associate { payload ->
                payload.profileIndex to normalizedProfileBackgroundUrl(payload.backgroundUrl)
            }
            val avatarUrlOverrides = profiles.associate { payload ->
                payload.profileIndex to normalizedAvatarUrl(payload.avatarUrl)
            }
            val avatarIdOverrides = profiles.associate { payload ->
                payload.profileIndex to payload.avatarId
            }
            val params = buildJsonObject {
                put("p_client_max_profiles", MAX_PROFILES)
                put("p_profiles", buildRemotePushProfilesPayload(profiles))
                putSyncOriginClientId()
            }
            SupabaseProvider.client.postgrest.rpc("sync_push_profiles", params)
            if (!pullProfiles(
                    backgroundOverrides = backgroundOverrides,
                    avatarUrlOverrides = avatarUrlOverrides,
                    avatarIdOverrides = avatarIdOverrides,
                )
            ) {
                applyPayloadsLocally(profiles)
            }
            return ProfileMutationResult(success = true)
        } catch (e: Throwable) {
            if (AuthRepository.signOutIfSessionInvalid(e, "Profile push")) {
                return ProfileMutationResult(
                    success = false,
                    message = localizedString(Res.string.profile_save_failed),
                )
            }
            log.e(e) { "Failed to push profiles" }
            return ProfileMutationResult(
                success = false,
                message = e.message?.takeIf { it.isNotBlank() }
                    ?: localizedString(Res.string.profile_save_failed),
            )
        }
    }

    suspend fun createProfile(
        name: String,
        avatarColorHex: String,
        avatarId: String? = null,
        avatarUrl: String? = null,
        backgroundUrl: String? = null,
        usesPrimaryAddons: Boolean = false,
    ): ProfileMutationResult {
        val existing = _state.value.profiles
        val nextIndex = ((1..MAX_PROFILES).toSet() - existing.map { it.profileIndex }.toSet()).minOrNull()
            ?: return ProfileMutationResult(
                success = false,
                message = localizedString(Res.string.profile_max_profiles_reached),
            )

        val allPayloads = existing.map { profile ->
            ProfilePushPayload(
                profileIndex = profile.profileIndex,
                name = profile.name,
                avatarColorHex = profile.avatarColorHex,
                usesPrimaryAddons = profile.usesPrimaryAddons,
                usesPrimaryPlugins = profile.usesPrimaryPlugins,
                avatarId = profile.avatarId,
                avatarUrl = profile.avatarUrl,
                backgroundUrl = profile.backgroundUrl,
            )
        } + ProfilePushPayload(
            profileIndex = nextIndex,
            name = name,
            avatarColorHex = avatarColorHex,
            usesPrimaryAddons = usesPrimaryAddons,
            usesPrimaryPlugins = usesPrimaryAddons,
            avatarId = avatarId,
            avatarUrl = avatarUrl,
            backgroundUrl = backgroundUrl,
        )

        return pushProfiles(allPayloads)
    }

    suspend fun updateProfile(
        profileIndex: Int,
        name: String,
        avatarColorHex: String,
        avatarId: String? = null,
        avatarUrl: String? = null,
        backgroundUrl: String? = null,
        usesPrimaryAddons: Boolean = false,
    ): ProfileMutationResult {
        val allPayloads = _state.value.profiles.map { profile ->
            if (profile.profileIndex == profileIndex) {
                ProfilePushPayload(
                    profileIndex = profileIndex,
                    name = name,
                    avatarColorHex = avatarColorHex,
                    usesPrimaryAddons = usesPrimaryAddons,
                    usesPrimaryPlugins = usesPrimaryAddons,
                    avatarId = avatarId,
                    avatarUrl = avatarUrl,
                    backgroundUrl = backgroundUrl,
                )
            } else {
                ProfilePushPayload(
                    profileIndex = profile.profileIndex,
                    name = profile.name,
                    avatarColorHex = profile.avatarColorHex,
                    usesPrimaryAddons = profile.usesPrimaryAddons,
                    usesPrimaryPlugins = profile.usesPrimaryPlugins,
                    avatarId = profile.avatarId,
                    avatarUrl = profile.avatarUrl,
                    backgroundUrl = profile.backgroundUrl,
                )
            }
        }

        return pushProfiles(allPayloads)
    }

    suspend fun deleteProfile(profileIndex: Int) {
        if (AuthRepository.state.value.isAnonymous) {
            val remaining = _state.value.profiles.filter { it.profileIndex != profileIndex }
            ProfilePinCacheStorage.removePayload(profileIndex)
            _state.value = _state.value.copy(
                profiles = remaining,
                activeProfile = if (_state.value.activeProfile?.profileIndex == profileIndex) remaining.firstOrNull() else _state.value.activeProfile,
            )
            if (_state.value.activeProfile != null) {
                activeProfileIndex = _state.value.activeProfile!!.profileIndex
            }
            persist()
            return
        }
        try {
            val params = buildJsonObject {
                put("p_profile_id", profileIndex)
                putSyncOriginClientId()
            }
            SupabaseProvider.client.postgrest.rpc("sync_delete_profile_data", params)
            pullProfiles()
        } catch (e: Throwable) {
            if (AuthRepository.signOutIfSessionInvalid(e, "Profile delete")) return
            log.e(e) { "Failed to delete profile $profileIndex" }
        }
    }

    suspend fun verifyPin(profileIndex: Int, pin: String): PinVerifyResult {
        if (AuthRepository.state.value !is AuthState.Authenticated) {
            return verifyPinLocally(profileIndex, pin)
        }

        return runCatching {
            val params = buildJsonObject {
                put("p_profile_id", profileIndex)
                put("p_pin", pin)
            }
            val result = SupabaseProvider.client.postgrest.rpc("verify_profile_pin", params)
            result.decodeSingle<PinVerifyResult>().also { verifyResult ->
                if (verifyResult.unlocked) {
                    rememberVerifiedPin(profileIndex = profileIndex, pin = pin)
                }
            }
        }.getOrElse { e ->
            log.e(e) { "Failed to verify pin" }
            verifyPinLocally(profileIndex, pin)
        }
    }

    suspend fun setPin(profileIndex: Int, pin: String, currentPin: String? = null): PinVerifyResult {
        if (AuthRepository.state.value !is AuthState.Authenticated) {
            return PinVerifyResult(unlocked = false, message = getString(Res.string.profile_pin_set_requires_internet))
        }

        return runCatching {
            val params = buildJsonObject {
                put("p_profile_id", profileIndex)
                put("p_pin", pin)
                currentPin?.let { put("p_current_pin", it) }
            }
            SupabaseProvider.client.postgrest.rpc("set_profile_pin", params)
            pullProfiles()
            rememberVerifiedPin(profileIndex = profileIndex, pin = pin)
            PinVerifyResult(unlocked = true)
        }.onFailure { e ->
            log.e(e) { "Failed to set pin" }
        }.getOrElse {
            PinVerifyResult(unlocked = false, message = getString(Res.string.profile_pin_set_failed))
        }
    }

    suspend fun clearPin(profileIndex: Int, currentPin: String? = null): PinVerifyResult {
        if (AuthRepository.state.value !is AuthState.Authenticated) {
            return PinVerifyResult(unlocked = false, message = getString(Res.string.profile_pin_clear_requires_internet))
        }

        return runCatching {
            val params = buildJsonObject {
                put("p_profile_id", profileIndex)
                currentPin?.let { put("p_current_pin", it) }
            }
            SupabaseProvider.client.postgrest.rpc("clear_profile_pin", params)
            pullProfiles()
            ProfilePinCacheStorage.removePayload(profileIndex)
            PinVerifyResult(unlocked = true)
        }.onFailure { e ->
            log.e(e) { "Failed to clear pin" }
        }.getOrElse {
            PinVerifyResult(unlocked = false, message = getString(Res.string.profile_pin_clear_failed))
        }
    }

    suspend fun clearPinWithPassword(profileIndex: Int, accountPassword: String) {
        runCatching {
            val params = buildJsonObject {
                put("p_account_password", accountPassword)
                put("p_profile_id", profileIndex)
            }
            SupabaseProvider.client.postgrest.rpc("clear_profile_pin_with_account_password", params)
            pullProfiles()
            ProfilePinCacheStorage.removePayload(profileIndex)
        }.onFailure { e ->
            log.e(e) { "Failed to clear pin with password" }
        }
    }

    suspend fun pullProfileLocks(): List<ProfileLockState> {
        return runCatching {
            val result = SupabaseProvider.client.postgrest.rpc("sync_pull_profile_locks")
            result.decodeList<ProfileLockState>()
        }.getOrElse { e ->
            log.e(e) { "Failed to pull profile locks" }
            emptyList()
        }
    }

    private fun applyPayloadsLocally(payloads: List<ProfilePushPayload>) {
        val authState = AuthRepository.state.value as? AuthState.Authenticated ?: return
        val profiles = payloads.map { p ->
            NuvioProfile(
                id = "",
                userId = authState.userId,
                profileIndex = p.profileIndex,
                name = p.name,
                avatarColorHex = p.avatarColorHex,
                avatarId = p.avatarId,
                avatarUrl = p.avatarUrl,
                backgroundUrl = p.backgroundUrl,
                usesPrimaryAddons = p.usesPrimaryAddons,
                usesPrimaryPlugins = p.usesPrimaryPlugins,
            )
        }.sortedBy { it.profileIndex }
        _state.value = _state.value.copy(
            profiles = profiles,
            isLoaded = true,
            activeProfile = profiles.find { it.profileIndex == activeProfileIndex } ?: profiles.firstOrNull(),
        )
        if (_state.value.activeProfile != null) {
            activeProfileIndex = _state.value.activeProfile!!.profileIndex
        }
        syncPinCache(profiles)
        persist()
    }

    private fun buildRemotePushProfilesPayload(profiles: List<ProfilePushPayload>) = buildJsonArray {
        profiles.forEach { payload ->
            add(
                buildJsonObject {
                    put("profile_index", payload.profileIndex)
                    put("name", payload.name)
                    put("avatar_color_hex", payload.avatarColorHex)
                    put("uses_primary_addons", payload.usesPrimaryAddons)
                    put("uses_primary_plugins", payload.usesPrimaryPlugins)
                    put("avatar_id", payload.avatarId?.let(::JsonPrimitive) ?: JsonNull)
                    put("avatar_url", normalizedAvatarUrl(payload.avatarUrl)?.let(::JsonPrimitive) ?: JsonNull)
                },
            )
        }
    }

    private fun mergeLocalProfileOverrides(
        remoteProfiles: List<NuvioProfile>,
        backgroundOverrides: Map<Int, String?> = emptyMap(),
        avatarUrlOverrides: Map<Int, String?> = emptyMap(),
        avatarIdOverrides: Map<Int, String?> = emptyMap(),
    ): List<NuvioProfile> {
        val inMemoryBackgrounds = _state.value.profiles.associate { profile ->
            profile.profileIndex to normalizedProfileBackgroundUrl(profile.backgroundUrl)
        }
        val cachedBackgrounds = decodeStoredPayload()
            ?.profiles
            .orEmpty()
            .associate { profile ->
                profile.profileIndex to normalizedProfileBackgroundUrl(profile.backgroundUrl)
            }

        return remoteProfiles.map { profile ->
            val resolvedBackground = when {
                backgroundOverrides.containsKey(profile.profileIndex) -> {
                    backgroundOverrides[profile.profileIndex]
                }
                !profile.backgroundUrl.isNullOrBlank() -> {
                    normalizedProfileBackgroundUrl(profile.backgroundUrl)
                }
                else -> {
                    inMemoryBackgrounds[profile.profileIndex]
                        ?: cachedBackgrounds[profile.profileIndex]
                }
            }
            val resolvedAvatarUrl = if (avatarUrlOverrides.containsKey(profile.profileIndex)) {
                avatarUrlOverrides[profile.profileIndex]
            } else {
                normalizedAvatarUrl(profile.avatarUrl)
            }
            val resolvedAvatarId = if (avatarIdOverrides.containsKey(profile.profileIndex)) {
                avatarIdOverrides[profile.profileIndex]
            } else {
                profile.avatarId
            }
            profile.copy(
                avatarId = resolvedAvatarId,
                avatarUrl = resolvedAvatarUrl,
                backgroundUrl = resolvedBackground,
            )
        }
    }

    private fun decodeStoredPayload(): StoredProfilePayload? {
        val payload = ProfileStorage.loadPayload().orEmpty().trim()
        if (payload.isEmpty()) return null

        return runCatching {
            json.decodeFromString<StoredProfilePayload>(payload)
        }.getOrNull()
    }

    private fun applyStoredPayload(stored: StoredProfilePayload) {
        val profiles = stored.profiles.sortedBy { it.profileIndex }
        activeProfileIndex = stored.activeProfileIndex
        _state.value = ProfileState(
            profiles = profiles,
            activeProfile = profiles.find { it.profileIndex == activeProfileIndex } ?: profiles.firstOrNull(),
            isLoaded = profiles.isNotEmpty(),
            hasEverSelectedProfile = stored.hasEverSelectedProfile,
            rememberLastProfileEnabled = stored.rememberLastProfileEnabled,
        )
        _state.value.activeProfile?.let { activeProfileIndex = it.profileIndex }
        syncPinCache(profiles)
    }

    private fun rememberVerifiedPin(profileIndex: Int, pin: String) {
        val profile = _state.value.profiles.find { it.profileIndex == profileIndex }
        val salt = generateProfilePinSalt()
        val payload = CachedProfilePinPayload(
            salt = salt,
            digest = hashProfilePin(profileIndex = profileIndex, salt = salt, pin = pin),
            profileUpdatedAt = profile?.updatedAt.orEmpty(),
        )
        ProfilePinCacheStorage.savePayload(profileIndex, json.encodeToString(payload))
    }

    private fun verifyPinLocally(profileIndex: Int, pin: String): PinVerifyResult {
        val profile = _state.value.profiles.find { it.profileIndex == profileIndex }
        if (profile?.pinEnabled != true) {
            return PinVerifyResult(unlocked = true)
        }

        val payload = ProfilePinCacheStorage.loadPayload(profileIndex).orEmpty().trim()
        if (payload.isEmpty()) {
            return PinVerifyResult(
                unlocked = false,
                message = localizedString(Res.string.profile_pin_offline_verification_requires_online),
            )
        }

        val cached = runCatching {
            json.decodeFromString<CachedProfilePinPayload>(payload)
        }.getOrNull() ?: return PinVerifyResult(
            unlocked = false,
            message = localizedString(Res.string.profile_pin_offline_verification_requires_online),
        )

        if (
            cached.profileUpdatedAt.isNotBlank() &&
            profile.updatedAt.isNotBlank() &&
            cached.profileUpdatedAt != profile.updatedAt
        ) {
            ProfilePinCacheStorage.removePayload(profileIndex)
            return PinVerifyResult(
                unlocked = false,
                message = localizedString(Res.string.profile_pin_changed_requires_refresh),
            )
        }

        val digest = hashProfilePin(profileIndex = profileIndex, salt = cached.salt, pin = pin)
        return if (digest == cached.digest) {
            PinVerifyResult(unlocked = true)
        } else {
            PinVerifyResult(unlocked = false, message = localizedString(Res.string.pin_incorrect))
        }
    }

    private fun syncPinCache(profiles: List<NuvioProfile>) {
        val profilesByIndex = profiles.associateBy { it.profileIndex }
        for (profileIndex in 1..MAX_PROFILES) {
            val profile = profilesByIndex[profileIndex]
            if (profile == null || !profile.pinEnabled) {
                ProfilePinCacheStorage.removePayload(profileIndex)
                continue
            }

            val raw = ProfilePinCacheStorage.loadPayload(profileIndex).orEmpty().trim()
            if (raw.isEmpty()) continue

            val cached = runCatching {
                json.decodeFromString<CachedProfilePinPayload>(raw)
            }.getOrNull() ?: run {
                ProfilePinCacheStorage.removePayload(profileIndex)
                continue
            }

            if (
                cached.profileUpdatedAt.isNotBlank() &&
                profile.updatedAt.isNotBlank() &&
                cached.profileUpdatedAt != profile.updatedAt
            ) {
                ProfilePinCacheStorage.removePayload(profileIndex)
            }
        }
    }

    private fun persist() {
        val authState = AuthRepository.state.value as? AuthState.Authenticated ?: return
        val state = _state.value
        ProfileStorage.savePayload(
            json.encodeToString(
                StoredProfilePayload(
                    userId = authState.userId,
                    activeProfileIndex = activeProfileIndex,
                    hasEverSelectedProfile = state.hasEverSelectedProfile,
                    rememberLastProfileEnabled = state.rememberLastProfileEnabled,
                    profiles = state.profiles,
                ),
            ),
        )
    }
}

@kotlinx.serialization.Serializable
data class ProfileLockState(
    @kotlinx.serialization.SerialName("profile_index") val profileIndex: Int,
    @kotlinx.serialization.SerialName("pin_enabled") val pinEnabled: Boolean = false,
    @kotlinx.serialization.SerialName("pin_locked_until") val pinLockedUntil: String? = null,
)
