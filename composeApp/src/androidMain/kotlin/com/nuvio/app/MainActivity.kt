package com.nuvio.app

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nuvio.app.core.auth.AuthStorage
import com.nuvio.app.core.diagnostics.CrashDiagnostics
import com.nuvio.app.core.diagnostics.SentryInitializer
import com.nuvio.app.core.deeplink.handleAppUrl
import com.nuvio.app.core.network.DnsOverHttpsSettingsStorage
import com.nuvio.app.core.storage.PlatformLocalAccountDataCleaner
import com.nuvio.app.core.sync.SyncClientIdentityStorage
import com.nuvio.app.core.ui.AppSystemUiController
import com.nuvio.app.features.addons.AddonStorage
import com.nuvio.app.features.ai.AiAssistantSettingsStorage
import com.nuvio.app.features.collection.CollectionMobileSettingsStorage
import com.nuvio.app.features.collection.CollectionStorage
import com.nuvio.app.features.cloudstream.CloudStreamPlatformStorage
import com.nuvio.app.features.debrid.DebridSettingsStorage
import com.nuvio.app.features.downloads.DownloadsLiveStatusPlatform
import com.nuvio.app.features.downloads.DownloadsPlatformDownloader
import com.nuvio.app.features.downloads.DownloadsStorage
import com.nuvio.app.features.library.LibraryStorage
import com.nuvio.app.features.livetv.LiveTvIncomingSourceRepository
import com.nuvio.app.features.livetv.LiveTvStorage
import com.nuvio.app.features.details.MetaScreenSettingsStorage
import com.nuvio.app.features.home.HomeCatalogSettingsStorage
import com.nuvio.app.features.mdblist.MdbListSettingsStorage
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationPlatform
import com.nuvio.app.features.notifications.EpisodeReleaseNotificationsStorage
import com.nuvio.app.features.player.PlayerSettingsStorage
import com.nuvio.app.features.player.PlayerTrackPreferenceStorage
import com.nuvio.app.features.player.ExternalPlayerPlatform
import com.nuvio.app.features.player.SubtitleFileCache
import com.nuvio.app.features.player.SubtitleFontFileBridge
import com.nuvio.app.features.player.PlayerPictureInPictureManager
import com.nuvio.app.features.p2p.P2pSettingsStorage
import com.nuvio.app.features.p2p.P2pStreamingEngine
import com.nuvio.app.features.plugins.PluginStorage
import com.nuvio.app.features.profiles.AvatarStorage
import com.nuvio.app.features.profiles.ProfilePinCacheStorage
import com.nuvio.app.features.profiles.ProfileStorage
import com.nuvio.app.features.details.SeasonViewModeStorage
import com.nuvio.app.features.search.SearchHistoryStorage
import com.nuvio.app.features.settings.SentrySettingsStorage
import com.nuvio.app.features.settings.ThemeSettingsStorage
import com.nuvio.app.features.settings.NuvioAppIconSwitcher
import com.nuvio.app.features.settings.NuvioEnhancedBackupFileBridge
import com.nuvio.app.features.settings.NuvioEnhancedSettingsStorage
import com.nuvio.app.features.trakt.TraktAuthStorage
import com.nuvio.app.features.trakt.TraktCommentsStorage
import com.nuvio.app.features.trakt.TraktLibraryStorage
import com.nuvio.app.features.trakt.TraktSettingsStorage
import com.nuvio.app.features.tmdb.TmdbSettingsStorage
import com.nuvio.app.features.updater.AndroidAppUpdaterPlatform
import com.nuvio.app.core.ui.PosterCardStyleStorage
import com.nuvio.app.features.watched.WatchedStorage
import com.nuvio.app.features.streams.StreamLinkCacheStorage
import com.nuvio.app.features.streams.StreamBadgeSettingsStorage
import com.nuvio.app.features.streams.StreamSourcePreferencesStorage
import com.nuvio.app.features.streams.BingeGroupCacheStorage
import com.nuvio.app.features.watchprogress.ContinueWatchingEnrichmentStorage
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesStorage
import com.nuvio.app.features.watchprogress.ResumePromptStorage
import com.nuvio.app.features.watchprogress.WatchProgressStorage

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        CrashDiagnostics.initialize(applicationContext)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = 0x00000000,
            ),
            navigationBarStyle = SystemBarStyle.dark(
                scrim = 0xFF020404.toInt(),
            ),
        )
        ThemeSettingsStorage.initialize(applicationContext)
        NuvioAppIconSwitcher.initialize(applicationContext)
        NuvioEnhancedSettingsStorage.initialize(applicationContext)
        SentrySettingsStorage.initialize(applicationContext)
        SentryInitializer.start(application)
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.nuvio_background)
        SyncClientIdentityStorage.initialize(applicationContext)
        AddonStorage.initialize(applicationContext)
        CloudStreamPlatformStorage.initialize(this)
        AiAssistantSettingsStorage.initialize(applicationContext)
        AuthStorage.initialize(applicationContext)
        DnsOverHttpsSettingsStorage.initialize(applicationContext)
        LibraryStorage.initialize(applicationContext)
        LiveTvStorage.initialize(applicationContext)
        WatchedStorage.initialize(applicationContext)
        MetaScreenSettingsStorage.initialize(applicationContext)
        HomeCatalogSettingsStorage.initialize(applicationContext)
        PlayerSettingsStorage.initialize(applicationContext)
        PlayerTrackPreferenceStorage.initialize(applicationContext)
        P2pSettingsStorage.initialize(applicationContext)
        P2pStreamingEngine.initialize(applicationContext)
        ExternalPlayerPlatform.initialize(applicationContext)
        SubtitleFileCache.initialize(applicationContext)
        ProfileStorage.initialize(applicationContext)
        AvatarStorage.initialize(applicationContext)
        ProfilePinCacheStorage.initialize(applicationContext)
        SearchHistoryStorage.initialize(applicationContext)
        SeasonViewModeStorage.initialize(applicationContext)
        PosterCardStyleStorage.initialize(applicationContext)
        DebridSettingsStorage.initialize(applicationContext)
        TmdbSettingsStorage.initialize(applicationContext)
        MdbListSettingsStorage.initialize(applicationContext)
        TraktAuthStorage.initialize(applicationContext)
        com.nuvio.app.features.anilist.AniListAuthStorage.initialize(applicationContext)
        com.nuvio.app.features.anilist.AnimeTrackerMappingStorage.initialize(applicationContext)
        com.nuvio.app.features.mal.MalAuthStorage.initialize(applicationContext)
        TraktCommentsStorage.initialize(applicationContext)
        TraktLibraryStorage.initialize(applicationContext)
        TraktSettingsStorage.initialize(applicationContext)
        ContinueWatchingPreferencesStorage.initialize(applicationContext)
        ResumePromptStorage.initialize(applicationContext)
        ContinueWatchingEnrichmentStorage.initialize(applicationContext)
        EpisodeReleaseNotificationsStorage.initialize(applicationContext)
        WatchProgressStorage.initialize(applicationContext)
        StreamLinkCacheStorage.initialize(applicationContext)
        StreamBadgeSettingsStorage.initialize(applicationContext)
        StreamSourcePreferencesStorage.initialize(applicationContext)
        BingeGroupCacheStorage.initialize(applicationContext)
        PluginStorage.initialize(applicationContext)
        CollectionMobileSettingsStorage.initialize(applicationContext)
        CollectionStorage.initialize(applicationContext)
        DownloadsStorage.initialize(applicationContext)
        DownloadsPlatformDownloader.initialize(applicationContext)
        DownloadsLiveStatusPlatform.initialize(applicationContext)
        AndroidAppUpdaterPlatform.initialize(applicationContext)
        PlatformLocalAccountDataCleaner.initialize(applicationContext)
        EpisodeReleaseNotificationPlatform.initialize(applicationContext)
        EpisodeReleaseNotificationPlatform.bindActivity(this)
        NuvioEnhancedBackupFileBridge.bindActivity(this)
        SubtitleFontFileBridge.bindActivity(this)
        AppSystemUiController.bind(this)
        handleIncomingAppIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingAppIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        PlayerPictureInPictureManager.onUserLeaveHint(this)
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PlayerPictureInPictureManager.onPictureInPictureModeChanged(this, isInPictureInPictureMode)
    }

    override fun onDestroy() {
        EpisodeReleaseNotificationPlatform.unbindActivity(this)
        NuvioEnhancedBackupFileBridge.unbindActivity(this)
        SubtitleFontFileBridge.unbindActivity(this)
        AppSystemUiController.unbind(this)
        super.onDestroy()
    }

    @Deprecated("Deprecated in Android platform APIs, still used for Storage Access Framework callbacks here.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (NuvioEnhancedBackupFileBridge.handleActivityResult(requestCode, resultCode, data)) {
            return
        }
        if (SubtitleFontFileBridge.handleActivityResult(requestCode, resultCode, data)) {
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        if (EpisodeReleaseNotificationPlatform.handlePermissionRequestResult(requestCode, grantResults)) {
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun handleIncomingAppIntent(intent: Intent?) {
        if (intent == null) return
        if (handleSharedStreamIntent(intent)) {
            return
        }
        val appUrl = intent.dataString?.trim().orEmpty()
        if (appUrl.isBlank()) return
        handleAppUrl(appUrl)
    }

    private fun handleSharedStreamIntent(intent: Intent): Boolean {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
                if (!sharedText.isNullOrBlank()) {
                    LiveTvIncomingSourceRepository.submitText(sharedText)
                    return true
                }
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null && submitPlaylistUri(uri)) {
                    return true
                }
            }

            Intent.ACTION_VIEW -> {
                val dataString = intent.dataString?.trim().orEmpty()
                if (dataString.startsWith("magnet:", ignoreCase = true)) {
                    LiveTvIncomingSourceRepository.submitText(dataString)
                    return true
                }
                if (dataString.startsWith("http://", ignoreCase = true) ||
                    dataString.startsWith("https://", ignoreCase = true)
                ) {
                    val loweredPath = dataString.substringBefore('?').substringBefore('#').lowercase()
                    if (
                        loweredPath.endsWith(".m3u") ||
                        loweredPath.endsWith(".m3u8") ||
                        loweredPath.endsWith(".mp4") ||
                        loweredPath.endsWith(".mkv") ||
                        loweredPath.endsWith(".webm") ||
                        loweredPath.endsWith(".mov") ||
                        loweredPath.endsWith(".ts")
                    ) {
                        LiveTvIncomingSourceRepository.submitText(dataString)
                        return true
                    }
                }
                val data = intent.data
                if (data != null && submitPlaylistUri(data)) {
                    return true
                }
            }
        }
        return false
    }

    private fun submitPlaylistUri(uri: Uri): Boolean {
        val text = runCatching {
            contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().use { reader -> reader.readText() }
            }
        }.getOrNull()?.trim().orEmpty()
        if (text.isBlank()) return false
        val fileName = displayNameFor(uri)
        LiveTvIncomingSourceRepository.submitPlaylistData(fileName = fileName, data = text)
        return true
    }

    private fun displayNameFor(uri: Uri): String {
        if (uri.scheme == "file") {
            return uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "Shared M3U playlist" }
        }
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull().orEmpty().ifBlank { "Shared M3U playlist" }
    }
}
