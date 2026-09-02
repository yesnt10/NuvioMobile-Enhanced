package com.nuvio.app.features.telegram

import com.nuvio.app.features.telegram.iostelegram.NuvioTelegramCacheSize
import com.nuvio.app.features.telegram.iostelegram.NuvioTelegramClearCache
import com.nuvio.app.features.telegram.iostelegram.NuvioTelegramFree
import com.nuvio.app.features.telegram.iostelegram.NuvioTelegramPlaybackURL
import com.nuvio.app.features.telegram.iostelegram.NuvioTelegramRequest
import com.nuvio.app.features.telegram.iostelegram.NuvioTelegramStart
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

@OptIn(ExperimentalForeignApi::class)
internal actual object TelegramPlatformClient {
    actual val isSupported: Boolean = true

    actual fun start(apiId: Int, apiHash: String, appVersion: String): Boolean =
        NuvioTelegramStart(apiId, apiHash, appVersion) == 1

    actual fun request(json: String, timeoutSeconds: Double): String? =
        NuvioTelegramRequest(json, timeoutSeconds)?.let { pointer ->
            try {
                pointer.toKString()
            } finally {
                NuvioTelegramFree(pointer)
            }
        }

    actual fun playbackUrl(fileId: Int, fileSize: Long, fileName: String, mimeType: String?): String? =
        NuvioTelegramPlaybackURL(fileId, fileSize, fileName, mimeType)?.let { pointer ->
            try {
                pointer.toKString()
            } finally {
                NuvioTelegramFree(pointer)
            }
        }

    actual fun cacheSizeBytes(): Long = NuvioTelegramCacheSize()

    actual fun clearCache() = NuvioTelegramClearCache()
}
