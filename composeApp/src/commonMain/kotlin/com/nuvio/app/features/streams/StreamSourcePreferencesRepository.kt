package com.nuvio.app.features.streams

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StreamSourcePreferencesUiState(
    val pinnedSourceId: String? = null,
    val pinnedSourceName: String? = null,
)

object StreamSourcePreferencesRepository {
    private val _uiState = MutableStateFlow(StreamSourcePreferencesUiState())
    val uiState: StateFlow<StreamSourcePreferencesUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var pinnedSourceId: String? = null
    private var pinnedSourceName: String? = null

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        pinnedSourceId = null
        pinnedSourceName = null
        _uiState.value = StreamSourcePreferencesUiState()
    }

    fun pinSource(sourceId: String, sourceName: String) {
        ensureLoaded()
        val normalizedId = sourceId.trim()
        val normalizedName = sourceName.trim()
        if (normalizedId.isBlank() || normalizedName.isBlank()) return
        if (pinnedSourceId == normalizedId && pinnedSourceName == normalizedName) return

        pinnedSourceId = normalizedId
        pinnedSourceName = normalizedName
        publish()
        StreamSourcePreferencesStorage.savePinnedSourceId(normalizedId)
        StreamSourcePreferencesStorage.savePinnedSourceName(normalizedName)
    }

    fun clearPinnedSource() {
        ensureLoaded()
        if (pinnedSourceId == null && pinnedSourceName == null) return
        pinnedSourceId = null
        pinnedSourceName = null
        publish()
        StreamSourcePreferencesStorage.clearPinnedSource()
    }

    private fun loadFromDisk() {
        hasLoaded = true
        pinnedSourceId = StreamSourcePreferencesStorage.loadPinnedSourceId()?.trim()?.takeIf { it.isNotBlank() }
        pinnedSourceName = StreamSourcePreferencesStorage.loadPinnedSourceName()?.trim()?.takeIf { it.isNotBlank() }
        if (pinnedSourceId == null || pinnedSourceName == null) {
            pinnedSourceId = null
            pinnedSourceName = null
        }
        publish()
    }

    private fun publish() {
        _uiState.value = StreamSourcePreferencesUiState(
            pinnedSourceId = pinnedSourceId,
            pinnedSourceName = pinnedSourceName,
        )
    }
}
