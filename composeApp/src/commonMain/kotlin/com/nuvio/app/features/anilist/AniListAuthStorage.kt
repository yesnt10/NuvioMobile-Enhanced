package com.nuvio.app.features.anilist

internal expect object AniListAuthStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
