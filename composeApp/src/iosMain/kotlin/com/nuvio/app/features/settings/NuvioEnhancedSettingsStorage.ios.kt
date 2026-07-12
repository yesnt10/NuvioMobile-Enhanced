package com.nuvio.app.features.settings

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object NuvioEnhancedSettingsStorage {
    private const val payloadKey = "enhanced_settings_payload"
    private const val onboardingCompletedKey = "enhanced_onboarding_completed"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(payloadKey))
    }

    actual fun loadOnboardingCompleted(): Boolean? {
        val defaults = NSUserDefaults.standardUserDefaults
        if (defaults.objectForKey(onboardingCompletedKey) == null) return null
        return defaults.boolForKey(onboardingCompletedKey)
    }

    actual fun saveOnboardingCompleted(completed: Boolean) {
        NSUserDefaults.standardUserDefaults.setBool(completed, forKey = onboardingCompletedKey)
    }
}
