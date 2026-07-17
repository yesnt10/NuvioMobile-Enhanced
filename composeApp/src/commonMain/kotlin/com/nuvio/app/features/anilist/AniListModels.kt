package com.nuvio.app.features.anilist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class AniListAuthState(
    val isAuthenticated: Boolean = false,
    val accessToken: String? = null,
    val expiresIn: Long? = null,
    val tokenExpiresAtMillis: Long? = null,
    val username: String? = null,
    val userId: Int? = null,
    val advancedScoringEnabled: Boolean = false,
    val scoreFormat: String? = null,
) {
    fun syncSignature(): String =
        "$isAuthenticated|${accessToken.orEmpty()}|$tokenExpiresAtMillis|${username.orEmpty()}|$userId|$advancedScoringEnabled|$scoreFormat"
}

data class AniListAuthUiState(
    val mode: AniListConnectionMode = AniListConnectionMode.DISCONNECTED,
    val username: String? = null,
    val isLoading: Boolean = false,
    val credentialsConfigured: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val advancedScoringEnabled: Boolean = false,
    val scoreFormat: String? = null,
)

enum class AniListConnectionMode {
    DISCONNECTED,
    CONNECTED
}

@Serializable
internal data class AniListViewerResponse(
    @SerialName("data") val data: AniListViewerData? = null
)

@Serializable
internal data class AniListViewerData(
    @SerialName("Viewer") val viewer: AniListViewer? = null
)

@Serializable
internal data class AniListViewer(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("mediaListOptions") val mediaListOptions: AniListMediaListOptions? = null
)

@Serializable
internal data class AniListMediaListOptions(
    @SerialName("scoreFormat") val scoreFormat: String? = null,
    @SerialName("animeList") val animeList: AniListMediaListTypeOptions? = null
)

@Serializable
internal data class AniListMediaListTypeOptions(
    @SerialName("advancedScoringEnabled") val advancedScoringEnabled: Boolean = false
)
