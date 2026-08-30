package com.nuvio.app.features.downloads

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nuvio.app.core.deeplink.buildDownloadsDeepLinkUrl
import com.nuvio.app.features.settings.AppIconPlatform
import kotlinx.coroutines.runBlocking
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.getString
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

internal actual object DownloadsLiveStatusPlatform {
    private const val permissionRequestCode = 4708
    private const val channelId = "downloads_live_status"
    private const val foregroundNotificationId = 77_431
    private const val notificationsPrefName = "nuvio_download_live_notifications"
    private const val trackedDownloadIdsKey = "tracked_download_ids"

    private var appContext: Context? = null
    private var currentActivity: ComponentActivity? = null
    private var permissionRequestInFlight = false
    private var permissionRequestedThisSession = false
    private val lastRenderStateById = mutableMapOf<String, RenderState>()
    private val artworkCache = mutableMapOf<String, Bitmap?>()
    private var foregroundServiceRequested = false

    fun initialize(context: Context) {
        appContext = context.applicationContext
        ensureNotificationChannel()
    }

    fun bindActivity(activity: ComponentActivity) {
        currentActivity = activity
    }

    fun unbindActivity(activity: ComponentActivity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    fun handlePermissionRequestResult(requestCode: Int): Boolean {
        if (requestCode != permissionRequestCode) return false
        permissionRequestInFlight = false
        return true
    }

    actual fun onItemsChanged(items: List<DownloadItem>) {
        val context = appContext ?: return
        ensureNotificationChannel()
        syncForegroundService(context, items)
        if (!canPostNotifications(context)) {
            if (items.any { it.status == DownloadStatus.Downloading }) {
                requestNotificationPermissionIfPossible(context)
            }
            return
        }

        val manager = NotificationManagerCompat.from(context)
        val trackedBefore = preferences(context)
            .getStringSet(trackedDownloadIdsKey, emptySet())
            .orEmpty()
            .toMutableSet()
        val foregroundDownloadIds = items
            .filter { item -> item.status == DownloadStatus.Downloading }
            .map { item -> item.id }
            .toSet()

        val activeItems = items.filter { item ->
            item.status == DownloadStatus.Paused ||
                item.status == DownloadStatus.Failed ||
                (
                    item.status == DownloadStatus.Completed &&
                        lastRenderStateById[item.id]?.status != DownloadStatus.Completed &&
                        lastRenderStateById.containsKey(item.id)
                )
        }
        foregroundDownloadIds.forEach { downloadId ->
            manager.cancel(notificationId(downloadId))
            lastRenderStateById.remove(downloadId)
        }

        val trackedNow = mutableSetOf<String>()
        activeItems.forEach { item ->
            val renderState = RenderState(
                status = item.status,
                progressPercent = progressPercent(item),
                downloadedBucket = item.downloadedBytes / (512L * 1024L),
                totalBytes = item.totalBytes,
                errorMessage = item.errorMessage,
            )

            val existingState = lastRenderStateById[item.id]
            if (existingState == renderState) {
                trackedNow += item.id
                return@forEach
            }

            manager.notify(notificationId(item.id), buildNotification(context, item))
            lastRenderStateById[item.id] = renderState
            trackedNow += item.id
        }

        val staleIds = trackedBefore - trackedNow
        staleIds.forEach { downloadId ->
            manager.cancel(notificationId(downloadId))
            lastRenderStateById.remove(downloadId)
        }

        preferences(context)
            .edit()
            .putStringSet(trackedDownloadIdsKey, trackedNow)
            .apply()
    }

    private fun buildNotification(context: Context, item: DownloadItem): android.app.Notification {
        val subtitle = buildSubtitle(item)
        val metadata = buildMetadata(item)
        val artwork = loadArtworkBitmap(item)
        val launchIntent = Intent(context, com.nuvio.app.MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse(buildDownloadsDeepLinkUrl())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val launchPendingIntent = PendingIntent.getActivity(
            context,
            notificationId(item.id),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.nuvio.app.R.drawable.ic_notification_small)
            .setContentTitle(item.title)
            .setContentText(subtitle)
            .setOnlyAlertOnce(true)
            .setContentIntent(launchPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        artwork?.let { bitmap ->
            notificationBuilder
                .setLargeIcon(bitmap)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .bigLargeIcon(bitmap)
                        .setSummaryText(metadata.ifBlank { subtitle }),
                )
        } ?: notificationBuilder.setStyle(
            NotificationCompat.BigTextStyle().bigText(
                listOf(subtitle, metadata)
                    .filter { it.isNotBlank() }
                    .joinToString("\n"),
            ),
        )

        when (item.status) {
            DownloadStatus.Downloading -> {
                notificationBuilder
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .addAction(
                        0,
                        runBlocking { getString(Res.string.compose_action_pause) },
                        buildActionPendingIntent(
                            context = context,
                            action = DownloadsNotificationActionReceiver.actionPause,
                            downloadId = item.id,
                        ),
                    )
                    .addAction(
                        0,
                        runBlocking { getString(Res.string.action_cancel) },
                        buildActionPendingIntent(
                            context = context,
                            action = DownloadsNotificationActionReceiver.actionCancel,
                            downloadId = item.id,
                        ),
                    )

                val progress = progressPercent(item)
                if (progress >= 0) {
                    notificationBuilder.setProgress(100, progress, false)
                } else {
                    notificationBuilder.setProgress(100, 0, true)
                }
            }

            DownloadStatus.Paused,
            DownloadStatus.Failed -> {
                notificationBuilder
                    .setOngoing(false)
                    .setAutoCancel(false)
                    .setPriority(
                        if (item.status == DownloadStatus.Failed) {
                            NotificationCompat.PRIORITY_DEFAULT
                        } else {
                            NotificationCompat.PRIORITY_LOW
                        },
                    )
                    .setProgress(0, 0, false)
                    .addAction(
                        0,
                        runBlocking {
                            getString(
                                if (item.status == DownloadStatus.Failed) {
                                    Res.string.action_retry
                                } else {
                                    Res.string.action_resume
                                },
                            )
                        },
                        buildActionPendingIntent(
                            context = context,
                            action = DownloadsNotificationActionReceiver.actionResume,
                            downloadId = item.id,
                        ),
                    )
                    .addAction(
                        0,
                        runBlocking { getString(Res.string.action_cancel) },
                        buildActionPendingIntent(
                            context = context,
                            action = DownloadsNotificationActionReceiver.actionCancel,
                            downloadId = item.id,
                        ),
                    )
            }

            DownloadStatus.Completed -> {
                notificationBuilder
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setProgress(0, 0, false)
            }
        }

        return notificationBuilder.build()
    }

    internal fun buildForegroundNotification(
        context: Context,
        items: List<DownloadItem>,
    ): android.app.Notification {
        ensureNotificationChannel()
        val downloadingItems = items.filter { it.status == DownloadStatus.Downloading }
        val primaryItem = downloadingItems.firstOrNull()
        val launchIntent = Intent(context, com.nuvio.app.MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse(buildDownloadsDeepLinkUrl())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val launchPendingIntent = PendingIntent.getActivity(
            context,
            foregroundNotificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val contentTitle = runBlocking { getString(Res.string.downloads_channel_name) }
        val contentText = when {
            primaryItem == null -> contentTitle
            downloadingItems.size == 1 -> listOf(
                buildSubtitle(primaryItem),
                buildMetadata(primaryItem),
            ).filter { it.isNotBlank() }.joinToString(" • ")
            else -> runBlocking {
                getString(
                    Res.string.downloads_live_multiple_active,
                    downloadingItems.size,
                    buildSubtitle(primaryItem),
                )
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(com.nuvio.app.R.drawable.ic_notification_small)
            .setContentTitle(primaryItem?.title ?: contentTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setOnlyAlertOnce(true)
            .setOngoing(primaryItem != null)
            .setContentIntent(launchPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        primaryItem
            ?.let(::loadArtworkBitmap)
            ?.let { bitmap ->
                builder
                    .setLargeIcon(bitmap)
                    .setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(bitmap)
                            .setSummaryText(contentText),
                    )
            }

        primaryItem?.let { item ->
            builder
                .addAction(
                    0,
                    runBlocking { getString(Res.string.compose_action_pause) },
                    buildActionPendingIntent(
                        context = context,
                        action = DownloadsNotificationActionReceiver.actionPause,
                        downloadId = item.id,
                    ),
                )
                .addAction(
                    0,
                    runBlocking { getString(Res.string.action_cancel) },
                    buildActionPendingIntent(
                        context = context,
                        action = DownloadsNotificationActionReceiver.actionCancel,
                        downloadId = item.id,
                    ),
                )
        }

        val progress = primaryItem?.let(::progressPercent) ?: -1
        if (progress >= 0) {
            builder.setProgress(100, progress, false)
        } else if (primaryItem != null) {
            builder.setProgress(100, 0, true)
        } else {
            builder.setProgress(0, 0, false)
        }

        return builder.build()
    }

    internal fun foregroundNotificationId(): Int = foregroundNotificationId

    private fun buildSubtitle(item: DownloadItem): String {
        val detail = item.displaySubtitle.ifBlank { item.providerName }
        return when (item.status) {
            DownloadStatus.Downloading -> {
                val downloaded = formatBytes(item.downloadedBytes)
                val total = item.totalBytes?.let(::formatBytes)
                val sizeText = if (total != null) {
                    runBlocking { getString(Res.string.downloads_live_downloading_with_total, detail, downloaded, total) }
                } else {
                    runBlocking { getString(Res.string.downloads_live_downloading, detail, downloaded) }
                }
                listOfNotNull(
                    sizeText,
                    item.downloadSpeedLabel(),
                    item.downloadEtaLabel(),
                ).joinToString(" • ")
            }

            DownloadStatus.Paused -> runBlocking { getString(Res.string.downloads_live_paused, detail) }
            DownloadStatus.Failed -> item.errorMessage?.takeIf { it.isNotBlank() } ?: runBlocking { getString(Res.string.downloads_live_failed) }
            DownloadStatus.Completed -> runBlocking { getString(Res.string.downloads_live_completed) }
        }
    }

    private fun buildMetadata(item: DownloadItem): String =
        listOf(
            item.displaySubtitle,
            item.providerName,
            item.streamTitle,
            item.streamSubtitle.orEmpty(),
        )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)
            .joinToString(" • ")

    private fun artworkUrl(item: DownloadItem): String? =
        listOf(
            item.episodeThumbnail,
            item.poster,
            item.background,
            item.detailsSnapshot?.poster,
            item.detailsSnapshot?.background,
        )
            .firstOrNull { it?.startsWith("http", ignoreCase = true) == true }

    private fun loadArtworkBitmap(item: DownloadItem): Bitmap? {
        if (Looper.myLooper() == Looper.getMainLooper()) return null
        val url = artworkUrl(item) ?: return null
        synchronized(artworkCache) {
            if (artworkCache.containsKey(url)) return artworkCache[url]
        }
        val bitmap = runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 2_500
                readTimeout = 2_500
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            try {
                connection.inputStream.use(BitmapFactory::decodeStream)
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
        synchronized(artworkCache) {
            if (artworkCache.size > 24) {
                artworkCache.clear()
            }
            artworkCache[url] = bitmap
        }
        return bitmap
    }

    private fun formatBytes(bytes: Long): String {
        val safe = bytes.coerceAtLeast(0L).toDouble()
        val units = runBlocking {
            arrayOf(
                getString(Res.string.unit_bytes_b),
                getString(Res.string.unit_bytes_kb),
                getString(Res.string.unit_bytes_mb),
                getString(Res.string.unit_bytes_gb),
                getString(Res.string.unit_bytes_tb),
            )
        }
        var value = safe
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex += 1
        }
        return if (unitIndex == 0) {
            "${value.toLong()} ${units[unitIndex]}"
        } else {
            "${"%.1f".format(value)} ${units[unitIndex]}"
        }
    }

    private fun progressPercent(item: DownloadItem): Int {
        val total = item.totalBytes?.takeIf { it > 0L } ?: return -1
        return ((item.downloadedBytes.toDouble() / total.toDouble()) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun buildActionPendingIntent(
        context: Context,
        action: String,
        downloadId: String,
    ): PendingIntent {
        val intent = Intent(context, DownloadsNotificationActionReceiver::class.java).apply {
            this.action = action
            putExtra(DownloadsNotificationActionReceiver.extraDownloadId, downloadId)
        }
        return PendingIntent.getBroadcast(
            context,
            notificationId("$action:$downloadId"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureNotificationChannel() {
        val context = appContext ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        if (manager.getNotificationChannel(channelId) != null) return

        manager.createNotificationChannel(
            NotificationChannel(
                channelId,
                runBlocking { getString(Res.string.downloads_channel_name) },
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = runBlocking { getString(Res.string.downloads_channel_description) }
            },
        )
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            )
            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun requestNotificationPermissionIfPossible(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (permissionRequestInFlight || permissionRequestedThisSession) return
        val permissionState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        if (permissionState == PackageManager.PERMISSION_GRANTED) return
        val activity = currentActivity ?: return
        permissionRequestInFlight = true
        permissionRequestedThisSession = true
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            permissionRequestCode,
        )
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(notificationsPrefName, Context.MODE_PRIVATE)

    private fun syncForegroundService(context: Context, items: List<DownloadItem>) {
        if (items.any { it.status == DownloadStatus.Downloading }) {
            if (!foregroundServiceRequested) {
                DownloadsForegroundService.start(context)
                foregroundServiceRequested = true
            }
            runCatching {
                NotificationManagerCompat.from(context).notify(
                    foregroundNotificationId,
                    buildForegroundNotification(context, items),
                )
            }
        } else {
            DownloadsForegroundService.stop(context)
            foregroundServiceRequested = false
            runCatching {
                NotificationManagerCompat.from(context).cancel(foregroundNotificationId)
            }
        }
    }

    private fun notificationId(downloadId: String): Int = abs(downloadId.hashCode())

    private data class RenderState(
        val status: DownloadStatus,
        val progressPercent: Int,
        val downloadedBucket: Long,
        val totalBytes: Long?,
        val errorMessage: String?,
    )
}
