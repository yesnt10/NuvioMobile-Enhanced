package com.nuvio.app.features.telegram

import com.nuvio.app.core.build.AppVersionConfig
import com.nuvio.app.features.streams.StreamBehaviorHints
import com.nuvio.app.features.streams.StreamItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

enum class TelegramAuthorizationMode {
    Unsupported,
    MissingCredentials,
    Starting,
    PhoneNumber,
    Code,
    EmailAddress,
    EmailCode,
    Password,
    Ready,
    LoggingOut,
    Error,
}

data class TelegramUiState(
    val mode: TelegramAuthorizationMode = TelegramAuthorizationMode.Starting,
    val displayName: String? = null,
    val username: String? = null,
    val errorMessage: String? = null,
    val cacheSizeBytes: Long = 0L,
    val isBusy: Boolean = false,
) {
    val isConnected: Boolean get() = mode == TelegramAuthorizationMode.Ready
}

object TelegramRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(TelegramUiState())
    val uiState: StateFlow<TelegramUiState> = _uiState.asStateFlow()

    private var initializationJob: Job? = null
    private var initialized = false

    fun ensureLoaded() {
        if (initialized) return
        initialized = true
        initializationJob = scope.launch {
            when {
                !TelegramPlatformClient.isSupported -> {
                    _uiState.value = TelegramUiState(mode = TelegramAuthorizationMode.Unsupported)
                }
                TelegramConfig.API_ID <= 0 || TelegramConfig.API_HASH.isBlank() -> {
                    _uiState.value = TelegramUiState(mode = TelegramAuthorizationMode.MissingCredentials)
                }
                !TelegramPlatformClient.start(
                    apiId = TelegramConfig.API_ID,
                    apiHash = TelegramConfig.API_HASH,
                    appVersion = AppVersionConfig.VERSION_NAME,
                ) -> {
                    _uiState.value = TelegramUiState(
                        mode = TelegramAuthorizationMode.Error,
                        errorMessage = "TDLib could not be started",
                    )
                }
                else -> pollAuthorizationState()
            }
        }
    }

    fun submitPhoneNumber(phoneNumber: String) = submitAuthenticationRequest(
        buildJsonObject {
            put("@type", "setAuthenticationPhoneNumber")
            put("phone_number", phoneNumber.trim())
            put("settings", JsonNull)
        },
    )

    fun submitCode(code: String) = submitAuthenticationRequest(
        buildJsonObject {
            put("@type", "checkAuthenticationCode")
            put("code", code.trim())
        },
    )

    fun submitEmailAddress(emailAddress: String) = submitAuthenticationRequest(
        buildJsonObject {
            put("@type", "setAuthenticationEmailAddress")
            put("email_address", emailAddress.trim())
        },
    )

    fun submitEmailCode(code: String) = submitAuthenticationRequest(
        buildJsonObject {
            put("@type", "checkAuthenticationEmailCode")
            put("code", buildJsonObject {
                put("@type", "emailAddressAuthenticationCode")
                put("code", code.trim())
            })
        },
    )

    fun submitPassword(password: String) = submitAuthenticationRequest(
        buildJsonObject {
            put("@type", "checkAuthenticationPassword")
            put("password", password)
        },
    )

    fun logOut() = submitAuthenticationRequest(
        buildJsonObject { put("@type", "logOut") },
    )

    fun refreshCacheSize() {
        scope.launch {
            _uiState.value = _uiState.value.copy(cacheSizeBytes = TelegramPlatformClient.cacheSizeBytes())
        }
    }

    fun clearCache() {
        scope.launch {
            TelegramPlatformClient.clearCache()
            _uiState.value = _uiState.value.copy(cacheSizeBytes = TelegramPlatformClient.cacheSizeBytes())
        }
    }

    suspend fun searchStreams(
        title: String,
        season: Int?,
        episode: Int?,
        limit: Int = 40,
    ): List<StreamItem> = withContext(Dispatchers.Default) {
        ensureLoaded()
        if (!_uiState.value.isConnected || title.isBlank()) return@withContext emptyList()

        val messages = buildTelegramSearchQueries(title, season, episode)
            .flatMap { query -> searchMessages(query, limit) }
            .distinctBy { message ->
                val value = message.jsonObject
                value.long("chat_id") to value.long("id")
            }
        val chatTitles = mutableMapOf<Long, String>()

        messages.mapNotNull { element ->
            val message = element.jsonObject
            val chatId = message.long("chat_id")
            val content = message.objectValue("content") ?: return@mapNotNull null
            val media = content.telegramMedia() ?: return@mapNotNull null
            if (!media.fileName.isLikelyVideoFile(media.mimeType)) return@mapNotNull null

            val playbackUrl = TelegramPlatformClient.playbackUrl(
                fileId = media.fileId,
                fileSize = media.fileSize,
                fileName = media.fileName,
                mimeType = media.mimeType,
            )
                ?: return@mapNotNull null
            val chatTitle = chatTitles.getOrPut(chatId) {
                request(
                    buildJsonObject {
                        put("@type", "getChat")
                        put("chat_id", chatId)
                    },
                    timeoutSeconds = 8.0,
                )?.string("title") ?: "Telegram"
            }
            val caption = content.objectValue("caption")?.string("text")
            StreamItem(
                name = media.fileName,
                title = media.fileName,
                description = listOfNotNull(chatTitle, caption?.takeIf { it.isNotBlank() }).joinToString(" • "),
                url = playbackUrl,
                sourceName = chatTitle,
                addonName = "Telegram",
                addonId = TELEGRAM_ADDON_ID,
                behaviorHints = StreamBehaviorHints(
                    notWebReady = true,
                    videoSize = media.fileSize,
                    filename = media.fileName,
                ),
            )
        }.distinctBy { it.url }
    }

    private fun searchMessages(query: String, limit: Int): List<kotlinx.serialization.json.JsonElement> {
        val response = request(
            buildJsonObject {
                put("@type", "searchMessages")
                put("chat_list", JsonNull)
                put("query", query)
                put("offset", "")
                put("limit", limit.coerceIn(1, 100))
                put("filter", buildJsonObject { put("@type", "searchMessagesFilterEmpty") })
                put("chat_type_filter", JsonNull)
                put("min_date", 0)
                put("max_date", 0)
            },
            timeoutSeconds = 45.0,
        ) ?: return emptyList()
        if (response.type == "error") return emptyList()
        return response["messages"]?.jsonArray.orEmpty()
    }

    private suspend fun pollAuthorizationState() {
        while (true) {
            refreshAuthorizationState()
            delay(if (_uiState.value.isConnected) 5_000L else 750L)
        }
    }

    private fun refreshAuthorizationState() {
        val response = request(buildJsonObject { put("@type", "getAuthorizationState") })
            ?: return
        val mode = when (response.type) {
            "authorizationStateWaitPhoneNumber" -> TelegramAuthorizationMode.PhoneNumber
            "authorizationStateWaitCode" -> TelegramAuthorizationMode.Code
            "authorizationStateWaitEmailAddress" -> TelegramAuthorizationMode.EmailAddress
            "authorizationStateWaitEmailCode" -> TelegramAuthorizationMode.EmailCode
            "authorizationStateWaitPassword" -> TelegramAuthorizationMode.Password
            "authorizationStateReady" -> TelegramAuthorizationMode.Ready
            "authorizationStateLoggingOut", "authorizationStateClosing", "authorizationStateClosed" ->
                TelegramAuthorizationMode.LoggingOut
            "error" -> TelegramAuthorizationMode.Error
            else -> TelegramAuthorizationMode.Starting
        }
        val current = _uiState.value
        _uiState.value = current.copy(
            mode = mode,
            errorMessage = if (response.type == "error") response.string("message") else null,
            isBusy = false,
            cacheSizeBytes = TelegramPlatformClient.cacheSizeBytes(),
        )
        if (mode == TelegramAuthorizationMode.Ready && current.displayName == null) {
            refreshProfile()
        }
    }

    private fun refreshProfile() {
        val response = request(buildJsonObject { put("@type", "getMe") }) ?: return
        if (response.type == "error") return
        val firstName = response.string("first_name").orEmpty()
        val lastName = response.string("last_name").orEmpty()
        _uiState.value = _uiState.value.copy(
            displayName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Telegram" },
            username = response.objectValue("usernames")
                ?.get("active_usernames")
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonPrimitive
                ?.contentOrNull,
        )
    }

    private fun submitAuthenticationRequest(payload: JsonObject) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, errorMessage = null)
            val response = request(payload)
            if (response == null || response.type == "error") {
                _uiState.value = _uiState.value.copy(
                    isBusy = false,
                    errorMessage = response?.string("message") ?: "Telegram request timed out",
                )
            } else {
                delay(250)
                refreshAuthorizationState()
            }
        }
    }

    private fun request(payload: JsonObject, timeoutSeconds: Double = 30.0): JsonObject? =
        TelegramPlatformClient.request(payload.toString(), timeoutSeconds)
            ?.let { response -> runCatching { json.parseToJsonElement(response).jsonObject }.getOrNull() }
}

