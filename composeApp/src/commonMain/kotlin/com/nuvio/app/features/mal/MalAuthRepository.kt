package com.nuvio.app.features.mal

import co.touchlab.kermit.Logger
import com.nuvio.app.features.trakt.TraktPlatformClock
import io.ktor.http.Url
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
import kotlin.random.Random

object MalAuthRepository {
    private const val AUTHORIZE_URL = "https://myanimelist.net/v1/oauth2/authorize"

    private val log = Logger.withTag("MalAuth")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(MalAuthUiState())
    val uiState: StateFlow<MalAuthUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private var hasLoaded = false
    private var authState = MalAuthState()
    private var pendingCodeVerifier: String? = null

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
        authState = MalAuthState()
        publish()
    }

    fun snapshot(): MalAuthUiState {
        ensureLoaded()
        return _uiState.value
    }

    fun getAccessToken(): String? {
        ensureLoaded()
        return authState.accessToken
    }

    fun hasRequiredCredentials(): Boolean =
        MalConfig.CLIENT_ID.isNotBlank()

    private fun generateCodeVerifier(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~"
        return (1..128).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    fun onConnectRequested(): String? {
        ensureLoaded()
        if (!hasRequiredCredentials()) {
            publish(errorMessage = "MyAnimeList credentials missing.")
            return null
        }

        publish(
            statusMessage = "Complete sign-in in browser...",
            errorMessage = null,
        )

        val codeVerifier = generateCodeVerifier()
        pendingCodeVerifier = codeVerifier

        return "$AUTHORIZE_URL?response_type=code&client_id=${MalConfig.CLIENT_ID}&code_challenge=$codeVerifier"
    }

    fun onAuthLaunchFailed(reason: String) {
        publish(errorMessage = reason)
    }

    fun onAuthCallbackReceived(callbackUrl: String) {
        ensureLoaded()
        // Our bridge will redirect nuvioenhanced://auth/mal
        if (!callbackUrl.startsWith("nuvioenhanced://auth/mal", ignoreCase = true)) {
            return
        }

        scope.launch {
            completeAuthorizationFromCallback(callbackUrl)
        }
    }

    suspend fun refreshUserSettings(): String? {
        ensureLoaded()
        val accessToken = authState.accessToken ?: return null
        val viewer = MalApiClient.fetchViewer(accessToken) ?: return null

        authState = authState.copy(
            username = viewer.name,
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
                log.w { "Invalid MAL callback URL: ${it.message}" }
            }
            .getOrNull()

        if (parsedUrl == null) {
            publish(
                isLoading = false,
                errorMessage = "Invalid callback URL.",
            )
            return
        }

        val code = parsedUrl.parameters["code"]
        val error = parsedUrl.parameters["error"]

        if (!error.isNullOrBlank()) {
            val errorDescription = parsedUrl.parameters["error_description"] ?: "Authorization denied"
            publish(
                isLoading = false,
                errorMessage = errorDescription,
            )
            return
        }

        if (code.isNullOrBlank()) {
            publish(
                isLoading = false,
                errorMessage = "Missing access token in callback.",
            )
            return
        }

        val codeVerifier = pendingCodeVerifier
        if (codeVerifier == null) {
            publish(
                isLoading = false,
                errorMessage = "Invalid or expired authorization session.",
            )
            return
        }
        
        val tokenResponse = MalApiClient.exchangeToken(code, codeVerifier)
        
        if (tokenResponse == null) {
            publish(
                isLoading = false,
                errorMessage = "Failed to exchange authorization code.",
            )
            return
        }

        val expiresAt = TraktPlatformClock.nowEpochMs() + (tokenResponse.expiresIn * 1000)

        authState = authState.copy(
            isAuthenticated = true,
            accessToken = tokenResponse.accessToken,
            refreshToken = tokenResponse.refreshToken,
            expiresIn = tokenResponse.expiresIn,
            tokenExpiresAtMillis = expiresAt,
        )
        
        val username = refreshUserSettings()
        
        persist()
        publish(statusMessage = "Connected as ${username ?: "Unknown"}")
    }

    private fun disconnect() {
        authState = MalAuthState()
        persist()
        publish()
    }

    private fun loadFromDisk() {
        val payload = MalAuthStorage.loadPayload()
        authState = if (payload != null) {
            runCatching { json.decodeFromString<MalAuthState>(payload) }
                .onFailure { log.w(it) { "Failed to decode MAL auth state" } }
                .getOrDefault(MalAuthState())
        } else {
            MalAuthState()
        }
        hasLoaded = true
        publish()
    }

    private fun persist() {
        val payload = runCatching { json.encodeToString(authState) }
            .onFailure { log.e(it) { "Failed to encode MAL auth state" } }
            .getOrNull()
        if (payload != null) {
            MalAuthStorage.savePayload(payload)
        }
    }

    private fun publish(
        isLoading: Boolean = false,
        statusMessage: String? = null,
        errorMessage: String? = null,
    ) {
        _isAuthenticated.value = authState.isAuthenticated
        _uiState.value = MalAuthUiState(
            mode = if (authState.isAuthenticated) MalConnectionMode.CONNECTED else MalConnectionMode.DISCONNECTED,
            username = authState.username,
            isLoading = isLoading,
            credentialsConfigured = hasRequiredCredentials(),
            statusMessage = statusMessage,
            errorMessage = errorMessage,
        )
    }
}
