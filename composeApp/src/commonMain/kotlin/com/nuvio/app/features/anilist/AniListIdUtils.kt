package com.nuvio.app.features.anilist

import com.nuvio.app.features.player.skip.SkipIntroApi

internal suspend fun resolveToAniListId(
    contentId: String,
    videoId: String? = null,
): Int? {
    // 1. Check local overrides
    val override = AnimeTrackerMappingStorage.getAniListOverride(contentId)
    if (override != null) return override

    // 2. Try to extract AniList ID directly
    val rawId = contentId.trim()
    if (rawId.startsWith("anilist:", ignoreCase = true)) {
        return rawId.substringAfter(':').toIntOrNull()
    }
    val rawVid = videoId?.trim()
    if (rawVid != null && rawVid.startsWith("anilist:", ignoreCase = true)) {
        return rawVid.substringAfter(':').substringBefore(':').toIntOrNull()
    }

    // 2. Try Kits/MAL
    if (rawId.startsWith("kitsu:", ignoreCase = true)) {
        val kitsuId = rawId.substringAfter(':')
        val entry = SkipIntroApi.resolveKitsuToAnilist(kitsuId)
        return entry?.anilist
    }
    if (rawId.startsWith("mal:", ignoreCase = true)) {
        val malId = rawId.substringAfter(':')
        val entry = SkipIntroApi.resolveMalToAnilist(malId)
        return entry?.anilist
    }

    // 3. Try IMDB
    if (rawId.startsWith("tt", ignoreCase = true)) {
        val imdbId = rawId.substringBefore(':')
        val entries = SkipIntroApi.resolveImdbToAll(imdbId)
        // Best effort: take the first one that has an anilist ID
        return entries.firstOrNull { it.anilist != null }?.anilist
    }

    if (rawVid != null && rawVid.startsWith("tt", ignoreCase = true)) {
        val imdbId = rawVid.substringBefore(':')
        val entries = SkipIntroApi.resolveImdbToAll(imdbId)
        return entries.firstOrNull { it.anilist != null }?.anilist
    }

    return null
}
