package com.nuvio.app.features.anilist

internal expect object AnimeTrackerMappingStorage {
    fun getAniListOverride(contentId: String): Int?
    fun saveAniListOverride(contentId: String, aniListId: Int)
    fun removeAniListOverride(contentId: String)
    
    fun getMalOverride(contentId: String): Int?
    fun saveMalOverride(contentId: String, malId: Int)
    fun removeMalOverride(contentId: String)

    fun exportToSyncPayload(): Map<String, String>
    fun applySyncPayload(payload: Map<String, String>)
}
