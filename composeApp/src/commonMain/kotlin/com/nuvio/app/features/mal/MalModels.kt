package com.nuvio.app.features.mal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MalAuthState(
    val isAuthenticated: Boolean = false,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null,
    val tokenExpiresAtMillis: Long? = null,
    val username: String? = null,
) {
    fun syncSignature(): String =
        "$isAuthenticated|${accessToken.orEmpty()}|$tokenExpiresAtMillis|${username.orEmpty()}"
}

data class MalAuthUiState(
    val mode: MalConnectionMode = MalConnectionMode.DISCONNECTED,
    val username: String? = null,
    val isLoading: Boolean = false,
    val credentialsConfigured: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
)

enum class MalConnectionMode {
    DISCONNECTED,
    CONNECTED
}

@Serializable
internal data class MalUserResponse(
    @SerialName("id") val id: Int? = null,
    @SerialName("name") val name: String? = null,
)
