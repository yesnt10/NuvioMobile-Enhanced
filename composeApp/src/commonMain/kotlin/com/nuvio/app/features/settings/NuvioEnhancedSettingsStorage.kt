package com.nuvio.app.features.settings

internal expect object NuvioEnhancedSettingsStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
    fun loadOnboardingCompleted(): Boolean?
    fun saveOnboardingCompleted(completed: Boolean)
}
