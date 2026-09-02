package com.nuvio.app.features.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.AppIconResource
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.core.ui.appIconPainter
import com.nuvio.app.core.ui.nuvioTypeScale
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToLong

@Composable
internal fun PlayerControlsShell(
    title: String,
    streamTitle: String,
    providerName: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    playbackSnapshot: PlayerPlaybackSnapshot,
    displayedPositionMs: Long,
    metrics: PlayerLayoutMetrics,
    resizeMode: PlayerResizeMode,
    isLocked: Boolean,
    showPlaybackControls: Boolean = true,
    showDeviceStatusOverlay: Boolean = false,
    onLockToggle: () -> Unit,
    onBack: () -> Unit,
    onTogglePlayback: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onResizeModeClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSubtitleClick: (() -> Unit)?,
    onSubtitleSyncClick: (() -> Unit)? = null,
    onAudioClick: (() -> Unit)?,
    qualityLabel: String? = null,
    onQualityClick: (() -> Unit)? = null,
    onChannelsClick: (() -> Unit)? = null,
    onVideoSettingsClick: (() -> Unit)? = null,
    onSourcesClick: (() -> Unit)? = null,
    onEpisodesClick: (() -> Unit)? = null,
    onRandomEpisodeClick: (() -> Unit)? = null,
    onOpenInExternalPlayer: (() -> Unit)? = null,
    onSubmitIntroClick: (() -> Unit)? = null,
    parentalWarnings: List<ParentalWarning> = emptyList(),
    showParentalGuide: Boolean = false,
    onParentalGuideAnimationComplete: () -> Unit = {},
    onScrubChange: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    horizontalSafePadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val showQuietDeviceStatusOverlay = showDeviceStatusOverlay &&
        !showPlaybackControls &&
        !showParentalGuide &&
        !isLocked
    val deviceStatus = if (showDeviceStatusOverlay) {
        rememberPlayerDeviceStatus()
    } else {
        null
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (showPlaybackControls || showParentalGuide) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }

        if (showPlaybackControls) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                            ),
                        ),
                    ),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalSafePadding),
        ) {
            if (showPlaybackControls || showParentalGuide) {
                PlayerHeader(
                    title = title,
                    streamTitle = streamTitle,
                    providerName = providerName,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    episodeTitle = episodeTitle,
                    metrics = metrics,
                    isLocked = isLocked,
                    showActions = showPlaybackControls,
                    onSubmitIntroClick = onSubmitIntroClick,
                    parentalWarnings = parentalWarnings,
                    showParentalGuide = showParentalGuide,
                    onParentalGuideAnimationComplete = onParentalGuideAnimationComplete,
                    onLockToggle = onLockToggle,
                    onVideoSettingsClick = onVideoSettingsClick,
                    onRandomEpisodeClick = onRandomEpisodeClick,
                    onOpenInExternalPlayer = onOpenInExternalPlayer,
                    onBack = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Top))
                        .padding(
                            start = metrics.horizontalPadding,
                            end = metrics.horizontalPadding,
                            top = metrics.verticalPadding / 4,
                        ),
                )
            }

            if (showQuietDeviceStatusOverlay && deviceStatus != null) {
                PlayerDeviceStatusOverlay(
                    status = deviceStatus,
                    playbackSnapshot = playbackSnapshot,
                    metrics = metrics,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Top))
                        .padding(top = metrics.verticalPadding / 4),
                )
            }

            if (showPlaybackControls) {
                CenterControls(
                    snapshot = playbackSnapshot,
                    metrics = metrics,
                    onSeekBack = onSeekBack,
                    onSeekForward = onSeekForward,
                    onTogglePlayback = onTogglePlayback,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = metrics.centerLift),
                )
            }

            if (showPlaybackControls) {
                ProgressControls(
                    playbackSnapshot = playbackSnapshot,
                    displayedPositionMs = displayedPositionMs,
                    metrics = metrics,
                    resizeMode = resizeMode,
                    onScrubChange = onScrubChange,
                    onScrubFinished = onScrubFinished,
                    onResizeModeClick = onResizeModeClick,
                    onSpeedClick = onSpeedClick,
                    onSubtitleClick = onSubtitleClick,
                    onSubtitleSyncClick = onSubtitleSyncClick,
                    onAudioClick = onAudioClick,
                    qualityLabel = qualityLabel,
                    onQualityClick = onQualityClick,
                    onChannelsClick = onChannelsClick,
                    onSourcesClick = onSourcesClick,
                    onEpisodesClick = onEpisodesClick,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = metrics.horizontalPadding)
                        .padding(bottom = metrics.sliderBottomOffset),
                )
            }
        }
    }
}

