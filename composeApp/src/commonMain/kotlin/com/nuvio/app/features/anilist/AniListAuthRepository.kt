package com.nuvio.app.features.anilist

import co.touchlab.kermit.Logger
import com.nuvio.app.features.trakt.TraktPlatformClock
import io.ktor.http.Url
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString

object AniListAuthRepository {
    private const val AUTHORIZE_URL = "https://anilist.co/api/v2/oauth/authorize"

    private val log = Logger.withTag("AniListAuth")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(AniListAuthUiState())
    val uiState: StateFlow<AniListAuthUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private var hasLoaded = false
    private var authState = AniListAuthState()

    init {
        ensureLoaded()
    }

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun clearLocalState() {
        hasLoaded = false
        authState = AniListAuthState()
        publish()
    }

    fun snapshot(): AniListAuthUiState {
        ensureLoaded()
        return _uiState.value
    }

    fun getAccessToken(): String? {
        ensureLoaded()
        return authState.accessToken
    }

    fun hasRequiredCredentials(): Boolean =
        AniListConfig.CLIENT_ID.isNotBlank()

    fun onConnectRequested(): String? {
        ensureLoaded()
        if (!hasRequiredCredentials()) {
            publish(errorMessage = "AniList credentials missing.") // TODO: localized
            return null
        }

        publish(
            statusMessage = "Complete sign-in in browser...",
            errorMessage = null,
        )

        return "$AUTHORIZE_URL?client_id=${AniListConfig.CLIENT_ID}&response_type=token"
    }

    fun onAuthLaunchFailed(reason: String) {
        publish(errorMessage = reason)
    }

    fun onAuthCallbackReceived(callbackUrl: String) {
        ensureLoaded()
        if (!callbackUrl.startsWith("${AniListConfig.REDIRECT_URI}#", ignoreCase = true) &&
            !callbackUrl.startsWith("${AniListConfig.REDIRECT_URI}?", ignoreCase = true)
        ) {
            return
        }

        scope.launch {
            completeAuthorizationFromCallback(callbackUrl)
        }
    }

    suspend fun refreshUserSettings(): String? {
        ensureLoaded()
        val accessToken = authState.accessToken ?: return null
        val viewer = AniListApiClient.fetchViewer(accessToken) ?: return null

        authState = authState.copy(
            username = viewer.name,
            userId = viewer.id,
            advancedScoringEnabled = viewer.mediaListOptions?.animeList?.advancedScoringEnabled ?: false,
            scoreFormat = viewer.mediaListOptions?.scoreFormat
        )
        persist()
        publish()
        return authState.username
    }

    fun onDisconnectRequested() {
        ensureLoaded()
        scope.launch {
            disconnect()
        }
    }

    private suspend fun completeAuthorizationFromCallback(callbackUrl: String) {
        publish(isLoading = true, errorMessage = null)

        val parsedUrl = runCatching { Url(callbackUrl) }
            .onFailure {
                log.w { "Invalid AniList callback URL: ${it.message}" }
            }
            .getOrNull()

        if (parsedUrl == null) {
            publish(
                isLoading = false,
                errorMessage = "Invalid callback URL.",
            )
            return
        }

        // Parse fragment or parameters for implicit token flow
        val fragmentStr = parsedUrl.fragment
        val params = mutableMapOf<String, String>()
        
        if (fragmentStr.isNotBlank()) {
            fragmentStr.split("&").forEach { pair ->
                val kv = pair.split("=")
                if (kv.size == 2) {
                    params[kv[0]] = kv[1]
                }
            }
        }
        
        // Sometimes parameters are in the query string depending on error or redirect mapping
        parsedUrl.parameters.entries().forEach { entry ->
            params[entry.key] = entry.value.firstOrNull().orEmpty()
        }

        val error = params["error"]
        if (!error.isNullOrBlank()) {
            val errorDescription = params["error_description"] ?: "Authorization denied"
            publish(
                isLoading = false,
                errorMessage = errorDescription,
            )
            return
        }

        val accessToken = params["access_token"]
        if (accessToken.isNullOrBlank()) {
            publish(
                isLoading = false,
                errorMessage = "Missing access token in callback.",
            )
            return
        }

        val expiresIn = params["expires_in"]?.toLongOrNull()
        
        val expiresAt = if (expiresIn != null) {
            TraktPlatformClock.nowEpochMs() + (expiresIn * 1000)
        } else {
            // Usually valid for a year
            TraktPlatformClock.nowEpochMs() + (31536000L * 1000L) 
        }

        authState = authState.copy(
            isAuthenticated = true,
            accessToken = accessToken,
            expiresIn = expiresIn,
            tokenExpiresAtMillis = expiresAt,
        )
        
        val username = refreshUserSettings()
        
        persist()
        publish(statusMessage = "Connected as ${username ?: "Unknown"}")
    }

    private fun disconnect() {
        authState = AniListAuthState()
        persist()
        publish()
    }

    private fun loadFromDisk() {
        val payload = AniListAuthStorage.loadPayload()
        authState = if (payload != null) {
            runCatching { json.decodeFromString<AniListAuthState>(payload) }
                .onFailure { log.w(it) { "Failed to decode AniList auth state" } }
                .getOrDefault(AniListAuthState())
        } else {
            AniListAuthState()
        }
        hasLoaded = true
        publish()
    }

    private fun persist() {
        val payload = runCatching { json.encodeToString(authState) }
            .onFailure { log.e(it) { "Failed to encode AniList auth state" } }
            .getOrNull()
        if (payload != null) {
            AniListAuthStorage.savePayload(payload)
        }
    }

    private fun publish(
        isLoading: Boolean = false,
        statusMessage: String? = null,
        errorMessage: String? = null,
    ) {
        _isAuthenticated.value = authState.isAuthenticated
        _uiState.value = AniListAuthUiState(
            mode = if (authState.isAuthenticated) AniListConnectionMode.CONNECTED else AniListConnectionMode.DISCONNECTED,
            username = authState.username,
            isLoading = isLoading,
            credentialsConfigured = hasRequiredCredentials(),
            statusMessage = statusMessage,
            errorMessage = errorMessage,
            advancedScoringEnabled = authState.advancedScoringEnabled,
            scoreFormat = authState.scoreFormat
        )
    }
}
