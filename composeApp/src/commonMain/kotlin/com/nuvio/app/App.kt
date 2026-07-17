package com.nuvio.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.nuvio.app.core.build.AppFeaturePolicy
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.core.diagnostics.CrashDiagnostics
import com.nuvio.app.core.diagnostics.LocalCrashReport
import com.nuvio.app.core.deeplink.AppDeepLink
import com.nuvio.app.core.deeplink.AppDeepLinkRepository
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import com.nuvio.app.core.sync.AppForegroundMonitor
import com.nuvio.app.core.sync.ProfileSettingsSync
import com.nuvio.app.core.sync.RealtimeSyncConfig
import com.nuvio.app.core.sync.RealtimeSyncInvalidationService
import com.nuvio.app.core.sync.SyncManager
import com.nuvio.app.core.ui.NuvioNavigationBar
import com.nuvio.app.core.ui.NuvioContinueWatchingActionSheet
import com.nuvio.app.core.ui.NuvioPosterZoomActionOverlay
import com.nuvio.app.core.ui.NuvioStatusModal
import com.nuvio.app.core.ui.PosterZoomAnchor
import com.nuvio.app.core.ui.PosterZoomAnchorHolder
import com.nuvio.app.core.ui.PosterZoomOverlayAction
import com.nuvio.app.core.ui.PlatformBackHandler
import com.nuvio.app.core.ui.AppSystemUiController
import com.nuvio.app.core.ui.platformExitApp
import com.nuvio.app.core.ui.configurePlatformImageLoader
import com.nuvio.app.core.ui.NuvioToastHost
import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.core.ui.NuvioFloatingPrompt
import com.nuvio.app.core.ui.ProfileMeshBackground
import com.nuvio.app.core.ui.TraktListPickerDialog
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.LocalNuvioBottomNavigationOverlayPadding
import com.nuvio.app.core.ui.NativeNavigationTab
import com.nuvio.app.core.ui.NativeTabBridge
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.isLiquidGlassNativeTabBarSupported
import com.nuvio.app.core.ui.appIconPainter
import com.nuvio.app.core.ui.localizedContinueWatchingSubtitle
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.nuvioBottomNavigationBarInsets
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.nuvio.app.features.auth.AuthScreen
import com.nuvio.app.features.addons.AddAddonResult
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.catalog.CatalogRepository
import com.nuvio.app.features.catalog.CatalogScreen
import com.nuvio.app.features.catalog.CatalogTarget
import com.nuvio.app.features.cloud.CloudLibraryContentType
import com.nuvio.app.features.cloud.CloudLibraryFile
import com.nuvio.app.features.cloud.CloudLibraryItem
import com.nuvio.app.features.cloud.CloudLibraryPlaybackResult
import com.nuvio.app.features.cloud.CloudLibraryPlaybackTargetLookupResult
import com.nuvio.app.features.cloud.CloudLibraryRepository
import com.nuvio.app.features.cloud.playbackVideoId
import com.nuvio.app.features.cloud.providerPosterUrl
import com.nuvio.app.features.debrid.DirectDebridPlayableResult
import com.nuvio.app.features.debrid.DirectDebridPlaybackResolver
import com.nuvio.app.features.debrid.toastMessage
import com.nuvio.app.features.downloads.DownloadItem
import com.nuvio.app.features.downloads.DownloadsRepository
import com.nuvio.app.features.downloads.DownloadsScreen
import com.nuvio.app.features.details.MetaDetailsRepository
import com.nuvio.app.features.details.MetaDetailsScreen
import com.nuvio.app.features.details.MetaPerson
import com.nuvio.app.features.details.PersonDetailScreen
import com.nuvio.app.features.details.TmdbEntityBrowseScreen
import com.nuvio.app.features.tmdb.TmdbEntityKind
import com.nuvio.app.features.home.HomeCatalogSection
import com.nuvio.app.features.home.HomeScreen
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.components.CollectionCardRemoteImage
import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.library.LibraryRepository
import com.nuvio.app.features.library.LibrarySection
import com.nuvio.app.features.library.LibrarySourceMode
import com.nuvio.app.features.library.LibraryScreen
import com.nuvio.app.features.library.toLibraryItem
import com.nuvio.app.features.library.toMetaPreview
import com.nuvio.app.features.livetv.LiveTvChannel
import com.nuvio.app.features.livetv.LiveTvIncomingSource
import com.nuvio.app.features.livetv.LiveTvIncomingSourceRepository
import com.nuvio.app.features.livetv.LiveTvRepository
import com.nuvio.app.features.livetv.LiveTvScreen
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsRepository
import com.nuvio.app.features.p2p.P2pConsentDialog
import com.nuvio.app.features.p2p.P2pSettingsRepository
import com.nuvio.app.features.player.PlayerLaunch
import com.nuvio.app.features.player.PlayerLaunchStore
import com.nuvio.app.features.player.PlayerRoute
import com.nuvio.app.features.player.PlayerScreen
import com.nuvio.app.features.player.PlayerPlaybackSnapshot
import com.nuvio.app.features.player.ExternalPlayerIntentResult
import com.nuvio.app.features.player.ExternalPlayerPlatform
import com.nuvio.app.features.player.ExternalPlayerPlaybackRequest
import com.nuvio.app.features.player.rememberExternalPlayerLauncher
import com.nuvio.app.features.player.prepareExternalPlayerLaunch
import com.nuvio.app.features.player.SubtitleLanguageOption
import com.nuvio.app.features.player.sanitizePlaybackHeaders
import com.nuvio.app.features.player.sanitizePlaybackResponseHeaders
import com.nuvio.app.features.profiles.AvatarRepository
import com.nuvio.app.features.profiles.NuvioProfile
import com.nuvio.app.features.profiles.NativeProfileSwitcherPopup
import com.nuvio.app.features.profiles.ProfileEditScreen
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.profiles.ProfileSelectionScreen
import com.nuvio.app.features.profiles.ProfileSwitcherTab
import com.nuvio.app.features.profiles.parseHexColor
import com.nuvio.app.features.profiles.profileAvatarImageUrl
import com.nuvio.app.features.profiles.profileBackgroundImageUrl
import com.nuvio.app.features.search.SearchScreen
import com.nuvio.app.features.settings.SettingsScreen
import com.nuvio.app.features.settings.HomescreenSettingsScreen
import com.nuvio.app.features.settings.MetaScreenSettingsScreen
import com.nuvio.app.features.settings.ContinueWatchingSettingsScreen
import com.nuvio.app.features.settings.AddonsSettingsScreen
import com.nuvio.app.features.settings.PluginsSettingsScreen
import com.nuvio.app.features.settings.AccountSettingsScreen
import com.nuvio.app.features.settings.SupportersContributorsSettingsScreen
import com.nuvio.app.features.settings.LicensesAttributionsSettingsScreen
import com.nuvio.app.features.settings.NuvioEnhancedSettingsRepository
import com.nuvio.app.features.settings.SettingsPage
import com.nuvio.app.features.settings.ThemeSettingsRepository
import com.nuvio.app.features.collection.CollectionManagementScreen
import com.nuvio.app.features.collection.CollectionEditorScreen
import com.nuvio.app.features.collection.CollectionEditorRepository
import com.nuvio.app.features.collection.CollectionSyncService
import com.nuvio.app.features.collection.FolderDetailScreen
import com.nuvio.app.features.collection.FolderDetailRepository
import com.nuvio.app.features.streams.StreamAutoPlayPolicy
import com.nuvio.app.features.streams.BingeGroupCacheRepository
import com.nuvio.app.features.streams.StreamBehaviorHints
import com.nuvio.app.features.streams.StreamItem
import com.nuvio.app.features.streams.StreamLaunch
import com.nuvio.app.features.streams.StreamLaunchStore
import com.nuvio.app.features.streams.StreamLinkCacheRepository
import com.nuvio.app.features.streams.StreamsRepository
import com.nuvio.app.features.streams.StreamsScreen
import com.nuvio.app.features.tmdb.TmdbService
import com.nuvio.app.features.player.PlayerSettingsRepository
import com.nuvio.app.features.trakt.TraktAuthRepository
import com.nuvio.app.features.trakt.TraktListTab
import com.nuvio.app.features.trakt.TraktScrobbleRepository
import com.nuvio.app.features.updater.AppUpdaterHost
import com.nuvio.app.features.updater.AppUpdaterPlatform
import com.nuvio.app.features.updater.rememberAppUpdaterController
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesRepository
import com.nuvio.app.features.watchprogress.ResumePromptRepository
import com.nuvio.app.features.watchprogress.WatchProgressPlaybackSession
import com.nuvio.app.features.watchprogress.WatchProgressRepository
import com.nuvio.app.features.watchprogress.nextUpDismissKey
import com.nuvio.app.features.watchprogress.toContinueWatchingItem
import com.nuvio.app.features.watching.application.WatchingActions
import com.nuvio.app.features.watching.application.WatchingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import nuvio.composeapp.generated.resources.*
import nuvio.composeapp.generated.resources.app_logo_wordmark
import nuvio.composeapp.generated.resources.compose_catalog_subtitle_library
import nuvio.composeapp.generated.resources.compose_catalog_subtitle_trakt_library
import nuvio.composeapp.generated.resources.compose_nav_home
import nuvio.composeapp.generated.resources.compose_nav_library
import nuvio.composeapp.generated.resources.compose_nav_profile
import nuvio.composeapp.generated.resources.compose_nav_search
import nuvio.composeapp.generated.resources.sidebar_library
import nuvio.composeapp.generated.resources.sidebar_search
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val NuvioDiscordInviteUrl = "https://discord.gg/at8xffxuRU"

@Serializable
object TabsRoute

@Serializable
data class DetailRoute(val type: String, val id: String)

@Serializable
data class PersonDetailRoute(
    val personId: Int,
    val personName: String,
    val personPhoto: String? = null,
    val castAvatarTransitionKey: String? = null,
    val preferCrew: Boolean = false,
)

@Serializable
data class EntityBrowseRoute(
    val entityKind: String,
    val entityId: Int,
    val entityName: String,
    val sourceType: String = "tv",
)

private data class PendingP2pStreamOpen(
    val stream: StreamItem,
    val resumePositionMs: Long?,
    val resumeProgressFraction: Float?,
    val forceExternal: Boolean,
    val forceInternal: Boolean,
    val isAutoPlay: Boolean,
)

@Serializable
object HomescreenSettingsRoute

@Serializable
object MetaScreenSettingsRoute

@Serializable
object ContinueWatchingSettingsRoute

@Serializable
object DownloadsSettingsRoute

@Serializable
object AddonsSettingsRoute

@Serializable
object PluginsSettingsRoute

@Serializable
object AccountSettingsRoute

@Serializable
object SupportersContributorsSettingsRoute

@Serializable
object LicensesAttributionsSettingsRoute

@Serializable
object CollectionsRoute

@Serializable
data class CollectionEditorRoute(val collectionId: String? = null)

@Serializable
data class FolderDetailRoute(val collectionId: String, val folderId: String)

@Serializable
data class StreamRoute(
    val launchId: Long,
)

@Serializable
data class CatalogRoute(
    val launchId: Long,
)

private data class CatalogLaunch(
    val title: String,
    val subtitle: String,
    val target: CatalogTarget,
)

private object CatalogLaunchStore {
    private var nextLaunchId = 1L
    private val launches = mutableMapOf<Long, CatalogLaunch>()

    fun put(launch: CatalogLaunch): Long {
        val launchId = nextLaunchId++
        launches[launchId] = launch
        return launchId
    }

    fun get(launchId: Long): CatalogLaunch? = launches[launchId]

    fun remove(launchId: Long) {
        launches.remove(launchId)
    }
}

private data class PosterActionTarget(
    val preview: MetaPreview,
    val libraryItem: LibraryItem? = null,
    val libraryListKey: String? = null,
)

enum class AppScreenTab {
    Home,
    Search,
    LiveTv,
    Library,
    Settings,
}

private fun AppScreenTab.toNativeNavigationTab(): NativeNavigationTab = when (this) {
    AppScreenTab.Home -> NativeNavigationTab.Home
    AppScreenTab.Search -> NativeNavigationTab.Search
    AppScreenTab.LiveTv -> NativeNavigationTab.LiveTv
    AppScreenTab.Library -> NativeNavigationTab.Library
    AppScreenTab.Settings -> NativeNavigationTab.Settings
}

private fun NativeNavigationTab.toAppScreenTab(): AppScreenTab = when (this) {
    NativeNavigationTab.Home -> AppScreenTab.Home
    NativeNavigationTab.Search -> AppScreenTab.Search
    NativeNavigationTab.LiveTv -> AppScreenTab.LiveTv
    NativeNavigationTab.Library -> AppScreenTab.Library
    NativeNavigationTab.Settings -> AppScreenTab.Settings
}

private fun PlayerLaunch.toExternalPlayerPlaybackRequest(): ExternalPlayerPlaybackRequest =
    ExternalPlayerPlaybackRequest(
        sourceUrl = sourceUrl,
        title = title,
        streamTitle = streamTitle,
        sourceHeaders = sourceHeaders,
        resumePositionMs = initialPositionMs,
        season = seasonNumber,
        episode = episodeNumber,
        episodeTitle = episodeTitle,
    )

private enum class AppGateScreen {
    Loading,
    Auth,
    ProfileSelection,
    ProfileEdit,
    Main,
}

