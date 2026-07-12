package com.nuvio.app.features.onboarding

import com.nuvio.app.features.settings.NuvioEnhancedSettingsStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class EnhancedOnboardingUiState(
    val visible: Boolean = false,
    val preview: Boolean = false,
)

internal object EnhancedOnboardingRepository {
    private val mutableUiState = MutableStateFlow(EnhancedOnboardingUiState())
    val uiState = mutableUiState.asStateFlow()

    private var loaded = false

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val completed = NuvioEnhancedSettingsStorage.loadOnboardingCompleted() ?: false
        mutableUiState.value = EnhancedOnboardingUiState(visible = !completed)
    }

    fun showPreview() {
        ensureLoaded()
        mutableUiState.value = EnhancedOnboardingUiState(
            visible = true,
            preview = true,
        )
    }

    fun dismiss() {
        ensureLoaded()
        if (!mutableUiState.value.preview) {
            NuvioEnhancedSettingsStorage.saveOnboardingCompleted(true)
        }
        mutableUiState.value = EnhancedOnboardingUiState()
    }
}
