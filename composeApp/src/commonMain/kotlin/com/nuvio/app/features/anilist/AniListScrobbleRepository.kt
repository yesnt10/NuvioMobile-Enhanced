package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.CancellationException

internal object AniListScrobbleRepository {
    private val log = Logger.withTag("AniListScrobble")

    private var lastScrobbleTimeMs: Long = 0
    private val minSendIntervalMs = 8_000L

    suspend fun scrobbleStop(
        contentId: String,
        videoId: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
    ) {
        val uiState = AniListAuthRepository.snapshot()
        if (uiState.mode != AniListConnectionMode.CONNECTED) return
        val accessToken = AniListAuthRepository.getAccessToken() ?: return


        // We only care about episodes for anime. If episodeNumber is null, we can't really track progress accurately on AniList.
        // Also, AniList tracks absolute episodes (1..1000). If seasonNumber > 1, mapping is needed.
        // We will do a best effort using the resolved AniList ID. If it's a TV show, AniList often splits seasons into different entries.
        // So season 2 episode 1 might just be episode 1 of the new mediaId.
        val progress = episodeNumber ?: 1

        val now = TraktPlatformClock.nowEpochMs()
        if (now - lastScrobbleTimeMs < minSendIntervalMs) return

        val mediaId = resolveToAniListId(contentId, videoId) ?: return

        lastScrobbleTimeMs = now

        log.d { "Scrobbling to AniList: mediaId=$mediaId progress=$progress" }

        val success = runCatching {
            AniListApiClient.saveProgress(
                accessToken = accessToken,
                mediaId = mediaId,
                progress = progress,
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
            log.w(error) { "Failed to scrobble to AniList" }
        }.getOrDefault(false)

        if (success) {
            log.d { "Successfully scrobbled to AniList" }
        } else {
            log.w { "AniList scrobble returned false" }
        }
    }
}