private enum class ProfileEditReturnTarget {
    ProfileSelection,
    ProfileSettings,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .components {
                add(SvgDecoder.Factory())
            }
            .configurePlatformImageLoader(context)
            .build()
    }
    val selectedTheme by remember {
        ThemeSettingsRepository.ensureLoaded()
        ThemeSettingsRepository.selectedTheme
    }.collectAsStateWithLifecycle()
    val amoledEnabled by remember { ThemeSettingsRepository.amoledEnabled }.collectAsStateWithLifecycle()
    val customThemeFirstColor by remember {
        ThemeSettingsRepository.customThemeFirstColor
    }.collectAsStateWithLifecycle()
    val customThemeSecondColor by remember {
        ThemeSettingsRepository.customThemeSecondColor
    }.collectAsStateWithLifecycle()
    NuvioTheme(
        appTheme = selectedTheme,
        customFirst = customThemeFirstColor,
        customSecond = customThemeSecondColor,
        amoled = amoledEnabled,
    ) {
        LaunchedEffect(Unit) {
            AuthRepository.initialize()
        }

        LaunchedEffect(Unit) {
            NetworkStatusRepository.ensureStarted()
            ProfileRepository.loadCachedProfiles()
            AvatarRepository.fetchAvatars()
        }

        val authState by AuthRepository.state.collectAsStateWithLifecycle()
        val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
        val profileAvatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
        val networkStatusUiState by remember {
            NetworkStatusRepository.uiState
        }.collectAsStateWithLifecycle()
        val pendingCrashReport by remember {
            CrashDiagnostics.pendingReport
        }.collectAsStateWithLifecycle()

        LaunchedEffect(
            profileState.activeProfile?.profileIndex,
            profileState.activeProfile?.name,
            profileState.activeProfile?.avatarColorHex,
            profileState.activeProfile?.avatarId,
            profileState.activeProfile?.avatarUrl,
            profileAvatars,
        ) {
            val activeProfile = profileState.activeProfile
            val avatarItem = activeProfile?.avatarId?.let { avatarId ->
                profileAvatars.find { it.id == avatarId }
            }
            NativeTabBridge.publishProfileTabIcon(
                name = activeProfile?.name,
                avatarColorHex = activeProfile?.avatarColorHex,
                avatarImageUrl = activeProfile?.let { profileAvatarImageUrl(it, avatarItem) },
                avatarBackgroundColorHex = avatarItem?.bgColor,
            )
        }

        var gateScreen by rememberSaveable { mutableStateOf(AppGateScreen.Loading.name) }
        var editingProfile by remember { mutableStateOf<NuvioProfile?>(null) }
        var isNewProfile by remember { mutableStateOf(false) }
        var autoSkipProfileSelection by rememberSaveable { mutableStateOf(false) }
        var profileEditReturnTarget by rememberSaveable { mutableStateOf(ProfileEditReturnTarget.ProfileSelection.name) }
        var pendingMainInitialTabName by rememberSaveable { mutableStateOf<String?>(null) }
        var pendingMainSettingsPageName by rememberSaveable { mutableStateOf<String?>(null) }

        fun rememberedStartupProfile(profiles: List<NuvioProfile>): NuvioProfile? {
            val currentProfileState = ProfileRepository.state.value
            if (
                !currentProfileState.rememberLastProfileEnabled ||
                !currentProfileState.hasEverSelectedProfile
            ) {
                return null
            }

            return profiles
                .find { it.profileIndex == ProfileRepository.activeProfileId }
                ?.takeUnless { it.pinEnabled }
        }

        fun enterProfileGate(profiles: List<NuvioProfile>, syncOnEnter: Boolean) {
            if (profiles.isEmpty()) {
                autoSkipProfileSelection = true
                gateScreen = AppGateScreen.ProfileSelection.name
                return
            }

            rememberedStartupProfile(profiles)?.let { profile ->
                ProfileRepository.selectProfile(profile.profileIndex)
                if (syncOnEnter) {
                    SyncManager.pullAllForProfile(profile.profileIndex)
                }
                gateScreen = AppGateScreen.Main.name
                autoSkipProfileSelection = false
                return
            }

            autoSkipProfileSelection = true
            if (profiles.size == 1) {
                val onlyProfile = profiles.first()
                if (onlyProfile.pinEnabled) {
                    gateScreen = AppGateScreen.ProfileSelection.name
                    return
                }
                ProfileRepository.selectProfile(onlyProfile.profileIndex)
                if (syncOnEnter) {
                    SyncManager.pullAllForProfile(onlyProfile.profileIndex)
                }
                gateScreen = AppGateScreen.Main.name
                autoSkipProfileSelection = false
            } else {
                gateScreen = AppGateScreen.ProfileSelection.name
            }
        }

        LaunchedEffect(authState, networkStatusUiState.condition, profileState.profiles) {
            val cachedProfiles = profileState.profiles
            val hasCachedProfileAccess =
                cachedProfiles.isNotEmpty() &&
                    authState !is AuthState.Authenticated
            val allowCachedProfileAccess =
                hasCachedProfileAccess &&
                    (
                        networkStatusUiState.condition != NetworkCondition.Online ||
                            gateScreen != AppGateScreen.Auth.name
                    )

            when (authState) {
                is AuthState.Loading -> {
                    if (hasCachedProfileAccess) {
                        enterProfileGate(cachedProfiles, syncOnEnter = false)
                    } else {
                        gateScreen = AppGateScreen.Loading.name
                    }
                }
                is AuthState.Unauthenticated -> {
                    if (allowCachedProfileAccess) {
                        enterProfileGate(cachedProfiles, syncOnEnter = false)
                    } else {
                        ProfileRepository.clearInMemory()
                        gateScreen = AppGateScreen.Auth.name
                    }
                }
                is AuthState.Authenticated -> {
                    val authenticatedState = authState as AuthState.Authenticated
                    ProfileRepository.ensureLoaded(authenticatedState.userId)
                    if (gateScreen == AppGateScreen.Loading.name || gateScreen == AppGateScreen.Auth.name) {
                        enterProfileGate(ProfileRepository.state.value.profiles, syncOnEnter = true)
                    }
                }
            }
        }

        LaunchedEffect((authState as? AuthState.Authenticated)?.userId) {
            val authenticatedState = authState as? AuthState.Authenticated ?: return@LaunchedEffect
            ProfileRepository.ensureLoaded(authenticatedState.userId)
            ProfileRepository.pullProfiles()
        }

        LaunchedEffect(
            gateScreen,
            autoSkipProfileSelection,
            profileState.profiles,
            profileState.hasEverSelectedProfile,
            profileState.rememberLastProfileEnabled,
            profileState.activeProfile?.profileIndex,
            profileState.activeProfile?.pinEnabled,
        ) {
            if (
                autoSkipProfileSelection &&
                gateScreen == AppGateScreen.ProfileSelection.name
            ) {
                rememberedStartupProfile(profileState.profiles)?.let { profile ->
                    ProfileRepository.selectProfile(profile.profileIndex)
                    SyncManager.pullAllForProfile(profile.profileIndex)
                    gateScreen = AppGateScreen.Main.name
                    autoSkipProfileSelection = false
                    return@LaunchedEffect
                }

                if (profileState.profiles.size != 1) return@LaunchedEffect

                val onlyProfile = profileState.profiles.first()
                if (onlyProfile.pinEnabled) return@LaunchedEffect

                ProfileRepository.selectProfile(onlyProfile.profileIndex)
                SyncManager.pullAllForProfile(onlyProfile.profileIndex)
                gateScreen = AppGateScreen.Main.name
                autoSkipProfileSelection = false
            }
        }

        fun closeProfileEdit() {
            if (profileEditReturnTarget == ProfileEditReturnTarget.ProfileSettings.name) {
                pendingMainInitialTabName = AppScreenTab.Settings.name
                pendingMainSettingsPageName = SettingsPage.Profile.name
                gateScreen = AppGateScreen.Main.name
            } else {
                gateScreen = AppGateScreen.ProfileSelection.name
            }
        }

        AnimatedContent(
            targetState = gateScreen,
            label = "app_gate",
            transitionSpec = {
                (fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.94f))
                    .togetherWith(fadeOut(tween(250)))
            },
        ) { currentGate ->
            when (currentGate) {
                AppGateScreen.Loading.name -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.nuvio.colors.background),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.nuvio.colors.accent)
                    }
                }
                AppGateScreen.Auth.name -> {
                    AuthScreen(modifier = Modifier.fillMaxSize())
                }
                AppGateScreen.ProfileSelection.name -> {
                    PlatformBackHandler(enabled = gateScreen == AppGateScreen.ProfileSelection.name) {
                        if (!autoSkipProfileSelection) {
                            gateScreen = AppGateScreen.Main.name
                        }
                    }
                    ProfileSelectionScreen(
                        onProfileSelected = { profile ->
                            ProfileRepository.selectProfile(profile.profileIndex)
                            if (authState is AuthState.Authenticated) {
                                SyncManager.pullAllForProfile(profile.profileIndex)
                            }
                            gateScreen = AppGateScreen.Main.name
                        },
                        onEditProfile = { profile ->
                            editingProfile = profile
                            isNewProfile = false
                            profileEditReturnTarget = ProfileEditReturnTarget.ProfileSelection.name
                            gateScreen = AppGateScreen.ProfileEdit.name
                        },
                        onAddProfile = {
                            editingProfile = null
                            isNewProfile = true
                            profileEditReturnTarget = ProfileEditReturnTarget.ProfileSelection.name
                            gateScreen = AppGateScreen.ProfileEdit.name
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                AppGateScreen.ProfileEdit.name -> {
                    PlatformBackHandler(enabled = gateScreen == AppGateScreen.ProfileEdit.name) {
                        closeProfileEdit()
                    }
                    ProfileEditScreen(
                        profile = editingProfile,
                        onBack = ::closeProfileEdit,
                        onSaved = ::closeProfileEdit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                AppGateScreen.Main.name -> {
                    MainAppContent(
                        initialSelectedTab = pendingMainInitialTabName
                            ?.let { runCatching { AppScreenTab.valueOf(it) }.getOrNull() }
                            ?: AppScreenTab.Home,
                        initialSettingsPageName = pendingMainSettingsPageName,
                        onInitialNavigationConsumed = {
                            pendingMainInitialTabName = null
                            pendingMainSettingsPageName = null
                        },
                        onSwitchProfile = {
                            autoSkipProfileSelection = false
                            gateScreen = AppGateScreen.ProfileSelection.name
                        },
                        onEditProfile = {
                            val activeProfile = ProfileRepository.state.value.activeProfile
                            if (activeProfile != null) {
                                editingProfile = activeProfile
                                isNewProfile = false
                                profileEditReturnTarget = ProfileEditReturnTarget.ProfileSettings.name
                                gateScreen = AppGateScreen.ProfileEdit.name
                            } else {
                                autoSkipProfileSelection = false
                                gateScreen = AppGateScreen.ProfileSelection.name
                            }
                        },
                    )
                }
            }
        }

        LocalCrashReportDialog(
            report = pendingCrashReport,
            onDismiss = { report ->
                CrashDiagnostics.dismiss(report.id)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun MainAppContent(
    initialSelectedTab: AppScreenTab = AppScreenTab.Home,
    initialSettingsPageName: String? = null,
    onInitialNavigationConsumed: () -> Unit = {},
    onSwitchProfile: () -> Unit = {},
    onEditProfile: () -> Unit = {},
) {
        val navController = rememberNavController()
        val appUpdaterController = rememberAppUpdaterController()
        remember {
            EpisodeReleaseNotificationsRepository.ensureLoaded()
        }
        remember {
            CollectionSyncService.startObserving()
        }
        remember {
            ProfileSettingsSync.startObserving()
        }
        val hapticFeedback = LocalHapticFeedback.current
        val focusManager = LocalFocusManager.current
        val uriHandler = LocalUriHandler.current
        val coroutineScope = rememberCoroutineScope()
        var selectedTab by rememberSaveable { mutableStateOf(initialSelectedTab) }
        var searchFocusRequestCount by remember { mutableStateOf(0) }
        val homeScrollToTopRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        val searchScrollToTopRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        val libraryScrollToTopRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        val settingsRootActionRequests = remember { MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
        var nativeProfileSwitcherVisible by remember { mutableStateOf(false) }
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val liquidGlassNativeTabBarEnabled by remember {
            ThemeSettingsRepository.liquidGlassNativeTabBarEnabled
        }.collectAsStateWithLifecycle()
        val liquidGlassAutoHideOnScrollEnabled by remember {
            ThemeSettingsRepository.liquidGlassAutoHideOnScrollEnabled
        }.collectAsStateWithLifecycle()
        val nuvioEnhancedSettings by remember {
            NuvioEnhancedSettingsRepository.ensureLoaded()
            NuvioEnhancedSettingsRepository.uiState
        }.collectAsStateWithLifecycle()
        LaunchedEffect(nuvioEnhancedSettings.statusBarVisible) {
            AppSystemUiController.setStatusBarVisible(nuvioEnhancedSettings.statusBarVisible)
        }
        val liveTvEnabled = nuvioEnhancedSettings.liveTvEnabled
        val liquidGlassNativeTabBarSupported = remember { isLiquidGlassNativeTabBarSupported() }
        var showExitConfirmation by rememberSaveable { mutableStateOf(false) }
        var selectedPosterActionTarget by remember { mutableStateOf<PosterActionTarget?>(null) }
        var showPosterTrackerSheet by remember { mutableStateOf<PosterActionTarget?>(null) }
        var selectedPosterAnchor by remember { mutableStateOf<PosterZoomAnchor?>(null) }
        val posterOverlayHazeState = rememberHazeState()
        var selectedContinueWatchingForActions by remember { mutableStateOf<ContinueWatchingItem?>(null) }
        var requestedSettingsPageName by rememberSaveable { mutableStateOf(initialSettingsPageName) }
        var showLibraryListPicker by remember { mutableStateOf(false) }
        var pickerItem by remember { mutableStateOf<LibraryItem?>(null) }
        var pickerTitle by remember { mutableStateOf("") }
        var pickerTabs by remember { mutableStateOf<List<TraktListTab>>(emptyList()) }
        var pickerMembership by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
        var pickerPending by remember { mutableStateOf(false) }
        var pickerError by remember { mutableStateOf<String?>(null) }
        val addonsUiState by remember {
            AddonRepository.initialize()
            AddonRepository.uiState
        }.collectAsStateWithLifecycle()
        val libraryUiState by remember {
            LibraryRepository.ensureLoaded()
            LibraryRepository.uiState
        }.collectAsStateWithLifecycle()
        val authState by AuthRepository.state.collectAsStateWithLifecycle()
        LaunchedEffect(initialSelectedTab, initialSettingsPageName) {
            if (initialSelectedTab != AppScreenTab.Home || initialSettingsPageName != null) {
                selectedTab = initialSelectedTab
                requestedSettingsPageName = initialSettingsPageName
            }
            onInitialNavigationConsumed()
        }
        val openPosterActions: (PosterActionTarget) -> Unit = { target ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            focusManager.clearFocus(force = true)
            selectedPosterAnchor = PosterZoomAnchorHolder.consume()
            coroutineScope.launch {
                withFrameNanos { }
                selectedPosterActionTarget = target
            }
        }
        val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
        val profileAvatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
        val launchOverlayProfileColor = remember(profileState.activeProfile, profileState.profiles) {
            val sourceProfile = profileState.activeProfile ?: profileState.profiles.firstOrNull()
            sourceProfile?.avatarColorHex?.let(::parseHexColor) ?: Color(0xFF1E88E5)
        }
        val launchOverlayProfile = remember(profileState.activeProfile, profileState.profiles) {
            profileState.activeProfile ?: profileState.profiles.firstOrNull()
        }
        val launchOverlayBackgroundUrl = remember(profileState.activeProfile, profileState.profiles) {
            val sourceProfile = profileState.activeProfile ?: profileState.profiles.firstOrNull()
            sourceProfile?.let(::profileBackgroundImageUrl)
        }
        val launchOverlayAvatarUrl = remember(launchOverlayProfile, profileAvatars) {
            val profile = launchOverlayProfile ?: return@remember null
            val avatarItem = profile.avatarId?.let { avatarId ->
                profileAvatars.find { it.id == avatarId }
            }
            profileAvatarImageUrl(profile, avatarItem)
        }
    val playerSettingsUiState by remember {
        PlayerSettingsRepository.ensureLoaded()
        PlayerSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val p2pSettingsUiState by remember {
        P2pSettingsRepository.ensureLoaded()
        P2pSettingsRepository.uiState
    }.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val downloadsUiState by remember {
        DownloadsRepository.ensureLoaded()
        DownloadsRepository.uiState
    }.collectAsStateWithLifecycle()
    val networkStatusUiState by remember {
        NetworkStatusRepository.uiState
    }.collectAsStateWithLifecycle()
    val downloadedProviderLabel = stringResource(Res.string.provider_downloaded)
    val externalPlayerNotConfiguredText = stringResource(Res.string.external_player_not_configured)
    val externalPlayerUnavailableText = stringResource(Res.string.external_player_unavailable)
    val externalPlayerFailedText = stringResource(Res.string.external_player_failed)
    val cloudLibraryPlayFailedText = stringResource(Res.string.cloud_library_play_failed)
    val cloudLibraryPlayDisabledText = stringResource(Res.string.cloud_library_play_disabled)
    val cloudLibraryPlayNotConnectedText = stringResource(Res.string.cloud_library_play_not_connected)
    val nativeTabHomeTitle = stringResource(Res.string.compose_nav_home)
    val nativeTabSearchTitle = stringResource(Res.string.compose_nav_search)
    val nativeTabLiveTvTitle = stringResource(Res.string.compose_nav_live_tv)
    val nativeTabLibraryTitle = stringResource(Res.string.compose_nav_library)
    val nativeTabProfileTitle = stringResource(Res.string.compose_nav_profile)
    val isTraktLibrarySource = libraryUiState.sourceMode == LibrarySourceMode.TRAKT
    var initialHomeReady by rememberSaveable { mutableStateOf(false) }
    var nativeTabScrollVisible by rememberSaveable { mutableStateOf(true) }
    var offlineLaunchRouteHandled by rememberSaveable { mutableStateOf(false) }
    var networkToastBaselineReady by rememberSaveable { mutableStateOf(false) }
    var lastNetworkToastCondition by rememberSaveable { mutableStateOf(NetworkCondition.Unknown.name) }
    var discordWelcomeDismissedThisSession by rememberSaveable { mutableStateOf(false) }

    fun handleRootTabClick(tab: AppScreenTab) {
        if (tab == AppScreenTab.LiveTv && !liveTvEnabled) {
            return
        }
        val wasAlreadySelected = selectedTab == tab
        val tabsRouteActive = currentBackStackEntry?.destination?.hasRoute<TabsRoute>() == true
        selectedTab = tab

        if (!tabsRouteActive) {
            navController.navigate(TabsRoute) {
                launchSingleTop = true
                restoreState = true
                popUpTo<TabsRoute> {
                    inclusive = false
                    saveState = true
                }
            }
            return
        }

        if (!wasAlreadySelected) return

        when (tab) {
            AppScreenTab.Home -> homeScrollToTopRequests.tryEmit(Unit)
            AppScreenTab.Search -> {
                searchFocusRequestCount++
                searchScrollToTopRequests.tryEmit(Unit)
            }
            AppScreenTab.LiveTv -> Unit
            AppScreenTab.Library -> libraryScrollToTopRequests.tryEmit(Unit)
            AppScreenTab.Settings -> settingsRootActionRequests.tryEmit(Unit)
        }
    }

    fun dismissDiscordWelcomeForNow() {
        discordWelcomeDismissedThisSession = true
    }

    fun hideDiscordWelcomePermanently() {
        discordWelcomeDismissedThisSession = true
        NuvioEnhancedSettingsRepository.markDiscordWelcomeSeen()
    }

    fun openDiscordWelcome() {
        hideDiscordWelcomePermanently()
        uriHandler.openUri(NuvioDiscordInviteUrl)
    }

    LaunchedEffect(liquidGlassNativeTabBarSupported, liquidGlassNativeTabBarEnabled, liveTvEnabled) {
        NativeTabBridge.requestedTabs.collectLatest { requestedTab ->
            if (liquidGlassNativeTabBarSupported && liquidGlassNativeTabBarEnabled) {
                val appTab = requestedTab.toAppScreenTab()
                if (appTab != AppScreenTab.LiveTv || liveTvEnabled) {
                    handleRootTabClick(appTab)
                }
            }
        }
    }

    LaunchedEffect(liquidGlassNativeTabBarSupported, liquidGlassNativeTabBarEnabled) {
        NativeTabBridge.profileTabLongPresses.collectLatest {
            if (liquidGlassNativeTabBarSupported && liquidGlassNativeTabBarEnabled) {
                nativeProfileSwitcherVisible = true
            }
        }
    }

    LaunchedEffect(
        nativeTabHomeTitle,
        nativeTabSearchTitle,
        nativeTabLiveTvTitle,
        nativeTabLibraryTitle,
        nativeTabProfileTitle,
    ) {
        NativeTabBridge.publishTabTitles(
            home = nativeTabHomeTitle,
            search = nativeTabSearchTitle,
            liveTv = nativeTabLiveTvTitle,
            library = nativeTabLibraryTitle,
            profile = nativeTabProfileTitle,
        )
    }

    LaunchedEffect(selectedTab) {
        NativeTabBridge.publishSelectedTab(selectedTab.toNativeNavigationTab())
        nativeTabScrollVisible = true
        if (selectedTab != AppScreenTab.Search) {
            searchFocusRequestCount = 0
        }
    }

    LaunchedEffect(liquidGlassAutoHideOnScrollEnabled, liquidGlassNativeTabBarEnabled) {
        if (!liquidGlassAutoHideOnScrollEnabled || !liquidGlassNativeTabBarEnabled) {
            nativeTabScrollVisible = true
        }
    }

    fun setNativeTabScrollVisibilityFromGesture(visible: Boolean) {
        if (nativeTabScrollVisible == visible) return
        nativeTabScrollVisible = visible
    }

    val nativeTabAutoHideScrollConnection = remember(
        liquidGlassNativeTabBarSupported,
        liquidGlassNativeTabBarEnabled,
        liquidGlassAutoHideOnScrollEnabled,
        initialHomeReady,
    ) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (
                    !liquidGlassNativeTabBarSupported ||
                    !liquidGlassNativeTabBarEnabled ||
                    !liquidGlassAutoHideOnScrollEnabled ||
                    !initialHomeReady
                ) {
                    return Offset.Zero
                }

                val scrollDeltaY = consumed.y + available.y
                when {
                    scrollDeltaY < -12f -> setNativeTabScrollVisibilityFromGesture(false)
                    scrollDeltaY > 12f -> setNativeTabScrollVisibilityFromGesture(true)
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(liveTvEnabled, selectedTab) {
        NativeTabBridge.publishLiveTvEnabled(liveTvEnabled)
        if (!liveTvEnabled && selectedTab == AppScreenTab.LiveTv) {
            selectedTab = AppScreenTab.Home
        }
    }

    var profileSwitchLoading by remember { mutableStateOf(false) }

    val nativeTabBarVisibleForCurrentRoute =
        liquidGlassNativeTabBarSupported &&
            liquidGlassNativeTabBarEnabled &&
            initialHomeReady &&
            (!liquidGlassAutoHideOnScrollEnabled || nativeTabScrollVisible) &&
            !profileSwitchLoading &&
            currentBackStackEntry?.destination?.hasRoute<TabsRoute>() == true

    LaunchedEffect(nativeTabBarVisibleForCurrentRoute) {
        NativeTabBridge.publishTabBarVisible(nativeTabBarVisibleForCurrentRoute)
    }

    DisposableEffect(Unit) {
        onDispose {
            NativeTabBridge.publishTabBarVisible(false)
        }
    }

    LaunchedEffect(Unit) {
        NetworkStatusRepository.ensureStarted()
        EpisodeReleaseNotificationsRepository.refreshAsync()
        kotlinx.coroutines.delay(1_500)
        if (!initialHomeReady) {
            initialHomeReady = true
        }
    }

    LaunchedEffect(Unit) {
        AppForegroundMonitor.events().collect {
            NetworkStatusRepository.requestForegroundRefresh()
        }
    }

    LaunchedEffect(networkStatusUiState.condition) {
        val condition = networkStatusUiState.condition
        if (!networkToastBaselineReady) {
            networkToastBaselineReady = true
            lastNetworkToastCondition = condition.name
            return@LaunchedEffect
        }

        val previousConditionName = lastNetworkToastCondition
        if (previousConditionName == condition.name) return@LaunchedEffect

        when (condition) {
            NetworkCondition.NoInternet -> {
                NuvioToastController.show(getString(Res.string.network_no_internet_connection))
            }

            NetworkCondition.ServersUnreachable -> {
                NuvioToastController.show(getString(Res.string.network_cannot_reach_servers))
            }

            NetworkCondition.Online -> {
                if (
                    previousConditionName == NetworkCondition.NoInternet.name ||
                    previousConditionName == NetworkCondition.ServersUnreachable.name
                ) {
                    NuvioToastController.show(getString(Res.string.network_back_online))
                }
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }

        lastNetworkToastCondition = condition.name
    }

    LaunchedEffect(
        initialHomeReady,
        offlineLaunchRouteHandled,
        networkStatusUiState.condition,
        downloadsUiState.completedItems,
    ) {
        if (!initialHomeReady || offlineLaunchRouteHandled) return@LaunchedEffect

        when (networkStatusUiState.condition) {
            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> return@LaunchedEffect

            NetworkCondition.Online -> {
                offlineLaunchRouteHandled = true
            }

            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                offlineLaunchRouteHandled = true
                val hasPlayableDownload = downloadsUiState.completedItems.any {
                    DownloadsRepository.playableLocalFileUri(it) != null
                }
                if (hasPlayableDownload) {
                    selectedTab = AppScreenTab.Settings
                    navController.navigate(DownloadsSettingsRoute) {
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    LaunchedEffect(authState, profileState.activeProfile?.profileIndex) {
        if (!RealtimeSyncConfig.ENABLED) {
            RealtimeSyncInvalidationService.stop()
            return@LaunchedEffect
        }

        val authenticatedState = authState as? AuthState.Authenticated ?: return@LaunchedEffect
        if (authenticatedState.isAnonymous) return@LaunchedEffect

        val activeProfileId = profileState.activeProfile?.profileIndex ?: return@LaunchedEffect
        RealtimeSyncInvalidationService.start(
            userId = authenticatedState.userId,
            profileId = activeProfileId,
        )
    }

    DisposableEffect(authState, profileState.activeProfile?.profileIndex) {
        val authenticatedState = authState as? AuthState.Authenticated
        if (
            !RealtimeSyncConfig.ENABLED ||
            authenticatedState == null ||
            authenticatedState.isAnonymous ||
            profileState.activeProfile == null
        ) {
            RealtimeSyncInvalidationService.stop()
        }
        onDispose {
            RealtimeSyncInvalidationService.stop()
        }
    }

    LaunchedEffect(authState, profileState.activeProfile?.profileIndex) {
        val authenticatedState = authState as? AuthState.Authenticated ?: return@LaunchedEffect
        if (authenticatedState.isAnonymous) return@LaunchedEffect

        val activeProfileId = profileState.activeProfile?.profileIndex ?: return@LaunchedEffect
        AppForegroundMonitor.events().collect {
            SyncManager.requestForegroundPull(activeProfileId, force = true)
        }
    }
    var resumePromptItem by remember { mutableStateOf<ContinueWatchingItem?>(null) }
    var lastExternalPlayerLaunch by remember { mutableStateOf<PlayerLaunch?>(null) }
    val activePlaybackProfileId = profileState.activeProfile?.profileIndex ?: ProfileRepository.activeProfileId
    val launchExternalPlayer = rememberExternalPlayerLauncher { result ->
        if (result != null && result.positionMs > 0L) {
            coroutineScope.launch {
                val durationMs = result.durationMs
                val progressPercent = if (durationMs != null && durationMs > 0L) {
                    (result.positionMs.toFloat() / durationMs.toFloat() * 100f).coerceIn(0f, 100f)
                } else {
                    null
                }
                val playerLaunch = lastExternalPlayerLaunch
                if (playerLaunch?.randomEpisodeMode == true) {
                    return@launch
                }
                if (TraktAuthRepository.isAuthenticated.value && progressPercent != null && playerLaunch != null) {
                    val scrobbleItem = TraktScrobbleRepository.buildItem(
                        contentType = playerLaunch.parentMetaType,
                        parentMetaId = playerLaunch.parentMetaId,
                        videoId = playerLaunch.videoId,
                        title = playerLaunch.title,
                        seasonNumber = playerLaunch.seasonNumber,
                        episodeNumber = playerLaunch.episodeNumber,
                        episodeTitle = playerLaunch.episodeTitle,
                    )
                    if (scrobbleItem != null) {
                        runCatching {
                            TraktScrobbleRepository.scrobbleStop(
                                profileId = playerLaunch.profileId,
                                item = scrobbleItem,
                                progressPercent = progressPercent,
                            )
                        }
                    }
                }
                playerLaunch?.let { playerLaunch ->
                    val session = WatchProgressPlaybackSession(
                        profileId = playerLaunch.profileId,
                        contentType = playerLaunch.contentType ?: playerLaunch.parentMetaType,
                        parentMetaId = playerLaunch.parentMetaId,
                        parentMetaType = playerLaunch.parentMetaType,
                        videoId = playerLaunch.videoId ?: playerLaunch.parentMetaId,
                        title = playerLaunch.title,
                        logo = playerLaunch.logo,
                        poster = playerLaunch.poster,
                        background = playerLaunch.background,
                        seasonNumber = playerLaunch.seasonNumber,
                        episodeNumber = playerLaunch.episodeNumber,
                        episodeTitle = playerLaunch.episodeTitle,
                        episodeThumbnail = playerLaunch.episodeThumbnail,
                        providerName = playerLaunch.providerName,
                        providerAddonId = playerLaunch.providerAddonId,
                        lastStreamTitle = playerLaunch.streamTitle,
                        lastSourceUrl = playerLaunch.sourceUrl,
                    )
                    val snapshot = PlayerPlaybackSnapshot(
                        isLoading = false,
                        isPlaying = false,
                        isEnded = !result.endedByUser,
                        durationMs = durationMs ?: 0L,
                        positionMs = result.positionMs,
                    )
                    WatchProgressRepository.upsertPlaybackProgress(
                        session = session,
                        snapshot = snapshot,
                    )
                }
            }
        }
    }
    val continueWatchingPreferencesUiState by remember {
        ContinueWatchingPreferencesRepository.ensureLoaded()
        ContinueWatchingPreferencesRepository.uiState
    }.collectAsStateWithLifecycle()

    LaunchedEffect(
        initialHomeReady,
        profileSwitchLoading,
        profileState.activeProfile?.profileIndex,
        continueWatchingPreferencesUiState.showResumePromptOnLaunch,
    ) {
        if (!initialHomeReady || profileSwitchLoading) return@LaunchedEffect
        if (resumePromptItem != null) return@LaunchedEffect
        if (continueWatchingPreferencesUiState.showResumePromptOnLaunch) {
            resumePromptItem = ResumePromptRepository.consumeResumePrompt()
        }
    }

    LaunchedEffect(currentBackStackEntry?.destination) {
        val inPlaybackFlow = currentBackStackEntry?.destination?.hasRoute<StreamRoute>() == true ||
            currentBackStackEntry?.destination?.hasRoute<PlayerRoute>() == true
        if (inPlaybackFlow) {
            resumePromptItem = null
        }
    }

        LaunchedEffect(navController) {
            AppDeepLinkRepository.pendingDeepLink.collectLatest { deepLink ->
                when (deepLink) {
                    is AppDeepLink.Meta -> {
                        selectedTab = AppScreenTab.Home
                        navController.navigate(DetailRoute(type = deepLink.type, id = deepLink.id)) {
                            launchSingleTop = true
                        }
                        AppDeepLinkRepository.markConsumed(deepLink)
                    }

                    is AppDeepLink.AddonInstall -> {
                        selectedTab = AppScreenTab.Settings
                        navController.navigate(AddonsSettingsRoute) {
                            launchSingleTop = true
                        }
                        NuvioToastController.show(getString(Res.string.addons_modal_checking_title))
                        AddonRepository.initialize()
                        when (val result = AddonRepository.addAddon(deepLink.manifestUrl)) {
                            is AddAddonResult.Success -> {
                                NuvioToastController.show(
                                    getString(Res.string.addons_modal_success_message, result.manifest.name),
                                )
                            }

                            is AddAddonResult.Error -> {
                                NuvioToastController.show(result.message)
                            }
                        }
                        AppDeepLinkRepository.markConsumed(deepLink)
                    }

                    AppDeepLink.Downloads -> {
                        selectedTab = AppScreenTab.Settings
                        navController.navigate(DownloadsSettingsRoute) {
                            launchSingleTop = true
                        }
                        AppDeepLinkRepository.markConsumed(deepLink)
                    }

                    null -> Unit
                }
            }
        }

        suspend fun openExternalPlayback(launch: PlayerLaunch): Boolean {
            lastExternalPlayerLaunch = launch

            // Persist binge group for subsequent episode plays (same as internal player)
            val bingeGroup = launch.bingeGroup
            if (bingeGroup != null && launch.parentMetaId.isNotBlank()) {
                BingeGroupCacheRepository.save(launch.parentMetaId, bingeGroup)
            }

            val baseRequest = launch.toExternalPlayerPlaybackRequest()
            val shouldForwardSubtitles = playerSettingsUiState.externalPlayerForwardSubtitles &&
                !playerSettingsUiState.preferredSubtitleLanguage.equals(SubtitleLanguageOption.NONE, ignoreCase = true)
            val shouldSendSkipSegments = playerSettingsUiState.externalPlayerSendSkipSegments
            if (shouldForwardSubtitles) {
                StreamsRepository.setOverlayVisible(true, getString(Res.string.streams_loading_subtitles))
            } else if (shouldSendSkipSegments) {
                StreamsRepository.setOverlayVisible(true, getString(Res.string.streams_loading_skip_segments))
            }
            val enrichedRequest = prepareExternalPlayerLaunch(
                request = baseRequest,
                type = launch.contentType ?: launch.parentMetaType,
                videoId = launch.videoId ?: launch.parentMetaId,
                forwardSubtitles = playerSettingsUiState.externalPlayerForwardSubtitles,
                sendSkipSegments = shouldSendSkipSegments,
                preferredLanguage = playerSettingsUiState.preferredSubtitleLanguage,
                secondaryLanguage = playerSettingsUiState.secondaryPreferredSubtitleLanguage,
                onOverlayMessage = { _ -> },
            )
            StreamsRepository.setOverlayVisible(false)
            return when (
                val intentResult = ExternalPlayerPlatform.buildIntent(
                    request = enrichedRequest,
                    playerId = playerSettingsUiState.externalPlayerId,
                )
            ) {
                is ExternalPlayerIntentResult.Success -> {
                    val launched = launchExternalPlayer(intentResult)
                    if (!launched) {
                        NuvioToastController.show(externalPlayerFailedText)
                    }
                    launched
                }
                ExternalPlayerIntentResult.NotConfigured -> {
                    NuvioToastController.show(externalPlayerNotConfiguredText)
                    false
                }
                ExternalPlayerIntentResult.Failed -> {
                    NuvioToastController.show(externalPlayerFailedText)
                    false
                }
            }
        }

        suspend fun launchPlayer(launch: PlayerLaunch) {
            if (playerSettingsUiState.externalPlayerEnabled && launch.torrentInfoHash == null) {
                openExternalPlayback(launch)
                return
            }
            val launchId = PlayerLaunchStore.put(launch)
            navController.navigate(PlayerRoute(launchId = launchId))
        }

        suspend fun openDownloadedItem(item: DownloadItem) {
            val sourceUrl = DownloadsRepository.playableLocalFileUri(item) ?: return
            val resumeEntry = item.videoId
                .takeIf { it.isNotBlank() }
                ?.let(WatchProgressRepository::progressForVideo)
                ?.takeIf { it.isResumable }

            launchPlayer(
                PlayerLaunch(
                    profileId = activePlaybackProfileId,
                    title = item.title,
                    sourceUrl = sourceUrl,
                    sourceHeaders = emptyMap(),
                    sourceResponseHeaders = emptyMap(),
                    externalSubtitles = emptyList(),
                    streamType = null,
                    logo = item.logo,
                    poster = item.poster,
                    background = item.background,
                    seasonNumber = item.seasonNumber,
                    episodeNumber = item.episodeNumber,
                    episodeTitle = item.episodeTitle,
                    episodeThumbnail = item.episodeThumbnail,
                    streamTitle = item.streamTitle,
                    streamSubtitle = item.streamSubtitle,
                    providerName = item.providerName,
                    providerAddonId = item.providerAddonId,
                    contentType = item.contentType,
                    videoId = item.videoId,
                    parentMetaId = item.parentMetaId,
                    parentMetaType = item.parentMetaType,
                    initialPositionMs = resumeEntry?.lastPositionMs?.takeIf { it > 0L } ?: 0L,
                    initialProgressFraction = resumeEntry?.progressFraction?.takeIf { it > 0f },
                ),
            )
        }

        suspend fun openLiveTvChannel(
            channel: LiveTvChannel,
            providerName: String = "Live TV",
            recordRecent: Boolean = true,
        ) {
            if (recordRecent) {
                LiveTvRepository.recordRecentChannel(channel)
            }
            launchPlayer(
                PlayerLaunch(
                    profileId = activePlaybackProfileId,
                    title = channel.name,
                    sourceUrl = channel.streamUrl,
                    sourceHeaders = channel.headers,
                    streamType = channel.streamType,
                    logo = channel.logoUrl,
                    streamTitle = channel.name,
                    streamSubtitle = channel.group.takeIf { it.isNotBlank() },
                    providerName = providerName,
                    providerAddonId = "live-tv",
                    contentType = "live-tv",
                    videoId = channel.id,
                    parentMetaId = channel.id.ifBlank { channel.streamUrl },
                    parentMetaType = "live-tv",
                ),
            )
        }

        LaunchedEffect(activePlaybackProfileId, liveTvEnabled, playerSettingsUiState.externalPlayerEnabled, p2pSettingsUiState.p2pEnabled) {
            LiveTvIncomingSourceRepository.requests.collectLatest { request ->
                when (request) {
                    is LiveTvIncomingSource.DirectStream -> {
                        val channel = LiveTvChannel(
                            id = "shared:${request.url}",
                            name = request.title.ifBlank { "Shared stream" },
                            streamUrl = request.url,
                            group = "Shared",
                            headers = request.headers,
                        )
                        openLiveTvChannel(channel, providerName = "Shared", recordRecent = false)
                    }

                    is LiveTvIncomingSource.SourceUrl -> {
                        val result = LiveTvRepository.load(request.url)
                        result.onSuccess { channels ->
                            if (channels.size == 1) {
                                openLiveTvChannel(channels.first(), providerName = "Shared")
                            } else if (liveTvEnabled) {
                                handleRootTabClick(AppScreenTab.LiveTv)
                            } else {
                                NuvioToastController.show("Live TV tab is disabled.")
                            }
                        }.onFailure { error ->
                            NuvioToastController.show(error.message ?: "Unable to open shared playlist.")
                        }
                    }

                    is LiveTvIncomingSource.PlaylistData -> {
                        val result = LiveTvRepository.loadLocalPlaylist(request.fileName, request.data)
                        result.onSuccess { channels ->
                            if (channels.size == 1) {
                                openLiveTvChannel(channels.first(), providerName = "Shared")
                            } else if (liveTvEnabled) {
                                handleRootTabClick(AppScreenTab.LiveTv)
                            } else {
                                NuvioToastController.show("Live TV tab is disabled.")
                            }
                        }.onFailure { error ->
                            NuvioToastController.show(error.message ?: "Unable to open shared playlist.")
                        }
                    }

                    is LiveTvIncomingSource.Magnet -> {
                        if (!p2pSettingsUiState.p2pEnabled) {
                            NuvioToastController.show("Enable P2P playback to open magnet links.")
                            return@collectLatest
                        }
                        val title = "Magnet stream"
                        launchPlayer(
                            PlayerLaunch(
                                profileId = activePlaybackProfileId,
                                title = title,
                                sourceUrl = "torrent://${request.infoHash}",
                                streamTitle = title,
                                streamSubtitle = request.infoHash,
                                providerName = "Magnet",
                                providerAddonId = "shared-magnet",
                                contentType = "shared-stream",
                                videoId = request.infoHash,
                                parentMetaId = request.infoHash,
                                parentMetaType = "shared-stream",
                                torrentInfoHash = request.infoHash,
                                torrentTrackers = request.trackers,
                            ),
                        )
                    }
                }
            }
        }

        suspend fun launchCloudLibraryFile(
            item: CloudLibraryItem,
            file: CloudLibraryFile,
            resumePositionMs: Long? = null,
            resumeProgressFraction: Float? = null,
            startFromBeginning: Boolean = false,
        ): Boolean {
            return when (
                val resolved = CloudLibraryRepository.resolvePlayback(
                    item = item,
                    file = file,
                )
            ) {
                is CloudLibraryPlaybackResult.Success -> {
                    val playbackTitle = resolved.filename
                        ?.takeIf { it.isNotBlank() }
                        ?: file.name.ifBlank { item.name }
                    val playerLaunch = PlayerLaunch(
                        profileId = activePlaybackProfileId,
                        title = playbackTitle,
                        sourceUrl = resolved.url,
                        streamTitle = playbackTitle,
                        streamSubtitle = item.name.takeIf { it != playbackTitle },
                        providerName = item.providerName,
                        providerAddonId = "cloud:${item.providerId}",
                        poster = item.providerPosterUrl(),
                        contentType = CloudLibraryContentType,
                        videoId = item.playbackVideoId(file),
                        parentMetaId = item.stableKey,
                        parentMetaType = CloudLibraryContentType,
                        initialPositionMs = if (startFromBeginning) 0L else (resumePositionMs ?: 0L),
                        initialProgressFraction = if (startFromBeginning) null else resumeProgressFraction,
                    )
                    if (playerSettingsUiState.externalPlayerEnabled) {
                        openExternalPlayback(playerLaunch)
                        true
                    } else {
                        val launchId = PlayerLaunchStore.put(playerLaunch)
                        navController.navigate(PlayerRoute(launchId = launchId))
                        true
                    }
                }

                else -> false
            }
        }

        fun launchPlaybackWithDownloadPreference(
            type: String,
            videoId: String,
            parentMetaId: String,
            parentMetaType: String,
            title: String,
            logo: String?,
            poster: String?,
            background: String?,
            seasonNumber: Int?,
            episodeNumber: Int?,
            episodeTitle: String?,
            episodeThumbnail: String?,
            pauseDescription: String?,
            resumePositionMs: Long?,
            resumeProgressFraction: Float?,
            manualSelection: Boolean,
            startFromBeginning: Boolean,
            randomEpisodeMode: Boolean,
        ) {
            val targetResumePositionMs = if (startFromBeginning) 0L else (resumePositionMs ?: 0L)
            val targetResumeProgressFraction = if (startFromBeginning) null else resumeProgressFraction

            if (!manualSelection) {
                val downloadedItem = DownloadsRepository.findPlayableDownload(
                    parentMetaId = parentMetaId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    videoId = videoId,
                )
                val localSourceUrl = downloadedItem?.let(DownloadsRepository::playableLocalFileUri)
                if (!localSourceUrl.isNullOrBlank()) {
                    val playerLaunch = PlayerLaunch(
                        profileId = activePlaybackProfileId,
                        title = title,
                        sourceUrl = localSourceUrl,
                        sourceHeaders = emptyMap(),
                        sourceResponseHeaders = emptyMap(),
                        externalSubtitles = emptyList(),
                        logo = logo,
                        poster = poster,
                        background = background,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        episodeTitle = episodeTitle,
                        episodeThumbnail = episodeThumbnail,
                        streamTitle = downloadedItem.streamTitle.ifBlank { title },
                        streamSubtitle = downloadedItem.streamSubtitle,
                        pauseDescription = pauseDescription,
                        providerName = downloadedItem.providerName.ifBlank { downloadedProviderLabel },
                        providerAddonId = downloadedItem.providerAddonId,
                        contentType = type,
                        videoId = videoId,
                        parentMetaId = parentMetaId,
                        parentMetaType = parentMetaType,
                        initialPositionMs = targetResumePositionMs,
                        initialProgressFraction = targetResumeProgressFraction,
                        randomEpisodeMode = randomEpisodeMode,
                    )
                    if (playerSettingsUiState.externalPlayerEnabled) {
                        coroutineScope.launch { openExternalPlayback(playerLaunch) }
                        return
                    }
                    val launchId = PlayerLaunchStore.put(playerLaunch)
                    navController.navigate(PlayerRoute(launchId = launchId))
                    return
                }
            }

            val streamLaunchId = StreamLaunchStore.put(
                StreamLaunch(
                    profileId = activePlaybackProfileId,
                    type = type,
                    videoId = videoId,
                    parentMetaId = parentMetaId,
                    parentMetaType = parentMetaType,
                    title = title,
                    logo = logo,
                    poster = poster,
                    background = background,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    episodeTitle = episodeTitle,
                    episodeThumbnail = episodeThumbnail,
                    pauseDescription = pauseDescription,
                    resumePositionMs = if (startFromBeginning) 0L else resumePositionMs,
                    resumeProgressFraction = targetResumeProgressFraction,
                    manualSelection = manualSelection,
                    startFromBeginning = startFromBeginning,
                    randomEpisodeMode = randomEpisodeMode,
                ),
            )
            navController.navigate(
                StreamRoute(launchId = streamLaunchId),
            )
        }

        val onPlay: (String, String, String, String, String, String?, String?, String?, Int?, Int?, String?, String?, String?, Long?) -> Unit =
            { type, videoId, parentMetaId, parentMetaType, title, logo, poster, background, seasonNumber, episodeNumber, episodeTitle, episodeThumbnail, pauseDescription, resumePositionMs ->
                launchPlaybackWithDownloadPreference(
                    type = type,
                    videoId = videoId,
                    parentMetaId = parentMetaId,
                    parentMetaType = parentMetaType,
                    title = title,
                    logo = logo,
                    poster = poster,
                    background = background,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    episodeTitle = episodeTitle,
                    episodeThumbnail = episodeThumbnail,
                    pauseDescription = pauseDescription,
                    resumePositionMs = resumePositionMs,
                    resumeProgressFraction = null,
                    manualSelection = false,
                    startFromBeginning = false,
                    randomEpisodeMode = false,
                )
            }

        val onPlayManually: (String, String, String, String, String, String?, String?, String?, Int?, Int?, String?, String?, String?, Long?) -> Unit =
            { type, videoId, parentMetaId, parentMetaType, title, logo, poster, background, seasonNumber, episodeNumber, episodeTitle, episodeThumbnail, pauseDescription, resumePositionMs ->
                launchPlaybackWithDownloadPreference(
                    type = type,
                    videoId = videoId,
                    parentMetaId = parentMetaId,
                    parentMetaType = parentMetaType,
                    title = title,
                    logo = logo,
                    poster = poster,
                    background = background,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    episodeTitle = episodeTitle,
                    episodeThumbnail = episodeThumbnail,
                    pauseDescription = pauseDescription,
                    resumePositionMs = resumePositionMs,
                    resumeProgressFraction = null,
                    manualSelection = true,
                    startFromBeginning = false,
                    randomEpisodeMode = false,
                )
            }

        val onRandomPlay: (String, String, String, String, String, String?, String?, String?, Int?, Int?, String?, String?, String?, Long?) -> Unit =
            { type, videoId, parentMetaId, parentMetaType, title, logo, poster, background, seasonNumber, episodeNumber, episodeTitle, episodeThumbnail, pauseDescription, _ ->
                launchPlaybackWithDownloadPreference(
                    type = type,
                    videoId = videoId,
                    parentMetaId = parentMetaId,
                    parentMetaType = parentMetaType,
                    title = title,
                    logo = logo,
                    poster = poster,
                    background = background,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    episodeTitle = episodeTitle,
                    episodeThumbnail = episodeThumbnail,
                    pauseDescription = pauseDescription,
                    resumePositionMs = null,
                    resumeProgressFraction = null,
                    manualSelection = false,
                    startFromBeginning = true,
                    randomEpisodeMode = true,
                )
            }

        val onRandomPlayManually: (String, String, String, String, String, String?, String?, String?, Int?, Int?, String?, String?, String?, Long?) -> Unit =
            { type, videoId, parentMetaId, parentMetaType, title, logo, poster, background, seasonNumber, episodeNumber, episodeTitle, episodeThumbnail, pauseDescription, _ ->
                launchPlaybackWithDownloadPreference(
                    type = type,
                    videoId = videoId,
                    parentMetaId = parentMetaId,
                    parentMetaType = parentMetaType,
                    title = title,
                    logo = logo,
                    poster = poster,
                    background = background,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    episodeTitle = episodeTitle,
                    episodeThumbnail = episodeThumbnail,
                    pauseDescription = pauseDescription,
                    resumePositionMs = null,
                    resumeProgressFraction = null,
                    manualSelection = true,
                    startFromBeginning = true,
                    randomEpisodeMode = true,
                )
            }

        val onCatalogClick: (HomeCatalogSection) -> Unit = { section ->
            val launchId = CatalogLaunchStore.put(
                CatalogLaunch(
                    title = section.title,
                    subtitle = section.subtitle,
                    target = section.target,
                ),
            )
            navController.navigate(
                CatalogRoute(
                    launchId = launchId,
                ),
            )
        }

        val librarySectionSubtitle = if (libraryUiState.sourceMode == LibrarySourceMode.TRAKT) {
            stringResource(Res.string.compose_catalog_subtitle_trakt_library)
        } else {
            stringResource(Res.string.compose_catalog_subtitle_library)
        }

        val onLibrarySectionViewAllClick: (LibrarySection) -> Unit = { section ->
            val launchId = CatalogLaunchStore.put(
                CatalogLaunch(
                    title = section.displayTitle,
                    subtitle = librarySectionSubtitle,
                    target = CatalogTarget.Library(
                        contentType = section.items.firstOrNull()?.type ?: "movie",
                        sectionType = section.type,
                    ),
                ),
            )
            navController.navigate(
                CatalogRoute(
                    launchId = launchId,
                ),
            )
        }

        val openContinueWatching: (ContinueWatchingItem, Boolean, Boolean) -> Unit = { item, manualSelection, startFromBeginning ->
            resumePromptItem = null
            if (item.isCloudLibraryContinueWatchingItem()) {
                coroutineScope.launch {
                    when (
                        val lookup = CloudLibraryRepository.findPlaybackTargetForProgressResult(
                            contentId = item.parentMetaId,
                            videoId = item.videoId,
                        )
                    ) {
                        is CloudLibraryPlaybackTargetLookupResult.Found -> {
                            val launched = launchCloudLibraryFile(
                                item = lookup.target.item,
                                file = lookup.target.file,
                                resumePositionMs = item.resumePositionMs,
                                resumeProgressFraction = item.resumeProgressFraction,
                                startFromBeginning = startFromBeginning,
                            )
                            if (!launched) {
                                NuvioToastController.show(cloudLibraryPlayFailedText)
                            }
                        }

                        CloudLibraryPlaybackTargetLookupResult.Disabled -> {
                            NuvioToastController.show(cloudLibraryPlayDisabledText)
                        }

                        is CloudLibraryPlaybackTargetLookupResult.NotConnected -> {
                            val providerName = lookup.providerName?.takeIf { it.isNotBlank() }
                            NuvioToastController.show(
                                providerName?.let { name ->
                                    getString(Res.string.cloud_library_play_provider_not_connected, name)
                                }
                                    ?: cloudLibraryPlayNotConnectedText,
                            )
                        }

                        CloudLibraryPlaybackTargetLookupResult.NotFound -> {
                            NuvioToastController.show(cloudLibraryPlayFailedText)
                        }
                    }
                }
            } else {
                launchPlaybackWithDownloadPreference(
                    type = item.parentMetaType,
                    videoId = item.videoId,
                    parentMetaId = item.parentMetaId,
                    parentMetaType = item.parentMetaType,
                    title = item.title,
                    logo = item.logo,
                    poster = item.poster,
                    background = item.background,
                    seasonNumber = item.seasonNumber,
                    episodeNumber = item.episodeNumber,
                    episodeTitle = item.episodeTitle,
                    episodeThumbnail = item.episodeThumbnail,
                    pauseDescription = item.pauseDescription,
                    resumePositionMs = item.resumePositionMs,
                    resumeProgressFraction = item.resumeProgressFraction,
                    manualSelection = manualSelection,
                    startFromBeginning = startFromBeginning,
                    randomEpisodeMode = false,
                )
            }
        }

        val onContinueWatchingClick: (ContinueWatchingItem) -> Unit = { item ->
            openContinueWatching(item, false, false)
        }

        val onContinueWatchingStartFromBeginning: (ContinueWatchingItem) -> Unit = { item ->
            openContinueWatching(item, false, true)
        }

        val onContinueWatchingPlayManually: (ContinueWatchingItem) -> Unit = { item ->
            openContinueWatching(item, true, false)
        }

        val onContinueWatchingLongPress: (ContinueWatchingItem) -> Unit = { item ->
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            PosterZoomAnchorHolder.consume()
            selectedContinueWatchingForActions = item
        }

        AppUpdaterHost(
            controller = appUpdaterController,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (selectedPosterActionTarget != null) {
                            Modifier.hazeSource(state = posterOverlayHazeState)
                        } else {
                            Modifier
                        },
                    )
                    .background(MaterialTheme.nuvio.colors.background),
            ) {
            SharedTransitionLayout {
                NavHost(
                    navController = navController,
                    startDestination = TabsRoute,
                    modifier = Modifier.fillMaxSize(),
                ) {
                composable<TabsRoute> {
                    PlatformBackHandler(
                        enabled = true,
                        onBack = {
                            if (selectedTab != AppScreenTab.Home) {
                                selectedTab = AppScreenTab.Home
                            } else {
                                showExitConfirmation = !showExitConfirmation
                            }
                        },
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(nativeTabAutoHideScrollConnection),
                    ) {
                        val isTabletLayout = maxWidth >= 768.dp
                        val useNativeBottomTabs =
                            liquidGlassNativeTabBarSupported && liquidGlassNativeTabBarEnabled && initialHomeReady
                        val nativeTabSafeBottomPadding = nuvioBottomNavigationBarInsets()
                            .asPaddingValues()
                            .calculateBottomPadding()
                        val nativeProfileTabAnchorBottomPadding =
                            nativeTabSafeBottomPadding + NuvioTokens.Space.s10
                        val tabsRouteActive = currentBackStackEntry?.destination?.hasRoute<TabsRoute>() == true
                        val onProfileSelected: (NuvioProfile) -> Unit = { profile ->
                            nativeProfileSwitcherVisible = false
                            profileSwitchLoading = true
                            NativeTabBridge.publishTabBarVisible(false)
                            selectedTab = AppScreenTab.Home
                            ProfileRepository.selectProfile(profile.profileIndex)
                            com.nuvio.app.core.sync.SyncManager.pullAllForProfile(profile.profileIndex)
                        }

                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (initialHomeReady) 1f else 0f),
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets(0),
                            bottomBar = {
                                if (!isTabletLayout && !useNativeBottomTabs) {
                                    NuvioNavigationBar {
                                        NavItem(
                                            selected = selectedTab == AppScreenTab.Home,
                                            onClick = { handleRootTabClick(AppScreenTab.Home) },
                                            icon = Icons.Filled.Home,
                                            contentDescription = stringResource(Res.string.compose_nav_home),
                                        )
                                        NavItem(
                                            selected = selectedTab == AppScreenTab.Search,
                                            onClick = { handleRootTabClick(AppScreenTab.Search) },
                                            icon = Res.drawable.sidebar_search,
                                            contentDescription = stringResource(Res.string.compose_nav_search),
                                        )
                                        if (liveTvEnabled) {
                                            NavItem(
                                                selected = selectedTab == AppScreenTab.LiveTv,
                                                onClick = { handleRootTabClick(AppScreenTab.LiveTv) },
                                                icon = Icons.Filled.Tv,
                                                contentDescription = stringResource(Res.string.compose_nav_live_tv),
                                            )
                                        }
                                        NavItem(
                                            selected = selectedTab == AppScreenTab.Library,
                                            onClick = { handleRootTabClick(AppScreenTab.Library) },
                                            icon = Res.drawable.sidebar_library,
                                            contentDescription = stringResource(Res.string.compose_nav_library),
                                        )
                                        NavItem(
                                            selected = selectedTab == AppScreenTab.Settings,
                                            onClick = { handleRootTabClick(AppScreenTab.Settings) },
                                        ) {
                                            ProfileSwitcherTab(
                                                selected = selectedTab == AppScreenTab.Settings,
                                                onClick = { handleRootTabClick(AppScreenTab.Settings) },
                                                onProfileSelected = onProfileSelected,
                                                onAddProfileRequested = onSwitchProfile,
                                            )
                                        }
                                    }
                                }
                            },
                        ) { innerPadding ->
                            Box(modifier = Modifier.fillMaxSize()) {
                                CompositionLocalProvider(
                                    LocalNuvioBottomNavigationOverlayPadding provides if (useNativeBottomTabs) 49.dp else 0.dp,
                                ) {
                                    AppTabHost(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(innerPadding),
                                        selectedTab = selectedTab,
                                        searchFocusRequestCount = searchFocusRequestCount,
                                        rootActionsEnabled = tabsRouteActive,
                                        homeScrollToTopRequests = homeScrollToTopRequests,
                                        searchScrollToTopRequests = searchScrollToTopRequests,
                                        libraryScrollToTopRequests = libraryScrollToTopRequests,
                                        settingsRootActionRequests = settingsRootActionRequests,
                                        animateHomeCollectionGifs = tabsRouteActive,
                                        onCatalogClick = onCatalogClick,
                                        onPosterClick = { meta ->
                                            navController.navigate(DetailRoute(type = meta.type, id = meta.id))
                                        },
                                        onPosterLongClick = { meta ->
                                            openPosterActions(PosterActionTarget(preview = meta))
                                        },
                                        onLiveTvChannelClick = { channel ->
                                            coroutineScope.launch {
                                                openLiveTvChannel(channel)
                                            }
                                        },
                                        onLibraryPosterClick = { item ->
                                            navController.navigate(DetailRoute(type = item.type, id = item.id))
                                        },
                                        onLibraryPosterLongClick = { item, section ->
                                            openPosterActions(
                                                PosterActionTarget(
                                                    preview = item.toMetaPreview(),
                                                    libraryItem = item,
                                                    libraryListKey = section.type,
                                                ),
                                            )
                                        },
                                        onLibrarySectionViewAllClick = onLibrarySectionViewAllClick,
                                        onCloudFilePlay = { item, file ->
                                            coroutineScope.launch {
                                                val resumeItem = WatchProgressRepository
                                                    .progressForVideo(item.playbackVideoId(file))
                                                    ?.takeIf { it.isResumable }
                                                    ?.toContinueWatchingItem()
                                                if (
                                                    !launchCloudLibraryFile(
                                                        item = item,
                                                        file = file,
                                                        resumePositionMs = resumeItem?.resumePositionMs,
                                                        resumeProgressFraction = resumeItem?.resumeProgressFraction,
                                                    )
                                                ) {
                                                    NuvioToastController.show(cloudLibraryPlayFailedText)
                                                }
                                            }
                                        },
                                        onConnectCloudClick = {
                                            requestedSettingsPageName = "Debrid"
                                            selectedTab = AppScreenTab.Settings
                                        },
                                        onContinueWatchingClick = onContinueWatchingClick,
                                        onContinueWatchingLongPress = onContinueWatchingLongPress,
                                        onSwitchProfile = onSwitchProfile,
                                        onEditProfile = onEditProfile,
                                        onHomescreenSettingsClick = { navController.navigate(HomescreenSettingsRoute) },
                                        onMetaScreenSettingsClick = { navController.navigate(MetaScreenSettingsRoute) },
                                        onContinueWatchingSettingsClick = { navController.navigate(ContinueWatchingSettingsRoute) },
                                        onDownloadsSettingsClick = { navController.navigate(DownloadsSettingsRoute) },
                                        onLibraryDownloadClick = { item ->
                                            coroutineScope.launch { openDownloadedItem(item) }
                                        },
                                        onAddonsSettingsClick = { navController.navigate(AddonsSettingsRoute) },
                                        onPluginsSettingsClick = {
                                            if (AppFeaturePolicy.pluginsEnabled) {
                                                navController.navigate(PluginsSettingsRoute)
                                            }
                                        },
                                        onAccountSettingsClick = { navController.navigate(AccountSettingsRoute) },
                                        onSupportersContributorsSettingsClick = {
                                            if (AppFeaturePolicy.supportersContributorsPageEnabled) {
                                                navController.navigate(SupportersContributorsSettingsRoute)
                                            }
                                        },
                                        onLicensesAttributionsSettingsClick = {
                                            navController.navigate(LicensesAttributionsSettingsRoute)
                                        },
                                        onCheckForUpdatesClick = if (AppFeaturePolicy.inAppUpdaterEnabled) {
                                            {
                                                appUpdaterController.checkForUpdates(
                                                    force = true,
                                                    showNoUpdateFeedback = true,
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        onTestUpdateBannerClick = if (
                                            AppFeaturePolicy.inAppUpdaterEnabled && AppUpdaterPlatform.isDebugBuild
                                        ) {
                                            appUpdaterController::showDebugTestUpdate
                                        } else {
                                            null
                                        },
                                        onCollectionsSettingsClick = { navController.navigate(CollectionsRoute) },
                                        onFolderClick = { collectionId, folderId ->
                                            navController.navigate(FolderDetailRoute(collectionId = collectionId, folderId = folderId))
                                        },
                                        requestedSettingsPageName = requestedSettingsPageName,
                                        onRequestedSettingsPageConsumed = {
                                            requestedSettingsPageName = null
                                        },
                                        onInitialHomeContentRendered = { initialHomeReady = true },
                                    )
                                }

                                if (isTabletLayout && !useNativeBottomTabs) {
                                    TabletFloatingTopBar(
                                        selectedTab = selectedTab,
                                        liveTvEnabled = liveTvEnabled,
                                        onTabSelected = ::handleRootTabClick,
                                        onProfileSelected = onProfileSelected,
                                        onAddProfileRequested = {
                                            nativeProfileSwitcherVisible = false
                                            onSwitchProfile()
                                        },
                                    )
                                }

                                if (!isTabletLayout && useNativeBottomTabs && tabsRouteActive) {
                                    NativeProfileSwitcherPopup(
                                        visible = nativeProfileSwitcherVisible,
                                        isSwitchingProfile = profileSwitchLoading,
                                        onDismissRequest = { nativeProfileSwitcherVisible = false },
                                        onProfileSelected = onProfileSelected,
                                        onAddProfileRequested = {
                                            nativeProfileSwitcherVisible = false
                                            onSwitchProfile()
                                        },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(bottom = nativeProfileTabAnchorBottomPadding),
                                    )
                                }
                            }
                        }
                    }
                }
                composable<DetailRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<DetailRoute>()
                    val directorRole = stringResource(Res.string.person_role_director)
                    val writerRole = stringResource(Res.string.person_role_writer)
                    val creatorRole = stringResource(Res.string.person_role_creator)
                    MetaDetailsScreen(
                        type = route.type,
                        id = route.id,
                        onBack = {
                            navController.popBackStack()
                        },
                        onPlay = onPlay,
                        onPlayManually = onPlayManually,
                        onRandomPlay = onRandomPlay,
                        onRandomPlayManually = onRandomPlayManually,
                        onOpenMeta = { preview ->
                            coroutineScope.launch {
                                val resolvedId = if (preview.id.startsWith("tmdb:")) {
                                    val tmdbId = preview.id.removePrefix("tmdb:").toIntOrNull()
                                    tmdbId?.let {
                                        TmdbService.tmdbToImdb(
                                            tmdbId = it,
                                            mediaType = preview.type,
                                        )
                                    } ?: preview.id
                                } else {
                                    preview.id
                                }
                                navController.navigate(
                                    DetailRoute(
                                        type = preview.type,
                                        id = resolvedId,
                                    ),
                                )
                            }
                        },
                        onCastClick = { person, avatarTransitionKey ->
                            val tmdbId = person.tmdbId
                            if (tmdbId != null && tmdbId > 0) {
                                navController.navigate(
                                    PersonDetailRoute(
                                        personId = tmdbId,
                                        personName = person.name,
                                        personPhoto = person.photo,
                                        castAvatarTransitionKey = avatarTransitionKey,
                                        preferCrew = person.role?.let {
                                            it.equals("Director", ignoreCase = true) ||
                                                it.equals(directorRole, ignoreCase = true) ||
                                                it.equals("Writer", ignoreCase = true) ||
                                                it.equals(writerRole, ignoreCase = true) ||
                                                it.equals("Creator", ignoreCase = true)
                                                || it.equals(creatorRole, ignoreCase = true)
                                        } ?: false,
                                    ),
                                )
                            }
                        },
                        onCompanyClick = { company, entityKind ->
                            val tmdbId = company.tmdbId
                            if (tmdbId != null && tmdbId > 0) {
                                navController.navigate(
                                    EntityBrowseRoute(
                                        entityKind = entityKind,
                                        entityId = tmdbId,
                                        entityName = company.name,
                                        sourceType = route.type,
                                    ),
                                )
                            }
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                composable<PersonDetailRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<PersonDetailRoute>()
                    PersonDetailScreen(
                        personId = route.personId,
                        personName = route.personName,
                        initialProfilePhoto = route.personPhoto,
                        avatarTransitionKey = route.castAvatarTransitionKey,
                        preferCrew = route.preferCrew,
                        onBack = { navController.popBackStack() },
                        onOpenMeta = { preview ->
                            coroutineScope.launch {
                                val resolvedId = if (preview.id.startsWith("tmdb:")) {
                                    val tmdbId = preview.id.removePrefix("tmdb:").toIntOrNull()
                                    tmdbId?.let {
                                        TmdbService.tmdbToImdb(
                                            tmdbId = it,
                                            mediaType = preview.type,
                                        )
                                    } ?: preview.id
                                } else {
                                    preview.id
                                }
                                navController.navigate(
                                    DetailRoute(
                                        type = preview.type,
                                        id = resolvedId,
                                    ),
                                )
                            }
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                composable<EntityBrowseRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<EntityBrowseRoute>()
                    TmdbEntityBrowseScreen(
                        entityKind = TmdbEntityKind.fromRouteValue(route.entityKind),
                        entityId = route.entityId,
                        entityName = route.entityName,
                        sourceType = route.sourceType,
                        onBack = { navController.popBackStack() },
                        onOpenMeta = { preview ->
                            coroutineScope.launch {
                                val resolvedId = if (preview.id.startsWith("tmdb:")) {
                                    val tmdbId = preview.id.removePrefix("tmdb:").toIntOrNull()
                                    tmdbId?.let {
                                        TmdbService.tmdbToImdb(
                                            tmdbId = it,
                                            mediaType = preview.type,
                                        )
                                    } ?: preview.id
                                } else {
                                    preview.id
                                }
                                navController.navigate(
                                    DetailRoute(
                                        type = preview.type,
                                        id = resolvedId,
                                    ),
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                composable<StreamRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<StreamRoute>()
                    val launch = remember(route.launchId) {
                        StreamLaunchStore.get(route.launchId)
                    }
                    if (launch == null) {
                        LaunchedEffect(route.launchId) {
                            StreamsRepository.clear()
                            navController.popBackStack()
                        }
                        return@composable
                    }
                    val pauseDescription = launch.pauseDescription
                    val streamRouteScope = rememberCoroutineScope()
                    var resolvingDebridStream by rememberSaveable(route.launchId) { mutableStateOf(false) }
                    var pendingP2pStreamOpen by remember { mutableStateOf<PendingP2pStreamOpen?>(null) }
                    val lifecycleOwner = backStackEntry
                    DisposableEffect(lifecycleOwner, route.launchId) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_DESTROY) {
                                StreamLaunchStore.remove(route.launchId)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }
                    val shouldResolveEpisodeVideoId =
                        launch.parentMetaId != null &&
                            launch.seasonNumber != null &&
                            launch.episodeNumber != null
                    var effectiveVideoId by rememberSaveable(
                        launch.videoId,
                        launch.parentMetaId,
                        launch.seasonNumber,
                        launch.episodeNumber,
                    ) {
                        mutableStateOf(launch.videoId)
                    }
                    var hasResolvedVideoId by rememberSaveable(
                        launch.videoId,
                        launch.parentMetaId,
                        launch.seasonNumber,
                        launch.episodeNumber,
                    ) {
                        mutableStateOf(!shouldResolveEpisodeVideoId)
                    }

                    LaunchedEffect(
                        launch.videoId,
                        launch.parentMetaId,
                        launch.parentMetaType,
                        launch.type,
                        launch.seasonNumber,
                        launch.episodeNumber,
                    ) {
                        effectiveVideoId = launch.videoId
                        if (!shouldResolveEpisodeVideoId) {
                            hasResolvedVideoId = true
                            return@LaunchedEffect
                        }

                        hasResolvedVideoId = false
                        val metaType = launch.parentMetaType ?: launch.type
                        val metaId = launch.parentMetaId ?: return@LaunchedEffect
                        val resolvedVideoId = runCatching {
                            MetaDetailsRepository.fetch(metaType, metaId)
                        }.getOrNull()
                            ?.videos
                            ?.firstOrNull { video ->
                                video.season == launch.seasonNumber &&
                                    video.episode == launch.episodeNumber
                            }
                            ?.id
                            ?.takeIf { it.isNotBlank() }

                        effectiveVideoId = resolvedVideoId ?: launch.videoId
                        hasResolvedVideoId = true
                    }

                    val playerSettings by remember {
                        PlayerSettingsRepository.ensureLoaded()
                        PlayerSettingsRepository.uiState
                    }.collectAsStateWithLifecycle()

                    fun p2pSentinelUrl(infoHash: String, fileIdx: Int?): String =
                        "torrent://$infoHash${fileIdx?.let { "?index=$it" }.orEmpty()}"

                    fun openP2pStream(
                        stream: StreamItem,
                        resolvedResumePositionMs: Long?,
                        resolvedResumeProgressFraction: Float?,
                        replaceStreamRoute: Boolean,
                    ) {
                        val infoHash = stream.p2pInfoHash ?: return
                        val sentinelUrl = p2pSentinelUrl(infoHash, stream.p2pFileIdx)
                        if (playerSettings.streamReuseLastLinkEnabled) {
                            val cacheKey = StreamLinkCacheRepository.contentKey(
                                type = launch.type,
                                videoId = effectiveVideoId,
                                parentMetaId = launch.parentMetaId,
                                season = launch.seasonNumber,
                                episode = launch.episodeNumber,
                            )
                            StreamLinkCacheRepository.save(
                                contentKey = cacheKey,
                                url = "",
                                streamName = stream.streamLabel,
                                addonName = stream.addonName,
                                addonId = stream.addonId,
                                requestHeaders = emptyMap(),
                                responseHeaders = emptyMap(),
                                filename = stream.behaviorHints.filename,
                                videoSize = stream.behaviorHints.videoSize,
                                infoHash = infoHash,
                                fileIdx = stream.p2pFileIdx,
                                sources = stream.sources,
                                bingeGroup = stream.behaviorHints.bingeGroup,
                            )
                        }
                        val playerLaunch = PlayerLaunch(
                            profileId = launch.profileId,
                            title = launch.title,
                            sourceUrl = sentinelUrl,
                            sourceHeaders = emptyMap(),
                            sourceResponseHeaders = emptyMap(),
                            streamType = stream.streamType,
                            logo = launch.logo,
                            poster = launch.poster,
                            background = launch.background,
                            seasonNumber = launch.seasonNumber,
                            episodeNumber = launch.episodeNumber,
                            episodeTitle = launch.episodeTitle,
                            episodeThumbnail = launch.episodeThumbnail,
                            streamTitle = stream.streamLabel,
                            streamSubtitle = stream.streamSubtitle,
                            bingeGroup = stream.behaviorHints.bingeGroup,
                            pauseDescription = pauseDescription,
                            providerName = stream.addonName,
                            providerAddonId = stream.addonId,
                            contentType = launch.type,
                            videoId = effectiveVideoId,
                            parentMetaId = launch.parentMetaId ?: effectiveVideoId,
                            parentMetaType = launch.parentMetaType ?: launch.type,
                            torrentInfoHash = infoHash,
                            torrentFileIdx = stream.p2pFileIdx,
                            torrentFilename = stream.behaviorHints.filename,
                            torrentTrackers = stream.p2pTrackers,
                            initialPositionMs = resolvedResumePositionMs ?: 0L,
                            initialProgressFraction = resolvedResumeProgressFraction,
                            randomEpisodeMode = launch.randomEpisodeMode,
                        )

                        val launchId = PlayerLaunchStore.put(playerLaunch)
                        StreamsRepository.cancelLoading()
                        navController.navigate(PlayerRoute(launchId = launchId)) {
                            if (replaceStreamRoute) {
                                popUpTo<StreamRoute> { inclusive = true }
                            }
                        }
                    }

                    fun requestOrOpenP2pStream(
                        stream: StreamItem,
                        resolvedResumePositionMs: Long?,
                        resolvedResumeProgressFraction: Float?,
                        forceExternal: Boolean,
                        forceInternal: Boolean,
                        isAutoPlay: Boolean,
                    ) {
                        if (stream.p2pInfoHash == null) {
                            if (isAutoPlay) StreamsRepository.skipAutoPlayStream(stream)
                            return
                        }
                        if (!P2pSettingsRepository.isVisible) {
                            if (isAutoPlay) StreamsRepository.skipAutoPlayStream(stream)
                            return
                        }
                        if (!p2pSettingsUiState.p2pEnabled) {
                            pendingP2pStreamOpen = PendingP2pStreamOpen(
                                stream = stream,
                                resumePositionMs = resolvedResumePositionMs,
                                resumeProgressFraction = resolvedResumeProgressFraction,
                                forceExternal = forceExternal,
                                forceInternal = forceInternal,
                                isAutoPlay = isAutoPlay,
                            )
                            return
                        }
                        openP2pStream(
                            stream = stream,
                            resolvedResumePositionMs = resolvedResumePositionMs,
                            resolvedResumeProgressFraction = resolvedResumeProgressFraction,
                            replaceStreamRoute = isAutoPlay,
                        )
                    }

                    // Reuse Last Link: auto-play from cache if enabled (only on first entry)
                    var reuseHandled by rememberSaveable(launch.videoId, effectiveVideoId) { mutableStateOf(false) }
                    var reuseNavigated by remember { mutableStateOf(false) }
                    LaunchedEffect(effectiveVideoId, hasResolvedVideoId, playerSettings.streamReuseLastLinkEnabled, launch.manualSelection) {
                        if (!hasResolvedVideoId) return@LaunchedEffect
                        if (reuseHandled) return@LaunchedEffect
                        reuseHandled = true
                        if (launch.manualSelection) return@LaunchedEffect
                        if (!playerSettings.streamReuseLastLinkEnabled) return@LaunchedEffect
                        val cacheKey = StreamLinkCacheRepository.contentKey(
                            type = launch.type,
                            videoId = effectiveVideoId,
                            parentMetaId = launch.parentMetaId,
                            season = launch.seasonNumber,
                            episode = launch.episodeNumber,
                        )
                        val maxAgeMs = playerSettings.streamReuseLastLinkCacheHours * 60L * 60L * 1000L
                        val cached = StreamLinkCacheRepository.getValid(cacheKey, maxAgeMs)
                        if (cached != null) {
                            if (cached.url.isBlank() && !cached.infoHash.isNullOrBlank()) {
                                val cachedStream = StreamItem(
                                    name = cached.streamName,
                                    url = null,
                                    infoHash = cached.infoHash,
                                    fileIdx = cached.fileIdx,
                                    sources = cached.sources,
                                    addonName = cached.addonName,
                                    addonId = cached.addonId,
                                    behaviorHints = StreamBehaviorHints(
                                        filename = cached.filename,
                                        videoSize = cached.videoSize,
                                        bingeGroup = cached.bingeGroup,
                                    ),
                                )
                                requestOrOpenP2pStream(
                                    stream = cachedStream,
                                    resolvedResumePositionMs = launch.resumePositionMs,
                                    resolvedResumeProgressFraction = launch.resumeProgressFraction,
                                    forceExternal = false,
                                    forceInternal = true,
                                    isAutoPlay = true,
                                )
                                reuseNavigated = true
                                return@LaunchedEffect
                            }
                            val playerLaunch = PlayerLaunch(
                                profileId = launch.profileId,
                                title = launch.title,
                                sourceUrl = cached.url,
                                sourceHeaders = sanitizePlaybackHeaders(cached.requestHeaders),
                                sourceResponseHeaders = sanitizePlaybackResponseHeaders(cached.responseHeaders),
                                externalSubtitles = emptyList(),
                                streamType = cached.streamType,
                                logo = launch.logo,
                                poster = launch.poster,
                                background = launch.background,
                                seasonNumber = launch.seasonNumber,
                                episodeNumber = launch.episodeNumber,
                                episodeTitle = launch.episodeTitle,
                                episodeThumbnail = launch.episodeThumbnail,
                                streamTitle = cached.streamName,
                                streamSubtitle = null,
                                bingeGroup = cached.bingeGroup,
                                pauseDescription = pauseDescription,
                                providerName = cached.addonName,
                                providerAddonId = cached.addonId,
                                contentType = launch.type,
                                videoId = effectiveVideoId,
                                parentMetaId = launch.parentMetaId ?: effectiveVideoId,
                                parentMetaType = launch.parentMetaType ?: launch.type,
                                initialPositionMs = launch.resumePositionMs ?: 0L,
                                initialProgressFraction = launch.resumeProgressFraction,
                                contentLanguage = cached.contentLanguage,
                                randomEpisodeMode = launch.randomEpisodeMode,
                            )
                            if (playerSettings.externalPlayerEnabled) {
                                openExternalPlayback(playerLaunch)
                                StreamsRepository.setOverlayVisible(false)
                                reuseNavigated = true
                                return@LaunchedEffect
                            }
                            StreamsRepository.clear()
                            reuseNavigated = true
                            val launchId = PlayerLaunchStore.put(playerLaunch)
                            navController.navigate(PlayerRoute(launchId = launchId)) {
                                popUpTo<StreamRoute> { inclusive = true }
                            }
                        }
                    }

                    val streamsUiState by StreamsRepository.uiState.collectAsStateWithLifecycle()
                    val expectedStreamsRequestToken = StreamsRepository.requestToken(
                        type = launch.type,
                        videoId = effectiveVideoId,
                        season = launch.seasonNumber,
                        episode = launch.episodeNumber,
                        manualSelection = launch.manualSelection,
                    )
                    var autoPlayHandled by rememberSaveable(launch.videoId, effectiveVideoId) { mutableStateOf(false) }
                    LaunchedEffect(
                        streamsUiState.autoPlayStream,
                        streamsUiState.requestToken,
                        expectedStreamsRequestToken,
                        reuseHandled,
                        launch.manualSelection,
                    ) {
                        if (!reuseHandled) return@LaunchedEffect
                        if (launch.manualSelection) return@LaunchedEffect
                        if (reuseNavigated) return@LaunchedEffect
                        if (autoPlayHandled) return@LaunchedEffect
                        if (streamsUiState.requestToken != expectedStreamsRequestToken) return@LaunchedEffect
                        val selectedStream = streamsUiState.autoPlayStream ?: return@LaunchedEffect
                        val stream = if (DirectDebridPlaybackResolver.shouldResolveToPlayableStream(selectedStream)) {
                            val resolved = try {
                                DirectDebridPlaybackResolver.resolveToPlayableStream(
                                    stream = selectedStream,
                                    season = launch.seasonNumber,
                                    episode = launch.episodeNumber,
                                )
                            } catch (error: Throwable) {
                                if (error is kotlinx.coroutines.CancellationException) throw error
                                val hasNextCandidate = StreamsRepository.skipAutoPlayStream(selectedStream)
                                if (!hasNextCandidate) {
                                    StreamsRepository.cancelLoading()
                                    error.message
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { NuvioToastController.show(it) }
                                }
                                return@LaunchedEffect
                            }
                            when (resolved) {
                                is DirectDebridPlayableResult.Success -> resolved.stream
                                else -> {
                                    val hasNextCandidate = StreamsRepository.skipAutoPlayStream(selectedStream)
                                    if (!hasNextCandidate) {
                                        resolved.toastMessage()?.let { NuvioToastController.show(it) }
                                    }
                                    if (!hasNextCandidate && resolved == DirectDebridPlayableResult.Stale) {
                                        StreamsRepository.reload(
                                            type = launch.type,
                                            videoId = effectiveVideoId,
                                            parentMetaId = launch.parentMetaId,
                                            parentMetaType = launch.parentMetaType,
                                            season = launch.seasonNumber,
                                            episode = launch.episodeNumber,
                                            manualSelection = launch.manualSelection,
                                            searchTitle = launch.title,
                                        )
                                    }
                                    return@LaunchedEffect
                                }
                            }
                        } else {
                            selectedStream
                        }
                        val sourceUrl = stream.playableDirectUrl
                        if (sourceUrl == null && stream.needsLocalDebridResolve && stream.p2pInfoHash != null) {
                            autoPlayHandled = true
                            requestOrOpenP2pStream(
                                stream = stream,
                                resolvedResumePositionMs = launch.resumePositionMs,
                                resolvedResumeProgressFraction = launch.resumeProgressFraction,
                                forceExternal = false,
                                forceInternal = true,
                                isAutoPlay = true,
                            )
                            StreamsRepository.consumeAutoPlay()
                            return@LaunchedEffect
                        }
                        if (sourceUrl == null) {
                            StreamsRepository.skipAutoPlayStream(selectedStream)
                            return@LaunchedEffect
                        }
                        autoPlayHandled = true
                        if (playerSettings.streamReuseLastLinkEnabled) {
                            val cacheKey = StreamLinkCacheRepository.contentKey(
                                type = launch.type,
                                videoId = effectiveVideoId,
                                parentMetaId = launch.parentMetaId,
                                season = launch.seasonNumber,
                                episode = launch.episodeNumber,
                            )
                            StreamLinkCacheRepository.save(
                                contentKey = cacheKey,
                                url = sourceUrl,
                                streamName = stream.streamLabel,
                                addonName = stream.addonName,
                                addonId = stream.addonId,
                                requestHeaders = sanitizePlaybackHeaders(stream.behaviorHints.proxyHeaders?.request),
                                responseHeaders = sanitizePlaybackResponseHeaders(stream.behaviorHints.proxyHeaders?.response),
                                filename = stream.behaviorHints.filename,
                                videoSize = stream.behaviorHints.videoSize,
                                bingeGroup = stream.behaviorHints.bingeGroup,
                                streamType = stream.streamType,
                            )
                        }
                        val playerLaunch = PlayerLaunch(
                            profileId = launch.profileId,
                            title = launch.title,
                            sourceUrl = sourceUrl,
                            sourceHeaders = sanitizePlaybackHeaders(stream.behaviorHints.proxyHeaders?.request),
                            sourceResponseHeaders = sanitizePlaybackResponseHeaders(stream.behaviorHints.proxyHeaders?.response),
                            externalSubtitles = stream.externalSubtitles,
                            streamType = stream.streamType,
                            logo = launch.logo,
                            poster = launch.poster,
                            background = launch.background,
                            seasonNumber = launch.seasonNumber,
                            episodeNumber = launch.episodeNumber,
                            episodeTitle = launch.episodeTitle,
                            episodeThumbnail = launch.episodeThumbnail,
                            streamTitle = stream.streamLabel,
                            streamSubtitle = stream.streamSubtitle,
                            bingeGroup = stream.behaviorHints.bingeGroup,
                            pauseDescription = pauseDescription,
                            providerName = stream.addonName,
                            providerAddonId = stream.addonId,
                            contentType = launch.type,
                            videoId = effectiveVideoId,
                            parentMetaId = launch.parentMetaId ?: effectiveVideoId,
                            parentMetaType = launch.parentMetaType ?: launch.type,
                            initialPositionMs = launch.resumePositionMs ?: 0L,
                            initialProgressFraction = launch.resumeProgressFraction,
                            randomEpisodeMode = launch.randomEpisodeMode,
                        )
                        if (playerSettings.externalPlayerEnabled) {
                            openExternalPlayback(playerLaunch)
                            StreamsRepository.consumeAutoPlay()
                            StreamsRepository.cancelLoading()
                            return@LaunchedEffect
                        }
                        StreamsRepository.consumeAutoPlay()
                        StreamsRepository.cancelLoading()
                        val launchId = PlayerLaunchStore.put(playerLaunch)
                        navController.navigate(PlayerRoute(launchId = launchId)) {
                            popUpTo<StreamRoute> { inclusive = true }
                        }
                    }

                    if (!hasResolvedVideoId) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.nuvio.colors.accent)
                        }
                        return@composable
                    }

                    fun openSelectedStream(
                        stream: StreamItem,
                        resolvedResumePositionMs: Long?,
                        resolvedResumeProgressFraction: Float?,
                        forceExternal: Boolean,
                        forceInternal: Boolean,
                    ) {
                        if (DirectDebridPlaybackResolver.shouldResolveToPlayableStream(stream)) {
                            if (resolvingDebridStream) return
                            streamRouteScope.launch {
                                resolvingDebridStream = true
                                val resolved = try {
                                    DirectDebridPlaybackResolver.resolveToPlayableStream(
                                        stream = stream,
                                        season = launch.seasonNumber,
                                        episode = launch.episodeNumber,
                                    )
                                } catch (error: Throwable) {
                                    if (error is kotlinx.coroutines.CancellationException) throw error
                                    error.message
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { NuvioToastController.show(it) }
                                    return@launch
                                } finally {
                                    resolvingDebridStream = false
                                }
                                when (resolved) {
                                    is DirectDebridPlayableResult.Success -> openSelectedStream(
                                        stream = resolved.stream,
                                        resolvedResumePositionMs = resolvedResumePositionMs,
                                        resolvedResumeProgressFraction = resolvedResumeProgressFraction,
                                        forceExternal = forceExternal,
                                        forceInternal = forceInternal,
                                    )
                                    else -> {
                                        resolved.toastMessage()?.let { NuvioToastController.show(it) }
                                        if (resolved == DirectDebridPlayableResult.Stale) {
                                            StreamsRepository.reload(
                                                type = launch.type,
                                                videoId = effectiveVideoId,
                                                parentMetaId = launch.parentMetaId,
                                                parentMetaType = launch.parentMetaType,
                                                season = launch.seasonNumber,
                                                episode = launch.episodeNumber,
                                                manualSelection = launch.manualSelection,
                                                searchTitle = launch.title,
                                            )
                                        }
                                    }
                                }
                            }
                            return
                        }
                        if (stream.needsLocalDebridResolve && stream.p2pInfoHash != null) {
                            requestOrOpenP2pStream(
                                stream = stream,
                                resolvedResumePositionMs = resolvedResumePositionMs,
                                resolvedResumeProgressFraction = resolvedResumeProgressFraction,
                                forceExternal = forceExternal,
                                forceInternal = forceInternal,
                                isAutoPlay = false,
                            )
                            return
                        }
                        val sourceUrl = stream.playableDirectUrl ?: return
                        if (playerSettings.streamReuseLastLinkEnabled) {
                            val cacheKey = StreamLinkCacheRepository.contentKey(
                                type = launch.type,
                                videoId = effectiveVideoId,
                                parentMetaId = launch.parentMetaId,
                                season = launch.seasonNumber,
                                episode = launch.episodeNumber,
                            )
                            StreamLinkCacheRepository.save(
                                contentKey = cacheKey,
                                url = sourceUrl,
                                streamName = stream.streamLabel,
                                addonName = stream.addonName,
                                addonId = stream.addonId,
                                requestHeaders = sanitizePlaybackHeaders(stream.behaviorHints.proxyHeaders?.request),
                                responseHeaders = sanitizePlaybackResponseHeaders(stream.behaviorHints.proxyHeaders?.response),
                                filename = stream.behaviorHints.filename,
                                videoSize = stream.behaviorHints.videoSize,
                                bingeGroup = stream.behaviorHints.bingeGroup,
                                streamType = stream.streamType,
                            )
                        }
                        val playerLaunch = PlayerLaunch(
                            profileId = launch.profileId,
                            title = launch.title,
                            sourceUrl = sourceUrl,
                            sourceHeaders = sanitizePlaybackHeaders(stream.behaviorHints.proxyHeaders?.request),
                            sourceResponseHeaders = sanitizePlaybackResponseHeaders(stream.behaviorHints.proxyHeaders?.response),
                            externalSubtitles = stream.externalSubtitles,
                            streamType = stream.streamType,
                            logo = launch.logo,
                            poster = launch.poster,
                            background = launch.background,
                            seasonNumber = launch.seasonNumber,
                            episodeNumber = launch.episodeNumber,
                            episodeTitle = launch.episodeTitle,
                            episodeThumbnail = launch.episodeThumbnail,
                            streamTitle = stream.streamLabel,
                            streamSubtitle = stream.streamSubtitle,
                            bingeGroup = stream.behaviorHints.bingeGroup,
                            pauseDescription = pauseDescription,
                            providerName = stream.addonName,
                            providerAddonId = stream.addonId,
                            contentType = launch.type,
                            videoId = effectiveVideoId,
                            parentMetaId = launch.parentMetaId ?: effectiveVideoId,
                            parentMetaType = launch.parentMetaType ?: launch.type,
                            initialPositionMs = resolvedResumePositionMs ?: 0L,
                            initialProgressFraction = resolvedResumeProgressFraction,
                            randomEpisodeMode = launch.randomEpisodeMode,
                        )

                        if (!forceInternal && (forceExternal || playerSettings.externalPlayerEnabled)) {
                            streamRouteScope.launch {
                                openExternalPlayback(playerLaunch)
                                StreamsRepository.cancelLoading()
                            }
                            return
                        }

                        val launchId = PlayerLaunchStore.put(playerLaunch)
                        StreamsRepository.cancelLoading()
                        navController.navigate(
                            PlayerRoute(launchId = launchId)
                        )
                    }

                    // Hide overlay when reuse navigated to external player (prevents reload from showing it again)
                    LaunchedEffect(reuseNavigated) {
                        if (reuseNavigated) {
                            StreamsRepository.setOverlayVisible(false)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        StreamsScreen(
                            type = launch.type,
                            videoId = effectiveVideoId,
                            parentMetaId = launch.parentMetaId ?: effectiveVideoId,
                            parentMetaType = launch.parentMetaType ?: launch.type,
                            title = launch.title,
                            logo = launch.logo,
                            poster = launch.poster,
                            background = launch.background,
                            seasonNumber = launch.seasonNumber,
                            episodeNumber = launch.episodeNumber,
                            episodeTitle = launch.episodeTitle,
                            episodeThumbnail = launch.episodeThumbnail,
                            episodeOverview = launch.pauseDescription,
                            resumePositionMs = launch.resumePositionMs,
                            resumeProgressFraction = launch.resumeProgressFraction,
                            manualSelection = launch.manualSelection,
                            startFromBeginning = launch.startFromBeginning,
                            onStreamSelected = { stream, resolvedResumePositionMs, resolvedResumeProgressFraction ->
                                openSelectedStream(
                                    stream = stream,
                                    resolvedResumePositionMs = resolvedResumePositionMs,
                                    resolvedResumeProgressFraction = resolvedResumeProgressFraction,
                                    forceExternal = false,
                                    forceInternal = false,
                                )
                            },
                            onStreamActionOpen = { stream, openExternally, resolvedResumePositionMs, resolvedResumeProgressFraction ->
                                openSelectedStream(
                                    stream = stream,
                                    resolvedResumePositionMs = resolvedResumePositionMs,
                                    resolvedResumeProgressFraction = resolvedResumeProgressFraction,
                                    forceExternal = openExternally,
                                    forceInternal = !openExternally,
                                )
                            },
                            onBack = {
                                StreamsRepository.clear()
                                navController.popBackStack()
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                        pendingP2pStreamOpen?.let { pending ->
                            P2pConsentDialog(
                                onEnableP2p = {
                                    P2pSettingsRepository.setP2pEnabled(true)
                                    pendingP2pStreamOpen = null
                                    openP2pStream(
                                        stream = pending.stream,
                                        resolvedResumePositionMs = pending.resumePositionMs,
                                        resolvedResumeProgressFraction = pending.resumeProgressFraction,
                                        replaceStreamRoute = pending.isAutoPlay,
                                    )
                                },
                                onDismiss = {
                                    if (pending.isAutoPlay) {
                                        StreamsRepository.skipAutoPlayStream(pending.stream)
                                        StreamsRepository.consumeAutoPlay()
                                    }
                                    pendingP2pStreamOpen = null
                                },
                            )
                        }
                        if (resolvingDebridStream) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.nuvio.colors.overlayScrim.copy(alpha = MaterialTheme.nuvio.opacity.overlayHeavy)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.nuvio.spacing.cardPadding),
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.nuvio.colors.playerControlsForeground)
                                    Text(
                                        text = stringResource(Res.string.streams_finding_source),
                                        color = MaterialTheme.nuvio.colors.playerControlsForeground.copy(alpha = MaterialTheme.nuvio.opacity.overlayHeavy),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }
                composable<PlayerRoute>(
                    enterTransition = {
                        if (isIos) fadeIn(animationSpec = tween(220)) else null
                    },
                    exitTransition = {
                        if (isIos) fadeOut(animationSpec = tween(220)) else null
                    },
                    popEnterTransition = {
                        if (isIos) fadeIn(animationSpec = tween(220)) else null
                    },
                    popExitTransition = {
                        if (isIos) fadeOut(animationSpec = tween(220)) else null
                    },
                ) { backStackEntry ->
                    val route = backStackEntry.toRoute<PlayerRoute>()
                    val launch = remember(route.launchId) { PlayerLaunchStore.get(route.launchId) }
                    if (launch == null) {
                        LaunchedEffect(route.launchId) {
                            navController.popBackStack()
                        }
                        Box(modifier = Modifier.fillMaxSize())
                        return@composable
                    }
                    LaunchedEffect(launch.videoId) {
                        launch.videoId?.let { ResumePromptRepository.markPlayerEntered(it) }
                    }
                    PlayerScreen(
                        profileId = launch.profileId,
                        title = launch.title,
                        sourceUrl = launch.sourceUrl,
                        sourceAudioUrl = launch.sourceAudioUrl,
                        sourceHeaders = launch.sourceHeaders,
                        sourceResponseHeaders = launch.sourceResponseHeaders,
                        externalSubtitles = launch.externalSubtitles,
                        streamType = launch.streamType,
                        logo = launch.logo,
                        poster = launch.poster,
                        background = launch.background,
                        seasonNumber = launch.seasonNumber,
                        episodeNumber = launch.episodeNumber,
                        episodeTitle = launch.episodeTitle,
                        episodeThumbnail = launch.episodeThumbnail,
                        streamTitle = launch.streamTitle,
                        streamSubtitle = launch.streamSubtitle,
                        initialBingeGroup = launch.bingeGroup,
                        pauseDescription = launch.pauseDescription,
                        providerName = launch.providerName,
                        providerAddonId = launch.providerAddonId,
                        contentType = launch.contentType,
                        videoId = launch.videoId,
                        parentMetaId = launch.parentMetaId,
                        parentMetaType = launch.parentMetaType,
                        torrentInfoHash = launch.torrentInfoHash,
                        torrentFileIdx = launch.torrentFileIdx,
                        torrentFilename = launch.torrentFilename,
                        torrentTrackers = launch.torrentTrackers,
                        initialPositionMs = launch.initialPositionMs,
                        initialProgressFraction = launch.initialProgressFraction,
                        contentLanguage = launch.contentLanguage,
                        onBack = {
                            ResumePromptRepository.markPlayerExitedNormally()
                            PlayerLaunchStore.remove(route.launchId)
                            navController.popBackStack()
                        },
                        onOpenInExternalPlayer = { request ->
                            val playerLaunch = PlayerLaunch(
                                profileId = launch.profileId,
                                title = launch.title,
                                sourceUrl = request.sourceUrl,
                                sourceHeaders = request.sourceHeaders,
                                logo = launch.logo,
                                poster = launch.poster,
                                background = launch.background,
                                seasonNumber = launch.seasonNumber,
                                episodeNumber = launch.episodeNumber,
                                episodeTitle = launch.episodeTitle,
                                episodeThumbnail = launch.episodeThumbnail,
                                streamTitle = request.streamTitle ?: launch.streamTitle,
                                streamSubtitle = launch.streamSubtitle,
                                bingeGroup = launch.bingeGroup,
                                pauseDescription = launch.pauseDescription,
                                providerName = launch.providerName,
                                providerAddonId = launch.providerAddonId,
                                contentType = launch.contentType,
                                videoId = launch.videoId,
                                parentMetaId = launch.parentMetaId,
                                parentMetaType = launch.parentMetaType,
                                initialPositionMs = request.resumePositionMs,
                                randomEpisodeMode = launch.randomEpisodeMode,
                            )
                            lastExternalPlayerLaunch = playerLaunch
                            val intentResult = ExternalPlayerPlatform.buildIntent(
                                request = request,
                                playerId = playerSettingsUiState.externalPlayerId,
                            )
                            when (intentResult) {
                                is ExternalPlayerIntentResult.Success -> {
                                    val launched = launchExternalPlayer(intentResult)
                                    if (!launched) {
                                        NuvioToastController.show(externalPlayerFailedText)
                                    }
                                }
                                ExternalPlayerIntentResult.NotConfigured -> {
                                    NuvioToastController.show(externalPlayerNotConfiguredText)
                                }
                                ExternalPlayerIntentResult.Failed -> {
                                    NuvioToastController.show(externalPlayerFailedText)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                composable<CatalogRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<CatalogRoute>()
                    val launch = remember(route.launchId) { CatalogLaunchStore.get(route.launchId) }
                    if (launch == null) {
                        LaunchedEffect(route.launchId) {
                            navController.popBackStack()
                        }
                        return@composable
                    }
                    val target = launch.target
                    CatalogScreen(
                        title = launch.title,
                        subtitle = launch.subtitle,
                        target = target,
                        onBack = {
                            CatalogRepository.clear()
                            CatalogLaunchStore.remove(route.launchId)
                            navController.popBackStack()
                        },
                        onPosterClick = { meta ->
                            navController.navigate(DetailRoute(type = meta.type, id = meta.id))
                        },
                        onPosterLongClick = { meta ->
                            openPosterActions(
                                if (target is CatalogTarget.Library) {
                                    PosterActionTarget(
                                        preview = meta,
                                        libraryItem = meta.toLibraryItem(savedAtEpochMs = 0L),
                                        libraryListKey = target.sectionType,
                                    )
                                } else {
                                    PosterActionTarget(preview = meta)
                                },
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                composable<HomescreenSettingsRoute> {
                    val onBack = rememberGuardedPopBackStack(
                        navController = navController,
                        backStackEntry = it,
                    )
                    HomescreenSettingsScreen(
                        onBack = onBack,
                    )
                }
                composable<MetaScreenSettingsRoute> { backStackEntry ->
                    val onBack = rememberGuardedPopBackStack(
                        navController = navController,
                        backStackEntry = backStackEntry,
                    )
                    MetaScreenSettingsScreen(
                        onBack = onBack,
                    )
                }
                composable<ContinueWatchingSettingsRoute> { backStackEntry ->
                    val onBack = rememberGuardedPopBackStack(
                        navController = navController,
                        backStackEntry = backStackEntry,
                    )
                    ContinueWatchingSettingsScreen(
                        onBack = onBack,
                    )
                }
                composable<DownloadsSettingsRoute> { backStackEntry ->
                    val onBack = rememberGuardedPopBackStack(
                        navController = navController,
                        backStackEntry = backStackEntry,
                    )
                    DownloadsScreen(
                        onBack = onBack,
                        onOpenDownload = { item ->
                            val sourceUrl = DownloadsRepository.playableLocalFileUri(item) ?: return@DownloadsScreen
                            val resumeEntry = item.videoId
                                .takeIf { it.isNotBlank() }
                                ?.let(WatchProgressRepository::progressForVideo)
                                ?.takeIf { it.isResumable }

                            val playerLaunch = PlayerLaunch(
                                profileId = activePlaybackProfileId,
                                title = item.title,
                                sourceUrl = sourceUrl,
                                sourceHeaders = emptyMap(),
                                sourceResponseHeaders = emptyMap(),
                                externalSubtitles = emptyList(),
                                streamType = null,
                                logo = item.logo,
                                poster = item.poster,
                                background = item.background,
                                seasonNumber = item.seasonNumber,
                                episodeNumber = item.episodeNumber,
                                episodeTitle = item.episodeTitle,
                                episodeThumbnail = item.episodeThumbnail,
                                streamTitle = item.streamTitle,
                                streamSubtitle = item.streamSubtitle,
                                providerName = item.providerName,
                                providerAddonId = item.providerAddonId,
                                contentType = item.contentType,
                                videoId = item.videoId,
                                parentMetaId = item.parentMetaId,
                                parentMetaType = item.parentMetaType,
                                initialPositionMs = resumeEntry?.lastPositionMs?.takeIf { it > 0L } ?: 0L,
                                initialProgressFraction = resumeEntry?.progressFraction?.takeIf { it > 0f },
                            )
                            if (playerSettingsUiState.externalPlayerEnabled) {
                                coroutineScope.launch { openExternalPlayback(playerLaunch) }
                                return@DownloadsScreen
                            }
                            val launchId = PlayerLaunchStore.put(playerLaunch)
                            navController.navigate(PlayerRoute(launchId = launchId))
                        },
                    )
                }
                composable<AddonsSettingsRoute> { backStackEntry ->
                    val onBack = rememberGuardedPopBackStack(
                        navController = navController,
                        backStackEntry = backStackEntry,
                    )
                    AddonsSettingsScreen(
                        onBack = onBack,
                    )
                }
                if (AppFeaturePolicy.pluginsEnabled) {
                    composable<PluginsSettingsRoute> { backStackEntry ->
                        val onBack = rememberGuardedPopBackStack(
                            navController = navController,
                            backStackEntry = backStackEntry,
                        )
                        PluginsSettingsScreen(
                            onBack = onBack,
                        )
                    }
                }
                composable<AccountSettingsRoute> { backStackEntry ->
                    val onBack = rememberGuardedPopBackStack(
                        navController = navController,
                        backStackEntry = backStackEntry,
                    )
                    AccountSettingsScreen(
                        onBack = onBack,
                    )
                }
                composable<SupportersContributorsSettingsRoute> { backStackEntry ->
                    val onBack = rememberGuardedPopBackStack(
                        navController = navController,
                        backStackEntry = backStackEntry,
                    )
                    if (AppFeaturePolicy.supportersContributorsPageEnabled) {
                        SupportersContributorsSettingsScreen(
                            onBack = onBack,
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            onBack()
                        }
                    }
                }
                composable<LicensesAttributionsSettingsRoute> { backStackEntry ->
                    val onBack = rememberGuardedPopBackStack(
                        navController = navController,
                        backStackEntry = backStackEntry,
                    )
                    LicensesAttributionsSettingsScreen(
                        onBack = onBack,
                    )
                }
                composable<CollectionsRoute> { backStackEntry ->
                    val onBack = rememberGuardedPopBackStack(
                        navController = navController,
                        backStackEntry = backStackEntry,
                    )
                    CollectionManagementScreen(
                        onBack = onBack,
                        onNavigateToEditor = { collectionId ->
                            navController.navigate(CollectionEditorRoute(collectionId = collectionId))
                        },
                    )
                }
                composable<CollectionEditorRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<CollectionEditorRoute>()
                    CollectionEditorScreen(
                        collectionId = route.collectionId,
                        onBack = {
                            CollectionEditorRepository.clear()
                            navController.popBackStack()
                        },
                    )
                }
                composable<FolderDetailRoute> { backStackEntry ->
                    val route = backStackEntry.toRoute<FolderDetailRoute>()
                    LaunchedEffect(route.collectionId, route.folderId) {
                        FolderDetailRepository.initialize(route.collectionId, route.folderId)
                    }
                    FolderDetailScreen(
                        onBack = {
                            FolderDetailRepository.clear()
                            navController.popBackStack()
                        },
                        onCatalogClick = onCatalogClick,
                        onPosterClick = { meta ->
                            navController.navigate(DetailRoute(type = meta.type, id = meta.id))
                        },
                    )
                }
                }
            }

            selectedPosterActionTarget?.let { posterActionTarget ->
                key(posterActionTarget) {
                    val preview = posterActionTarget.preview
                    val cachedDetails = remember(preview.type, preview.id) {
                        MetaDetailsRepository.peek(preview.type, preview.id)
                    }
                    val overlayImageUrl = selectedPosterAnchor?.imageUrl?.takeIf { it.isNotBlank() }
                        ?: cachedDetails?.poster?.takeIf { it.isNotBlank() }
                        ?: preview.poster?.takeIf { it.isNotBlank() }
                        ?: cachedDetails?.background?.takeIf { it.isNotBlank() }
                        ?: preview.banner?.takeIf { it.isNotBlank() }
                    val overlayBackgroundUrl = cachedDetails?.background?.takeIf { it.isNotBlank() }
                        ?: preview.banner?.takeIf { it.isNotBlank() }
                        ?: overlayImageUrl
                    val overlaySubtitle = cachedDetails?.releaseInfo
                        ?.takeIf { it.isNotBlank() }
                        ?.let { formatReleaseDateForDisplay(it) }
                        ?: preview.releaseInfo
                            ?.takeIf { it.isNotBlank() }
                            ?.let { formatReleaseDateForDisplay(it) }
                        ?: preview.type.replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase() else char.toString()
                        }
                    val overlaySynopsis = cachedDetails?.description?.trim()?.takeIf { it.isNotBlank() }
                        ?: preview.description?.trim()?.takeIf { it.isNotBlank() }
                    val isSaved = LibraryRepository.isSaved(preview.id, preview.type)
                    val isWatched = WatchingState.isPosterWatched(
                        watchedKeys = watchedUiState.watchedKeys,
                        item = preview,
                    )
                    val removesFromLibrary = isSaved &&
                        (posterActionTarget.libraryItem != null || !isTraktLibrarySource)

                    NuvioPosterZoomActionOverlay(
                        imageUrl = overlayImageUrl,
                        backgroundImageUrl = overlayBackgroundUrl,
                        title = preview.name,
                        subtitle = overlaySubtitle,
                        synopsis = overlaySynopsis,
                        isWatched = isWatched,
                        anchor = selectedPosterAnchor,
                        actions = listOf(
                            PosterZoomOverlayAction(
                                icon = Icons.Default.Info,
                                label = stringResource(Res.string.home_view_details),
                                onSelected = {
                                    navController.navigate(DetailRoute(type = preview.type, id = preview.id))
                                },
                            ),
                            PosterZoomOverlayAction(
                                icon = if (isSaved) Icons.Default.DeleteOutline else Icons.Default.Add,
                                label = if (isSaved) {
                                    stringResource(Res.string.hero_remove_from_library)
                                } else {
                                    stringResource(Res.string.hero_add_to_library)
                                },
                                isDestructive = removesFromLibrary,
                                onSelected = {
                                    val libraryItem = posterActionTarget.libraryItem
                                        ?: preview.toLibraryItem(savedAtEpochMs = 0L)
                                    if (posterActionTarget.libraryItem != null) {
                                        if (isTraktLibrarySource) {
                                            coroutineScope.launch {
                                                runCatching {
                                                    val listKey = posterActionTarget.libraryListKey
                                                    if (listKey.isNullOrBlank()) {
                                                        val currentMembership = LibraryRepository.getMembershipSnapshot(libraryItem)
                                                        LibraryRepository.applyMembershipChanges(
                                                            item = libraryItem,
                                                            desiredMembership = currentMembership.mapValues { false },
                                                        )
                                                    } else {
                                                        LibraryRepository.removeFromList(libraryItem, listKey)
                                                    }
                                                }.onFailure { error ->
                                                    NuvioToastController.show(
                                                        error.message ?: getString(Res.string.trakt_lists_update_failed),
                                                    )
                                                }
                                            }
                                        } else {
                                            LibraryRepository.remove(libraryItem.id)
                                        }
                                    } else {
                                        if (!isTraktLibrarySource) {
                                            LibraryRepository.toggleSaved(libraryItem)
                                        } else {
                                            pickerItem = libraryItem
                                            pickerTitle = preview.name
                                            pickerTabs = LibraryRepository.libraryListTabs()
                                            pickerMembership = pickerTabs.associate { it.key to false }
                                            pickerPending = true
                                            pickerError = null
                                            showLibraryListPicker = true
                                            coroutineScope.launch {
                                                runCatching {
                                                    val snapshot = LibraryRepository.getMembershipSnapshot(libraryItem)
                                                    val tabs = LibraryRepository.libraryListTabs()
                                                    pickerTabs = tabs
                                                    pickerMembership = tabs.associate { tab ->
                                                        tab.key to (snapshot[tab.key] == true)
                                                    }
                                                }.onFailure { error ->
                                                    pickerError = error.message ?: getString(Res.string.trakt_lists_load_failed)
                                                }
                                                pickerPending = false
                                            }
                                        }
                                    }
                                },
                            ),
                            PosterZoomOverlayAction(
                                icon = if (isWatched) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                label = if (isWatched) {
                                    stringResource(Res.string.hero_mark_unwatched)
                                } else {
                                    stringResource(Res.string.hero_mark_watched)
                                },
                                onSelected = {
                                    coroutineScope.launch {
                                        WatchingActions.togglePosterWatched(preview)
                                    }
                                },
                            ),
                            PosterZoomOverlayAction(
                                icon = Icons.Default.Edit,
                                label = "Tracking",
                                onSelected = {
                                    showPosterTrackerSheet = posterActionTarget
                                    selectedPosterActionTarget = null
                                    selectedPosterAnchor = null
                                },
                            ),
                        ),
                        hazeState = posterOverlayHazeState,
                        onDismissed = {
                            selectedPosterActionTarget = null
                            selectedPosterAnchor = null
                        },
                    )
                }
            }
            
            var showContinueWatchingTrackerSheet by remember { mutableStateOf<com.nuvio.app.features.watchprogress.ContinueWatchingItem?>(null) }
            
            if (showContinueWatchingTrackerSheet != null) {
                com.nuvio.app.features.anilist.AnimeTrackerSheet(
                    contentId = showContinueWatchingTrackerSheet!!.parentMetaId,
                    videoId = showContinueWatchingTrackerSheet!!.videoId,
                    title = showContinueWatchingTrackerSheet!!.title,
                    onDismiss = { showContinueWatchingTrackerSheet = null },
                )
            }
            if (showPosterTrackerSheet != null) {
                com.nuvio.app.features.anilist.AnimeTrackerSheet(
                    contentId = showPosterTrackerSheet!!.preview.id,
                    videoId = null,
                    title = showPosterTrackerSheet!!.preview.name,
                    onDismiss = { showPosterTrackerSheet = null },
                )
            }

            NuvioContinueWatchingActionSheet(
                item = selectedContinueWatchingForActions,
                showManualPlayOption = StreamAutoPlayPolicy.isEffectivelyEnabled(playerSettingsUiState),
                showDetailsOption = selectedContinueWatchingForActions?.isCloudLibraryContinueWatchingItem() != true,
                onDismiss = { selectedContinueWatchingForActions = null },
                onOpenDetails = {
                    selectedContinueWatchingForActions?.let { item ->
                        navController.navigate(
                            DetailRoute(
                                type = item.parentMetaType,
                                id = item.parentMetaId,
                            ),
                        )
                    }
                },
                onStartFromBeginning = selectedContinueWatchingForActions
                    ?.takeIf { !it.isNextUp }
                    ?.let { item -> { onContinueWatchingStartFromBeginning(item) } },
                onPlayManually = selectedContinueWatchingForActions
                    ?.let { item -> { onContinueWatchingPlayManually(item) } },
                onRemove = {
                    selectedContinueWatchingForActions?.let { item ->
                        if (item.isNextUp) {
                            ContinueWatchingPreferencesRepository.addDismissedNextUpKey(
                                nextUpDismissKey(
                                    item.parentMetaId,
                                    item.nextUpSeedSeasonNumber,
                                    item.nextUpSeedEpisodeNumber,
                                ),
                            )
                        } else {
                            WatchProgressRepository.removeProgress(contentId = item.parentMetaId)
                        }
                    }
                },
                onTracking = selectedContinueWatchingForActions?.let { item ->
                    { showContinueWatchingTrackerSheet = item }
                },
            )

            TraktListPickerDialog(
                visible = showLibraryListPicker,
                title = pickerTitle,
                tabs = pickerTabs,
                membership = pickerMembership,
                isPending = pickerPending,
                errorMessage = pickerError,
                onToggle = { listKey ->
                    pickerMembership = pickerMembership.toMutableMap().apply {
                        this[listKey] = !(this[listKey] == true)
                    }
                },
                onDismiss = {
                    if (!pickerPending) {
                        showLibraryListPicker = false
                        pickerItem = null
                        pickerError = null
                    }
                },
                onSave = {
                    val item = pickerItem ?: return@TraktListPickerDialog
                    coroutineScope.launch {
                        pickerPending = true
                        pickerError = null
                        runCatching {
                            LibraryRepository.applyMembershipChanges(
                                item = item,
                                desiredMembership = pickerMembership,
                            )
                        }.onSuccess {
                            showLibraryListPicker = false
                            pickerItem = null
                            pickerError = null
                        }.onFailure { error ->
                            pickerError = error.message ?: getString(Res.string.trakt_lists_update_failed)
                        }
                        pickerPending = false
                    }
                },
            )

            NuvioStatusModal(
                title = stringResource(Res.string.app_exit_title),
                message = stringResource(Res.string.app_exit_message),
                isVisible = showExitConfirmation,
                confirmText = stringResource(Res.string.action_yes),
                dismissText = stringResource(Res.string.action_no),
                onConfirm = {
                    showExitConfirmation = false
                    platformExitApp()
                },
                onDismiss = {
                    showExitConfirmation = false
                },
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = !initialHomeReady || profileSwitchLoading,
                enter = fadeIn(),
                exit = fadeOut(androidx.compose.animation.core.tween(400)),
            ) {
                AppLaunchOverlay(
                    profileColor = launchOverlayProfileColor,
                    backgroundImageUrl = launchOverlayBackgroundUrl,
                    profileName = launchOverlayProfile?.name?.takeIf { it.isNotBlank() },
                    avatarImageUrl = launchOverlayAvatarUrl,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Auto-dismiss profile switch overlay
            if (profileSwitchLoading) {
                LaunchedEffect(Unit) {
                    // Brief loading screen while home refreshes for the new profile
                    kotlinx.coroutines.delay(1200)
                    profileSwitchLoading = false
                }
            }

            NuvioFloatingPrompt(
                visible = resumePromptItem != null,
                imageUrl = resumePromptItem?.poster ?: resumePromptItem?.imageUrl,
                title = resumePromptItem?.title.orEmpty(),
                subtitle = resumePromptItem?.let { localizedContinueWatchingSubtitle(it) }.orEmpty(),
                progressFraction = resumePromptItem?.progressFraction ?: 0f,
                actionLabel = stringResource(Res.string.resume_prompt_action),
                onAction = {
                    val item = resumePromptItem ?: return@NuvioFloatingPrompt
                    resumePromptItem = null
                    openContinueWatching(item, false, false)
                },
                onDismiss = { resumePromptItem = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(15f),
            )

            NuvioDiscordWelcomeDialog(
                visible = !nuvioEnhancedSettings.discordWelcomeSeen && !discordWelcomeDismissedThisSession,
                onJoinDiscord = ::openDiscordWelcome,
                onDismiss = ::dismissDiscordWelcomeForNow,
                onDontShowAgain = ::hideDiscordWelcomePermanently,
            )

            NuvioToastHost(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(20f),
            )

            }
        }
}

@Composable
private fun LocalCrashReportDialog(
    report: LocalCrashReport?,
    onDismiss: (LocalCrashReport) -> Unit,
) {
    if (report == null || !CrashDiagnostics.reportsSupported) return

    val clipboardManager = LocalClipboardManager.current
    var copied by remember(report.id) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { onDismiss(report) },
        title = {
            Text(stringResource(Res.string.crash_report_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(Res.string.crash_report_description))
                Text(
                    text = report.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.nuvio.colors.textPrimary,
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.nuvio.colors.surfaceCard,
                ) {
                    Text(
                        text = report.details,
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.nuvio.colors.textMuted,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(report.details))
                    copied = true
                },
            ) {
                Text(
                    stringResource(
                        if (copied) {
                            Res.string.crash_report_copied
                        } else {
                            Res.string.crash_report_copy
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(report) }) {
                Text(stringResource(Res.string.action_close))
            }
        },
    )
}

@Composable
private fun NuvioDiscordWelcomeDialog(
    visible: Boolean,
    onJoinDiscord: () -> Unit,
    onDismiss: () -> Unit,
    onDontShowAgain: () -> Unit,
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5865F2)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = appIconPainter(AppIconResource.DiscordMark),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        title = {
            Text(stringResource(Res.string.nuvio_discord_welcome_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(Res.string.nuvio_discord_welcome_description),
                )
            }
        },
        confirmButton = {
            Button(onClick = onJoinDiscord) {
                Text(stringResource(Res.string.nuvio_discord_welcome_join))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDontShowAgain) {
                    Text(stringResource(Res.string.nuvio_discord_welcome_dont_show_again))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.nuvio_discord_welcome_not_now))
                }
            }
        },
    )
}

@Composable
private fun rememberGuardedPopBackStack(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    beforePop: () -> Unit = {},
): () -> Unit {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    var popHandled by remember(backStackEntry) { mutableStateOf(false) }

    return remember(navController, backStackEntry, currentBackStackEntry, popHandled, beforePop) {
        {
            if (!popHandled && currentBackStackEntry == backStackEntry) {
                popHandled = true
                beforePop()
                navController.popBackStack()
            }
        }
    }
}

@Composable
private fun AppTabHost(
    selectedTab: AppScreenTab,
    modifier: Modifier = Modifier,
    searchFocusRequestCount: Int = 0,
    rootActionsEnabled: Boolean = true,
    homeScrollToTopRequests: Flow<Unit>,
    searchScrollToTopRequests: Flow<Unit>,
    libraryScrollToTopRequests: Flow<Unit>,
    settingsRootActionRequests: Flow<Unit>,
    animateHomeCollectionGifs: Boolean = true,
    onCatalogClick: ((HomeCatalogSection) -> Unit)? = null,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    onLiveTvChannelClick: ((LiveTvChannel) -> Unit)? = null,
    onLibraryPosterClick: ((LibraryItem) -> Unit)? = null,
    onLibraryPosterLongClick: ((LibraryItem, LibrarySection) -> Unit)? = null,
    onLibrarySectionViewAllClick: ((LibrarySection) -> Unit)? = null,
    onCloudFilePlay: ((CloudLibraryItem, CloudLibraryFile) -> Unit)? = null,
    onConnectCloudClick: (() -> Unit)? = null,
    onLibraryDownloadClick: ((DownloadItem) -> Unit)? = null,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)? = null,
    onContinueWatchingLongPress: ((ContinueWatchingItem) -> Unit)? = null,
    onSwitchProfile: (() -> Unit)? = null,
    onEditProfile: (() -> Unit)? = null,
    onHomescreenSettingsClick: () -> Unit = {},
    onMetaScreenSettingsClick: () -> Unit = {},
    onContinueWatchingSettingsClick: () -> Unit = {},
    onDownloadsSettingsClick: () -> Unit = {},
    onAddonsSettingsClick: () -> Unit = {},
    onPluginsSettingsClick: () -> Unit = {},
    onAccountSettingsClick: () -> Unit = {},
    onSupportersContributorsSettingsClick: () -> Unit = {},
    onLicensesAttributionsSettingsClick: () -> Unit = {},
    onCheckForUpdatesClick: (() -> Unit)? = null,
    onTestUpdateBannerClick: (() -> Unit)? = null,
    onCollectionsSettingsClick: () -> Unit = {},
    onFolderClick: ((collectionId: String, folderId: String) -> Unit)? = null,
    requestedSettingsPageName: String? = null,
    onRequestedSettingsPageConsumed: () -> Unit = {},
    onInitialHomeContentRendered: () -> Unit = {},
) {
    val tabStateHolder = rememberSaveableStateHolder()

    Box(modifier = modifier.fillMaxSize()) {
        tabStateHolder.SaveableStateProvider(selectedTab.name) {
            when (selectedTab) {
                AppScreenTab.Home -> {
                    HomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        animateCollectionGifs = animateHomeCollectionGifs,
                        scrollToTopRequests = homeScrollToTopRequests,
                        onCatalogClick = onCatalogClick,
                        onPosterClick = onPosterClick,
                        onPosterLongClick = onPosterLongClick,
                        onContinueWatchingClick = onContinueWatchingClick,
                        onContinueWatchingLongPress = onContinueWatchingLongPress,
                        onFolderClick = onFolderClick,
                        onFirstCatalogRendered = onInitialHomeContentRendered,
                    )
                }

                AppScreenTab.Search -> {
                    SearchScreen(
                        modifier = Modifier.fillMaxSize(),
                        onPosterClick = onPosterClick,
                        onPosterLongClick = onPosterLongClick,
                        searchFocusRequestCount = searchFocusRequestCount,
                        scrollToTopRequests = searchScrollToTopRequests,
                    )
                }

                AppScreenTab.LiveTv -> {
                    LiveTvScreen(
                        modifier = Modifier.fillMaxSize(),
                        onChannelClick = { channel -> onLiveTvChannelClick?.invoke(channel) },
                    )
                }

                AppScreenTab.Library -> {
                    LibraryScreen(
                        modifier = Modifier.fillMaxSize(),
                        scrollToTopRequests = libraryScrollToTopRequests,
                        onPosterClick = onLibraryPosterClick,
                        onPosterLongClick = onLibraryPosterLongClick,
                        onSectionViewAllClick = onLibrarySectionViewAllClick,
                        onCloudFilePlay = onCloudFilePlay,
                        onConnectCloudClick = onConnectCloudClick,
                        onDownloadsClick = onDownloadsSettingsClick,
                        onOpenDownload = onLibraryDownloadClick,
                    )
                }

                AppScreenTab.Settings -> {
                    SettingsScreen(
                        modifier = Modifier.fillMaxSize(),
                        rootActionRequests = settingsRootActionRequests,
                        requestedPageName = requestedSettingsPageName,
                        onRequestedPageConsumed = onRequestedSettingsPageConsumed,
                        rootActionsEnabled = rootActionsEnabled,
                        onSwitchProfile = onSwitchProfile,
                        onEditProfile = onEditProfile,
                        onHomescreenClick = onHomescreenSettingsClick,
                        onMetaScreenClick = onMetaScreenSettingsClick,
                        onContinueWatchingClick = onContinueWatchingSettingsClick,
                        onDownloadsClick = onDownloadsSettingsClick,
                        onAddonsClick = onAddonsSettingsClick,
                        onPluginsClick = onPluginsSettingsClick,
                        onAccountClick = onAccountSettingsClick,
                        onSupportersContributorsClick = onSupportersContributorsSettingsClick,
                        onLicensesAttributionsClick = onLicensesAttributionsSettingsClick,
                        onCheckForUpdatesClick = onCheckForUpdatesClick,
                        onTestUpdateBannerClick = onTestUpdateBannerClick,
                        onCollectionsClick = onCollectionsSettingsClick,
                        onPosterClick = onPosterClick,
                        onContinueWatchingItemClick = onContinueWatchingClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabletFloatingTopBar(
    selectedTab: AppScreenTab,
    liveTvEnabled: Boolean,
    onTabSelected: (AppScreenTab) -> Unit,
    onProfileSelected: (NuvioProfile) -> Unit,
    onAddProfileRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = statusBarPadding + NuvioTokens.Space.s10, bottom = tokens.spacing.controlGap),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            color = tokens.colors.surface.copy(alpha = tokens.opacity.visible - tokens.opacity.subtle),
            shape = tokens.shapes.chip,
            tonalElevation = tokens.elevation.playerControls,
            shadowElevation = tokens.elevation.overlay,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = NuvioTokens.Space.s10, vertical = tokens.spacing.controlGap),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabletTopPillItem(
                    label = stringResource(Res.string.compose_nav_home),
                    selected = selectedTab == AppScreenTab.Home,
                    onClick = { onTabSelected(AppScreenTab.Home) },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = stringResource(Res.string.compose_nav_home),
                            modifier = Modifier.size(NuvioTokens.Space.s18),
                            tint = if (selectedTab == AppScreenTab.Home) {
                                tokens.colors.textPrimary
                            } else {
                                tokens.colors.textMuted
                            },
                        )
                    },
                )
                TabletTopPillItem(
                    label = stringResource(Res.string.compose_nav_search),
                    selected = selectedTab == AppScreenTab.Search,
                    onClick = { onTabSelected(AppScreenTab.Search) },
                    icon = {
                        Icon(
                            painter = painterResource(Res.drawable.sidebar_search),
                            contentDescription = stringResource(Res.string.compose_nav_search),
                            modifier = Modifier.size(NuvioTokens.Space.s18),
                            tint = if (selectedTab == AppScreenTab.Search) {
                                tokens.colors.textPrimary
                            } else {
                                tokens.colors.textMuted
                            },
                        )
                    },
                )
                if (liveTvEnabled) {
                    TabletTopPillItem(
                        label = stringResource(Res.string.compose_nav_live_tv),
                        selected = selectedTab == AppScreenTab.LiveTv,
                        onClick = { onTabSelected(AppScreenTab.LiveTv) },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Tv,
                                contentDescription = stringResource(Res.string.compose_nav_live_tv),
                                modifier = Modifier.size(NuvioTokens.Space.s18),
                                tint = if (selectedTab == AppScreenTab.LiveTv) {
                                    tokens.colors.textPrimary
                                } else {
                                    tokens.colors.textMuted
                                },
                            )
                        },
                    )
                }
                TabletTopPillItem(
                    label = stringResource(Res.string.compose_nav_library),
                    selected = selectedTab == AppScreenTab.Library,
                    onClick = { onTabSelected(AppScreenTab.Library) },
                    icon = {
                        Icon(
                            painter = painterResource(Res.drawable.sidebar_library),
                            contentDescription = stringResource(Res.string.compose_nav_library),
                            modifier = Modifier.size(NuvioTokens.Space.s18),
                            tint = if (selectedTab == AppScreenTab.Library) {
                                tokens.colors.textPrimary
                            } else {
                                tokens.colors.textMuted
                            },
                        )
                    },
                )
                Surface(
                    color = if (selectedTab == AppScreenTab.Settings) {
                        tokens.colors.overlaySelected
                    } else {
                        tokens.colors.surface
                    },
                    shape = tokens.shapes.chip,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = tokens.spacing.listGap, vertical = tokens.spacing.controlGap),
                        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProfileSwitcherTab(
                            selected = selectedTab == AppScreenTab.Settings,
                            onClick = { onTabSelected(AppScreenTab.Settings) },
                            onProfileSelected = onProfileSelected,
                            onAddProfileRequested = onAddProfileRequested,
                        )
                        Text(
                            text = stringResource(Res.string.compose_nav_profile),
                            modifier = Modifier.clickable { onTabSelected(AppScreenTab.Settings) },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedTab == AppScreenTab.Settings) {
                                tokens.colors.textPrimary
                            } else {
                                tokens.colors.textMuted
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun ContinueWatchingItem.isCloudLibraryContinueWatchingItem(): Boolean =
    parentMetaType.equals(CloudLibraryContentType, ignoreCase = true)

@Composable
private fun TabletTopPillItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val tokens = MaterialTheme.nuvio
    Surface(
        color = if (selected) tokens.colors.overlaySelected else tokens.colors.surface,
        shape = tokens.shapes.chip,
        tonalElevation = if (selected) tokens.elevation.raised else tokens.elevation.flat,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = tokens.components.chipHorizontalPadding, vertical = NuvioTokens.Space.s10),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    tokens.colors.textPrimary
                } else {
                    tokens.colors.textMuted
                },
            )
        }
    }
}

@Composable
private fun AppLaunchOverlay(
    profileColor: Color,
    backgroundImageUrl: String?,
    profileName: String?,
    avatarImageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = modifier
            .zIndex(NuvioTokens.Z.dialog),
        contentAlignment = Alignment.Center,
    ) {
        if (backgroundImageUrl != null) {
            AsyncImage(
                model = backgroundImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f)),
            )
        }
        ProfileMeshBackground(
            profileColor = profileColor,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (backgroundImageUrl != null) 0.7f else 1f
                },
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (profileName != null) {
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(profileColor.copy(alpha = 0.22f))
                        .border(2.dp, Color.White.copy(alpha = 0.74f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (avatarImageUrl != null) {
                        CollectionCardRemoteImage(
                            imageUrl = avatarImageUrl,
                            contentDescription = profileName,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            animateIfPossible = false,
                        )
                    } else {
                        Text(
                            text = profileName.take(1).uppercase(),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(NuvioTokens.Space.s24))
                Text(
                    text = stringResource(Res.string.profile_loading_welcome, profileName),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(NuvioTokens.Space.s8))
                Text(
                    text = stringResource(Res.string.profile_loading_message),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.76f),
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.app_logo_wordmark),
                    contentDescription = stringResource(Res.string.app_brand_name),
                    modifier = Modifier
                        .fillMaxWidth(0.48f)
                        .height(44.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(modifier = Modifier.height(tokens.spacing.sectionGap))
            CircularProgressIndicator(color = tokens.colors.accent)
        }
    }
}
