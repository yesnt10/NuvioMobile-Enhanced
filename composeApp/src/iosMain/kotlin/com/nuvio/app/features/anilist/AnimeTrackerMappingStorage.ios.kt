package com.nuvio.app.features.anilist

import platform.Foundation.NSUserDefaults

internal actual object AnimeTrackerMappingStorage {
    private const val prefixAniList = "nuvio_tracker_anilist_"
    private const val prefixMal = "nuvio_tracker_mal_"

    actual fun getAniListOverride(contentId: String): Int? {
        val id = NSUserDefaults.standardUserDefaults.integerForKey(prefixAniList + contentId).toInt()
        return if (id != 0) id else null
    }

    actual fun saveAniListOverride(contentId: String, aniListId: Int) {
        NSUserDefaults.standardUserDefaults.setInteger(aniListId.toLong(), forKey = prefixAniList + contentId)
    }

    actual fun removeAniListOverride(contentId: String) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(prefixAniList + contentId)
    }

    actual fun getMalOverride(contentId: String): Int? {
        val id = NSUserDefaults.standardUserDefaults.integerForKey(prefixMal + contentId).toInt()
        return if (id != 0) id else null
    }

    actual fun saveMalOverride(contentId: String, malId: Int) {
        NSUserDefaults.standardUserDefaults.setInteger(malId.toLong(), forKey = prefixMal + contentId)
    }

    actual fun removeMalOverride(contentId: String) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(prefixMal + contentId)
    }

    actual fun exportToSyncPayload(): Map<String, String> {
        val defaults = NSUserDefaults.standardUserDefaults.dictionaryRepresentation()
        val payload = mutableMapOf<String, String>()
        for ((key, value) in defaults) {
            val keyStr = key.toString()
            if (keyStr.startsWith(prefixAniList) || keyStr.startsWith(prefixMal)) {
                payload[keyStr] = value.toString()
            }
        }
        return payload
    }

    actual fun applySyncPayload(payload: Map<String, String>) {
        for ((key, value) in payload) {
            value.toIntOrNull()?.let {
                NSUserDefaults.standardUserDefaults.setInteger(it.toLong(), forKey = key)
            }
        }
    }
}