@Composable
private fun PlayerDeviceStatusOverlay(
    status: PlayerDeviceStatus,
    playbackSnapshot: PlayerPlaybackSnapshot,
    metrics: PlayerLayoutMetrics,
    modifier: Modifier = Modifier,
) {
    val typeScale = MaterialTheme.nuvioTypeScale
    val remainingLabel = playerFinishRemainingLabel(playbackSnapshot)
    val finishClockLabel = playerFinishClockLabel(status.timeLabel, playbackSnapshot)
    Surface(
        modifier = modifier.widthIn(min = 220.dp, max = 460.dp),
        shape = RoundedCornerShape(999.dp),
        color = Color.Black.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerDeviceStatusItem(
                icon = when (status.networkType) {
                    PlayerDeviceNetworkType.Wifi -> Icons.Rounded.Wifi
                    PlayerDeviceNetworkType.Cellular -> Icons.Rounded.SignalCellularAlt
                    PlayerDeviceNetworkType.Offline -> Icons.Rounded.WifiOff
                    PlayerDeviceNetworkType.Unknown -> Icons.Rounded.Wifi
                },
                text = when (status.networkType) {
                    PlayerDeviceNetworkType.Wifi -> "Wi-Fi"
                    PlayerDeviceNetworkType.Cellular -> "LTE"
                    PlayerDeviceNetworkType.Offline -> "Offline"
                    PlayerDeviceNetworkType.Unknown -> "Net"
                },
                metrics = metrics,
            )
            Text(
                text = status.timeLabel,
                style = typeScale.labelSm.copy(
                    fontSize = metrics.metadataSize,
                    lineHeight = metrics.metadataSize * 1.2f,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 1,
            )
            PlayerBatteryStatusItem(
                percent = status.batteryPercent,
                charging = status.batteryCharging,
                metrics = metrics,
            )
            if (remainingLabel != null) {
                PlayerDeviceStatusItem(
                    icon = Icons.Rounded.AccessTime,
                    text = finishClockLabel?.let { finishClock ->
                        stringResource(Res.string.player_status_ends_at, finishClock, remainingLabel)
                    } ?: stringResource(Res.string.player_status_time_left, remainingLabel),
                    metrics = metrics,
                )
            }
        }
    }
}

private fun playerFinishClockLabel(
    currentTimeLabel: String,
    snapshot: PlayerPlaybackSnapshot,
): String? {
    val durationMs = snapshot.durationMs.takeIf { it > 0L } ?: return null
    val remainingMs = (durationMs - snapshot.positionMs).coerceAtLeast(0L)
    if (remainingMs <= 0L) return null
    val speed = snapshot.playbackSpeed.takeIf { it > 0.05f } ?: 1f
    val adjustedMinutes = ((remainingMs / speed) / 60_000f).roundToLong().coerceAtLeast(1L)
    val clockMatch = Regex("""(\d{1,2})\D+(\d{2})""").find(currentTimeLabel) ?: return null
    val hour = clockMatch.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
    val minute = clockMatch.groupValues.getOrNull(2)?.toIntOrNull() ?: return null
    val totalMinutes = ((hour * 60L + minute + adjustedMinutes) % (24L * 60L)).let { value ->
        if (value < 0L) value + 24L * 60L else value
    }
    val finishHour = (totalMinutes / 60L).toInt()
    val finishMinute = (totalMinutes % 60L).toInt()
    return "${finishHour.toString().padStart(2, '0')}:${finishMinute.toString().padStart(2, '0')}"
}

