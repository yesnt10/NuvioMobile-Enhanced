package com.nuvio.app.features.streams

internal expect object StreamSourcePreferencesStorage {
    fun loadPinnedSourceId(): String?
    fun savePinnedSourceId(sourceId: String)
    fun loadPinnedSourceName(): String?
    fun savePinnedSourceName(sourceName: String)
    fun clearPinnedSource()
}
