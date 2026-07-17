package com.nuvio.app.features.mal

import com.nuvio.app.features.player.skip.SkipIntroApi

internal suspend fun resolveToMalId(
    contentId: String,
    videoId: String? = null,
): Int? {
    // 1. Check local overrides
    val override = com.nuvio.app.features.anilist.AnimeTrackerMappingStorage.getMalOverride(contentId)
    if (override != null) return override

    // 2. Try to extract MAL ID directly
    val rawId = contentId.trim()
    if (rawId.startsWith("mal:", ignoreCase = true)) {
        return rawId.substringAfter(':').toIntOrNull()
    }
    val rawVid = videoId?.trim()
    if (rawVid != null && rawVid.startsWith("mal:", ignoreCase = true)) {
        return rawVid.substringAfter(':').substringBefore(':').toIntOrNull()
    }

    // 2. Try Kits/AniList
    if (rawId.startsWith("kitsu:", ignoreCase = true)) {
        val kitsuId = rawId.substringAfter(':')
        val entry = SkipIntroApi.resolveKitsuToMal(kitsuId)
        return entry?.myanimelist
    }
    if (rawId.startsWith("anilist:", ignoreCase = true)) {
        val aniId = rawId.substringAfter(':').toIntOrNull()
        if (aniId != null) {
            val fetchedMalId = com.nuvio.app.features.anilist.AniListApiClient.fetchMalId(aniId)
            if (fetchedMalId != null) {
                return fetchedMalId
            }
        }
    }

    // 3. Try IMDB
    if (rawId.startsWith("tt", ignoreCase = true)) {
        val imdbId = rawId.substringBefore(':')
        val entries = SkipIntroApi.resolveImdbToAll(imdbId)
        return entries.firstOrNull { it.myanimelist != null }?.myanimelist
    }

    if (rawVid != null && rawVid.startsWith("tt", ignoreCase = true)) {
        val imdbId = rawVid.substringBefore(':')
        val entries = SkipIntroApi.resolveImdbToAll(imdbId)
        return entries.firstOrNull { it.myanimelist != null }?.myanimelist
    }

    return null
}