private fun playerFinishRemainingLabel(snapshot: PlayerPlaybackSnapshot): String? {
    val durationMs = snapshot.durationMs.takeIf { it > 0L } ?: return null
    val remainingMs = (durationMs - snapshot.positionMs).coerceAtLeast(0L)
    if (remainingMs <= 0L) return null
    val speed = snapshot.playbackSpeed.takeIf { it > 0.05f } ?: 1f
    val adjustedMinutes = ((remainingMs / speed) / 60_000f).roundToLong().coerceAtLeast(1L)
    val hours = adjustedMinutes / 60L
    val minutes = adjustedMinutes % 60L
    return when {
        hours <= 0L -> "${minutes}m"
        minutes <= 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

@Composable
private fun PlayerDeviceStatusItem(
    icon: ImageVector,
    text: String,
    metrics: PlayerLayoutMetrics,
) {
    val typeScale = MaterialTheme.nuvioTypeScale
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.88f),
            modifier = Modifier.size(metrics.headerIconSize * 0.74f),
        )
        Text(
            text = text,
            style = typeScale.labelSm.copy(
                fontSize = metrics.metadataSize,
                lineHeight = metrics.metadataSize * 1.2f,
                fontWeight = FontWeight.SemiBold,
            ),
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1,
        )
    }
}

@Composable
private fun PlayerBatteryStatusItem(
    percent: Int?,
    charging: Boolean,
    metrics: PlayerLayoutMetrics,
) {
    val typeScale = MaterialTheme.nuvioTypeScale
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerBatteryGlyph(
            percent = percent,
            charging = charging,
            metrics = metrics,
        )
        Text(
            text = percent?.let { "$it%" } ?: "--",
            style = typeScale.labelSm.copy(
                fontSize = metrics.metadataSize,
                lineHeight = metrics.metadataSize * 1.2f,
                fontWeight = FontWeight.SemiBold,
            ),
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1,
        )
    }
}

@Composable
private fun PlayerBatteryGlyph(
    percent: Int?,
    charging: Boolean,
    metrics: PlayerLayoutMetrics,
) {
    val level = percent?.coerceIn(0, 100)
    val fillFraction = (level ?: 0) / 100f
    val activeColor = if (charging) {
        Color(0xFF8CFFB3)
    } else {
        Color.White.copy(alpha = 0.9f)
    }
    val outlineColor = Color.White.copy(alpha = 0.72f)
    val bodyWidth = metrics.headerIconSize * 0.86f
    val bodyHeight = metrics.headerIconSize * 0.46f
    val capWidth = metrics.headerIconSize * 0.08f
    val capHeight = metrics.headerIconSize * 0.24f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = bodyWidth, height = bodyHeight)
                .clip(RoundedCornerShape(3.dp))
                .border(
                    width = 1.2.dp,
                    color = outlineColor,
                    shape = RoundedCornerShape(3.dp),
                )
                .padding(2.dp),
        ) {
            if (level != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fillFraction)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(activeColor),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(width = capWidth, height = capHeight)
                .clip(RoundedCornerShape(topEnd = 1.5.dp, bottomEnd = 1.5.dp))
                .background(outlineColor),
        )
    }
}