const val TELEGRAM_ADDON_ID = "telegram"

private data class TelegramMedia(
    val fileId: Int,
    val fileSize: Long,
    val fileName: String,
    val mimeType: String?,
)

private fun JsonObject.telegramMedia(): TelegramMedia? {
    val mediaObject = when (type) {
        "messageVideo" -> objectValue("video")
        "messageDocument" -> objectValue("document")
        else -> null
    } ?: return null
    val fileObject = when (type) {
        "messageVideo" -> mediaObject.objectValue("video")
        else -> mediaObject.objectValue("document")
    } ?: return null
    val fileId = fileObject.int("id").takeIf { it > 0 } ?: return null
    val fileSize = maxOf(fileObject.long("size"), fileObject.long("expected_size"))
        .takeIf { it > 0 } ?: return null
    return TelegramMedia(
        fileId = fileId,
        fileSize = fileSize,
        fileName = mediaObject.string("file_name")?.takeIf { it.isNotBlank() }
            ?: "Telegram video $fileId",
        mimeType = mediaObject.string("mime_type"),
    )
}

private fun buildTelegramSearchQueries(title: String, season: Int?, episode: Int?): List<String> {
    val normalizedTitle = title
        .replace(Regex("""[\\/:*?\"<>|]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
    if (season == null || episode == null) return listOf(normalizedTitle)

    val paddedSeason = season.toString().padStart(2, '0')
    val paddedEpisode = episode.toString().padStart(2, '0')
    return listOf(
        "$normalizedTitle S${paddedSeason}E$paddedEpisode",
        "$normalizedTitle S${season}E${episode}",
        "$normalizedTitle ${season}x$paddedEpisode",
        "$normalizedTitle Season $season Episode $episode",
    ).distinct()
}

private fun String.isLikelyVideoFile(mimeType: String?): Boolean =
    mimeType?.startsWith("video/", ignoreCase = true) == true ||
        lowercase().substringAfterLast('.', "") in setOf("mkv", "mp4", "m4v", "avi", "mov", "webm", "ts", "m2ts")

private val JsonObject.type: String? get() = string("@type")
private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.int(key: String): Int = this[key]?.jsonPrimitive?.intOrNull ?: 0
private fun JsonObject.long(key: String): Long = this[key]?.jsonPrimitive?.longOrNull ?: 0L
private fun JsonObject.objectValue(key: String): JsonObject? = this[key] as? JsonObject
