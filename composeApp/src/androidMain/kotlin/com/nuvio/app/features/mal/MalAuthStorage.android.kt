package com.nuvio.app.features.mal

import android.content.Context
import android.content.SharedPreferences
import com.nuvio.app.core.storage.ProfileScopedKey

internal actual object MalAuthStorage {
    private const val preferencesName = "nuvio_mal_auth"
    private const val payloadKey = "mal_auth_payload"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(): String? =
        preferences?.getString(ProfileScopedKey.of(payloadKey), null)

    actual fun savePayload(payload: String) {
        preferences
            ?.edit()
            ?.putString(ProfileScopedKey.of(payloadKey), payload)
            ?.apply()
    }
}
