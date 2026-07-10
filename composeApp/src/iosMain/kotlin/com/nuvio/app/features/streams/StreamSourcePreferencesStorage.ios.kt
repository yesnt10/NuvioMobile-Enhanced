package com.nuvio.app.features.streams

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object StreamSourcePreferencesStorage {
    private const val pinnedSourceIdKey = "pinned_source_id"
    private const val pinnedSourceNameKey = "pinned_source_name"

    actual fun loadPinnedSourceId(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(pinnedSourceIdKey))

    actual fun savePinnedSourceId(sourceId: String) {
        NSUserDefaults.standardUserDefaults.setObject(sourceId, forKey = ProfileScopedKey.of(pinnedSourceIdKey))
    }

    actual fun loadPinnedSourceName(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(pinnedSourceNameKey))

    actual fun savePinnedSourceName(sourceName: String) {
        NSUserDefaults.standardUserDefaults.setObject(sourceName, forKey = ProfileScopedKey.of(pinnedSourceNameKey))
    }

    actual fun clearPinnedSource() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(pinnedSourceIdKey))
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(pinnedSourceNameKey))
    }
}
