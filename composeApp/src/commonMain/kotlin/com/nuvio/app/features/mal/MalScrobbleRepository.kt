package com.nuvio.app.features.mal

import co.touchlab.kermit.Logger
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlinx.coroutines.CancellationException

internal object MalScrobbleRepository {
    private val log = Logger.withTag("MalScrobble")

    private var lastScrobbleTimeMs: Long = 0
    private val minSendIntervalMs = 8_000L

    suspend fun scrobbleStop(
        contentId: String,
        videoId: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
    ) {
        val uiState = MalAuthRepository.snapshot()
        if (uiState.mode != MalConnectionMode.CONNECTED) return
        val accessToken = MalAuthRepository.getAccessToken() ?: return


        val progress = episodeNumber ?: 1

        val now = TraktPlatformClock.nowEpochMs()
        if (now - lastScrobbleTimeMs < minSendIntervalMs) return

        val animeId = resolveToMalId(contentId, videoId) ?: return

        lastScrobbleTimeMs = now

        log.d { "Scrobbling to MAL: animeId=$animeId progress=$progress" }

        var success = runCatching {
            MalApiClient.saveProgress(
                accessToken = accessToken,
                animeId = animeId,
                numWatchedEpisodes = progress,
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
            log.w(error) { "Failed to scrobble to MAL (first attempt)" }
        }.getOrDefault(false)
        
        if (!success) {
            log.d { "MAL scrobble failed, possibly new entry. Retrying with status=watching" }
            success = runCatching {
                MalApiClient.saveProgress(
                    accessToken = accessToken,
                    animeId = animeId,
                    numWatchedEpisodes = progress,
                    status = "watching"
                )
            }.onFailure { error ->
                if (error is CancellationException) throw error
                log.w(error) { "Failed to scrobble to MAL (second attempt)" }
            }.getOrDefault(false)
        }

        if (success) {
            log.d { "Successfully scrobbled to MAL" }
        } else {
            log.w { "MAL scrobble returned false" }
        }
    }
}