@Composable
private fun PlayerHeader(
    title: String,
    streamTitle: String,
    providerName: String,
    seasonNumber: Int?,
    episodeNumber: Int?,
    episodeTitle: String?,
    metrics: PlayerLayoutMetrics,
    isLocked: Boolean,
    showActions: Boolean,
    onSubmitIntroClick: (() -> Unit)?,
    parentalWarnings: List<ParentalWarning>,
    showParentalGuide: Boolean,
    onParentalGuideAnimationComplete: () -> Unit,
    onLockToggle: () -> Unit,
    onVideoSettingsClick: (() -> Unit)?,
    onRandomEpisodeClick: (() -> Unit)?,
    onOpenInExternalPlayer: (() -> Unit)?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeScale = MaterialTheme.nuvioTypeScale
    val metadataAlpha by animateFloatAsState(
        targetValue = if (!showParentalGuide && showActions) 1f else 0f,
        animationSpec = tween(durationMillis = if (!showParentalGuide && showActions) 260 else 160),
        label = "playerHeaderMetadataAlpha",
    )
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    modifier = Modifier.graphicsLayer { alpha = metadataAlpha },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = title,
                        style = typeScale.titleLg.copy(
                            fontSize = metrics.titleSize,
                            lineHeight = metrics.titleSize * 1.16f,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (seasonNumber != null && episodeNumber != null && !episodeTitle.isNullOrBlank()) {
                        Text(
                            text = stringResource(
                                Res.string.compose_player_episode_title_format,
                                seasonNumber,
                                episodeNumber,
                                episodeTitle,
                            ),
                            style = typeScale.bodyMd.copy(
                                fontSize = metrics.episodeInfoSize,
                                lineHeight = metrics.episodeInfoSize * 1.3f,
                            ),
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = streamTitle,
                            style = typeScale.labelSm.copy(
                                fontSize = metrics.metadataSize,
                                lineHeight = metrics.metadataSize * 1.25f,
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = providerName,
                            style = typeScale.labelSm.copy(
                                fontSize = metrics.metadataSize,
                                lineHeight = metrics.metadataSize * 1.25f,
                                fontStyle = FontStyle.Italic,
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                ParentalGuideOverlay(
                    warnings = parentalWarnings,
                    isVisible = showParentalGuide,
                    onAnimationComplete = onParentalGuideAnimationComplete,
                    contentPadding = PaddingValues(0.dp),
                )
            }

            if (showActions) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onSubmitIntroClick != null) {
                        PlayerHeaderIconButton(
                            icon = Icons.Rounded.Flag,
                            contentDescription = stringResource(Res.string.submit_intro_action),
                            buttonSize = metrics.headerIconSize + 16.dp,
                            iconSize = metrics.headerIconSize,
                            onClick = onSubmitIntroClick,
                        )
                    }
                    if (onOpenInExternalPlayer != null) {
                        PlayerHeaderIconButton(
                            icon = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = stringResource(Res.string.streams_open_external_player),
                            buttonSize = metrics.headerIconSize + 16.dp,
                            iconSize = metrics.headerIconSize,
                            onClick = onOpenInExternalPlayer,
                        )
                    }
                    if (onRandomEpisodeClick != null) {
                        PlayerHeaderIconButton(
                            icon = Icons.Rounded.Shuffle,
                            contentDescription = stringResource(Res.string.action_random_episode),
                            buttonSize = metrics.headerIconSize + 16.dp,
                            iconSize = metrics.headerIconSize,
                            onClick = onRandomEpisodeClick,
                        )
                    }
                    PlayerHeaderIconButton(
                        icon = if (isLocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        contentDescription = if (isLocked) {
                            stringResource(Res.string.compose_player_unlock_controls)
                        } else {
                            stringResource(Res.string.compose_player_lock_controls)
                        },
                        buttonSize = metrics.headerIconSize + 16.dp,
                        iconSize = metrics.headerIconSize,
                        onClick = onLockToggle,
                    )
                    if (onVideoSettingsClick != null) {
                        PlayerHeaderIconButton(
                            icon = Icons.Rounded.Build,
                            contentDescription = stringResource(Res.string.player_action_video_settings),
                            buttonSize = metrics.headerIconSize + 16.dp,
                            iconSize = metrics.headerIconSize,
                            onClick = onVideoSettingsClick,
                        )
                    }
                    NuvioBackButton(
                        onClick = onBack,
                        containerColor = Color.Black.copy(alpha = 0.35f),
                        contentColor = Color.White,
                        buttonSize = metrics.headerIconSize + 16.dp,
                        iconSize = metrics.headerIconSize,
                        contentDescription = stringResource(Res.string.compose_player_close),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    buttonSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun CenterControls(
    snapshot: PlayerPlaybackSnapshot,
    metrics: PlayerLayoutMetrics,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onTogglePlayback: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(metrics.centerGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SideControlButton(
            icon = Icons.Rounded.Replay10,
            contentDescription = stringResource(Res.string.compose_player_seek_back_10),
            metrics = metrics,
            onClick = onSeekBack,
        )
        PlayPauseControlButton(
            isPlaying = snapshot.isPlaying,
            isBuffering = snapshot.isLoading,
            metrics = metrics,
            onClick = onTogglePlayback,
        )
        SideControlButton(
            icon = Icons.Rounded.Forward10,
            contentDescription = stringResource(Res.string.compose_player_seek_forward_10),
            metrics = metrics,
            onClick = onSeekForward,
        )
    }
}

@Composable
private fun SideControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    metrics: PlayerLayoutMetrics,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(metrics.sideButtonPadding),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(metrics.playIconSize),
        )
    }
}

@Composable
private fun PlayPauseControlButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    metrics: PlayerLayoutMetrics,
    onClick: () -> Unit,
) {
    val playPausePainter = appIconPainter(
        if (isPlaying) AppIconResource.PlayerPause else AppIconResource.PlayerPlay,
    )

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(metrics.playButtonPadding),
        contentAlignment = Alignment.Center,
    ) {
        if (isBuffering) {
            NuvioLoadingIndicator(
                color = Color.White,
                modifier = Modifier.size(metrics.playIconSize),
            )
        } else {
            Icon(
                painter = playPausePainter,
                contentDescription = if (isPlaying) {
                    stringResource(Res.string.compose_action_pause)
                } else {
                    stringResource(Res.string.detail_btn_play)
                },
                tint = Color.White,
                modifier = Modifier.size(metrics.playIconSize),
            )
        }
    }
}

@Composable
private fun ProgressControls(
    playbackSnapshot: PlayerPlaybackSnapshot,
    displayedPositionMs: Long,
    metrics: PlayerLayoutMetrics,
    resizeMode: PlayerResizeMode,
    onScrubChange: (Long) -> Unit,
    onScrubFinished: (Long) -> Unit,
    onResizeModeClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSubtitleClick: (() -> Unit)?,
    onSubtitleSyncClick: (() -> Unit)? = null,
    onAudioClick: (() -> Unit)?,
    qualityLabel: String? = null,
    onQualityClick: (() -> Unit)? = null,
    onChannelsClick: (() -> Unit)? = null,
    onSourcesClick: (() -> Unit)? = null,
    onEpisodesClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val seekableDurationMs = playbackSnapshot.durationMs.takeIf { it > 0L }
    val durationMs = seekableDurationMs ?: 1L
    val aspectRatioPainter = appIconPainter(AppIconResource.PlayerAspectRatio)
    val subtitlesPainter = appIconPainter(AppIconResource.PlayerSubtitles)
    val audioPainter = appIconPainter(AppIconResource.PlayerAudioFilled)

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(metrics.sliderTouchHeight)
                .graphicsLayer(scaleY = metrics.sliderScaleY)
                .tapToSeekOnTimeline(
                    durationMs = seekableDurationMs ?: 0L,
                    onSeek = { positionMs ->
                        val targetPositionMs = positionMs.coerceIn(0L, durationMs)
                        onScrubChange(targetPositionMs)
                        onScrubFinished(targetPositionMs)
                    },
                ),
        ) {
            Slider(
                modifier = Modifier.fillMaxSize(),
                value = displayedPositionMs.coerceIn(0L, durationMs).toFloat(),
                onValueChange = { value -> onScrubChange(value.toLong()) },
                onValueChangeFinished = { onScrubFinished(displayedPositionMs.coerceIn(0L, durationMs)) },
                valueRange = 0f..durationMs.toFloat(),
                enabled = seekableDurationMs != null,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(top = 4.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimePill(text = formatPlaybackTime(displayedPositionMs), fontSize = metrics.timeSize)
            TimePill(text = formatPlaybackTime(seekableDurationMs ?: 0L), fontSize = metrics.timeSize)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(24.dp),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerActionPillButton(
                        label = stringResource(resizeMode.labelRes),
                        painter = aspectRatioPainter,
                        onClick = onResizeModeClick,
                    )
                    PlayerActionPillButton(
                        label = formatPlaybackSpeedLabel(playbackSnapshot.playbackSpeed),
                        icon = Icons.Rounded.Speed,
                        onClick = onSpeedClick,
                    )
                    if (onSubtitleClick != null) {
                        PlayerActionPillButton(
                            label = stringResource(Res.string.compose_player_subs),
                            painter = subtitlesPainter,
                            onClick = onSubtitleClick,
                        )
                    }
                    if (onSubtitleSyncClick != null) {
                        PlayerActionPillButton(
                            label = stringResource(Res.string.compose_player_sync_short),
                            icon = Icons.Rounded.Tune,
                            onClick = onSubtitleSyncClick,
                        )
                    }
                    if (onAudioClick != null) {
                        PlayerActionPillButton(
                            label = stringResource(Res.string.compose_player_audio),
                            painter = audioPainter,
                            onClick = onAudioClick,
                        )
                    }
                    if (onQualityClick != null) {
                        PlayerActionPillButton(
                            label = qualityLabel?.takeIf { it.isNotBlank() } ?: "Quality",
                            onClick = onQualityClick,
                        )
                    }
                    if (onChannelsClick != null) {
                        PlayerActionPillButton(
                            label = stringResource(Res.string.live_tv_player_channels),
                            icon = Icons.Rounded.Tv,
                            onClick = onChannelsClick,
                        )
                    }
                    if (onSourcesClick != null) {
                        PlayerActionPillButton(
                            label = stringResource(Res.string.compose_player_sources),
                            icon = Icons.Rounded.SwapHoriz,
                            onClick = onSourcesClick,
                        )
                    }
                    if (onEpisodesClick != null) {
                        PlayerActionPillButton(
                            label = stringResource(Res.string.compose_player_episodes),
                            icon = Icons.Rounded.VideoLibrary,
                            onClick = onEpisodesClick,
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.tapToSeekOnTimeline(
    durationMs: Long,
    onSeek: (Long) -> Unit,
): Modifier {
    if (durationMs <= 0L) return this

    return pointerInput(durationMs) {
        awaitEachGesture {
            val width = size.width.toFloat().takeIf { it > 0f } ?: return@awaitEachGesture
            val down = awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            val downPosition = down.position
            var lastPosition = downPosition
            var maxDistance = 0f

            while (true) {
                val event = awaitPointerEvent(pass = PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                lastPosition = change.position
                maxDistance = maxOf(maxDistance, (lastPosition - downPosition).getDistance())
                if (!change.pressed) break
            }

            if (maxDistance <= viewConfiguration.touchSlop) {
                val targetFraction = (lastPosition.x / width).coerceIn(0f, 1f)
                onSeek((durationMs * targetFraction).roundToLong().coerceIn(0L, durationMs))
            }
        }
    }
}

@Composable
internal fun LockedPlayerOverlay(
    playbackSnapshot: PlayerPlaybackSnapshot,
    displayedPositionMs: Long,
    metrics: PlayerLayoutMetrics,
    horizontalSafePadding: androidx.compose.ui.unit.Dp,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val seekableDurationMs = playbackSnapshot.durationMs.takeIf { it > 0L }
    val durationMs = seekableDurationMs ?: 1L
    val sliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.28f),
        disabledThumbColor = Color.White,
        disabledActiveTrackColor = Color.White,
        disabledInactiveTrackColor = Color.White.copy(alpha = 0.28f),
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.52f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                    .clickable(onClick = onUnlock),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = stringResource(Res.string.compose_player_unlock_controls),
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.compose_player_tap_to_unlock),
                style = MaterialTheme.nuvioTypeScale.bodyMd.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.92f),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = horizontalSafePadding + metrics.horizontalPadding)
                .padding(bottom = metrics.sliderBottomOffset),
        ) {
            Slider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(metrics.sliderTouchHeight)
                    .graphicsLayer(scaleY = metrics.sliderScaleY),
                value = displayedPositionMs.coerceIn(0L, durationMs).toFloat(),
                onValueChange = {},
                onValueChangeFinished = {},
                valueRange = 0f..durationMs.toFloat(),
                enabled = false,
                colors = sliderColors,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TimePill(text = formatPlaybackTime(displayedPositionMs), fontSize = metrics.timeSize)
                TimePill(text = formatPlaybackTime(seekableDurationMs ?: 0L), fontSize = metrics.timeSize)
            }
        }
    }
}

@Composable
private fun TimePill(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.nuvioTypeScale.labelSm.copy(
                fontSize = fontSize,
                lineHeight = fontSize * 1.25f,
                fontWeight = FontWeight.Medium,
            ),
            color = Color.White,
        )
    }
}

@Composable
private fun PlayerActionPillButton(
    label: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    painter: Painter? = null,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            painter != null -> Icon(
                painter = painter,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )

            icon != null -> Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.nuvioTypeScale.labelSm,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}
