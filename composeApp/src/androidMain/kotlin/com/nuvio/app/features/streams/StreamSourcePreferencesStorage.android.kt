package com.nuvio.app.features.streams

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object StreamSourcePreferencesStorage {
    private const val preferencesName = "nuvio_stream_source_preferences"
    private const val pinnedSourceIdKey = "pinned_source_id"
    private const val pinnedSourceNameKey = "pinned_source_name"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPinnedSourceId(): String? =
        preferences?.getString(ProfileScopedKey.of(pinnedSourceIdKey), null)

    actual fun savePinnedSourceId(sourceId: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(pinnedSourceIdKey), sourceId)
            ?.apply()
    }

    actual fun loadPinnedSourceName(): String? =
        preferences?.getString(ProfileScopedKey.of(pinnedSourceNameKey), null)

    actual fun savePinnedSourceName(sourceName: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(pinnedSourceNameKey), sourceName)
            ?.apply()
    }

    actual fun clearPinnedSource() {
        preferences
            ?.edit()
            ?.remove(ProfileScopedKey.of(pinnedSourceIdKey))
            ?.remove(ProfileScopedKey.of(pinnedSourceNameKey))
            ?.apply()
    }
}
