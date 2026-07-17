package com.nuvio.app.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SubtitleStylePanel(
    style: SubtitleStyleState,
    subtitleDelayMs: Int,
    isCompact: Boolean,
    onStyleChanged: (SubtitleStyleState) -> Unit,
    onSubtitleDelayChanged: (Int) -> Unit,
    onSubtitleDelayReset: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val sectionPadding = if (isCompact) 12.dp else 16.dp
    val gap = if (isCompact) 12.dp else 16.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        StyleControlsCard(
            style = style,
            subtitleDelayMs = subtitleDelayMs,
            isCompact = isCompact,
            sectionPadding = sectionPadding,
            colorScheme = colorScheme,
            onStyleChanged = onStyleChanged,
            onSubtitleDelayChanged = onSubtitleDelayChanged,
            onSubtitleDelayReset = onSubtitleDelayReset,
        )
    }
}

@Composable
private fun StyleControlsCard(
    style: SubtitleStyleState,
    subtitleDelayMs: Int,
    isCompact: Boolean,
    sectionPadding: androidx.compose.ui.unit.Dp,
    colorScheme: androidx.compose.material3.ColorScheme,
    onStyleChanged: (SubtitleStyleState) -> Unit,
    onSubtitleDelayChanged: (Int) -> Unit,
    onSubtitleDelayReset: () -> Unit,
) {
    val btnSize = if (isCompact) 28.dp else 32.dp
    val btnRadius = if (isCompact) 14.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(sectionPadding),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp),
    ) {
        SectionHeader(
            icon = Icons.Rounded.Tune,
            label = stringResource(Res.string.compose_player_style),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.compose_player_subtitle_delay),
                color = colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            StepperControl(
                value = formatSubtitleDelay(subtitleDelayMs),
                onMinus = {
                    onSubtitleDelayChanged((subtitleDelayMs - SUBTITLE_DELAY_STEP_MS).coerceAtLeast(SUBTITLE_DELAY_MIN_MS))
                },
                onPlus = {
                    onSubtitleDelayChanged((subtitleDelayMs + SUBTITLE_DELAY_STEP_MS).coerceAtMost(SUBTITLE_DELAY_MAX_MS))
                },
                buttonSize = btnSize,
                buttonRadius = btnRadius,
                minWidth = 72.dp,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            SmallActionPill(
                text = stringResource(Res.string.compose_player_reset),
                onClick = onSubtitleDelayReset,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.compose_player_font_size),
                color = colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            StepperControl(
                value = stringResource(Res.string.compose_player_font_size_value, style.fontSizeSp),
                onMinus = {
                    onStyleChanged(style.copy(fontSizeSp = (style.fontSizeSp - 2).coerceAtLeast(12)))
                },
                onPlus = {
                    onStyleChanged(style.copy(fontSizeSp = (style.fontSizeSp + 2).coerceAtMost(40)))
                },
                buttonSize = btnSize,
                buttonRadius = btnRadius,
                minWidth = 58.dp,
                minusIcon = Icons.Rounded.KeyboardArrowDown,
                plusIcon = Icons.Rounded.KeyboardArrowUp,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.compose_player_font_family),
                color = colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SubtitleFontFamily.entries.forEach { family ->
                    val label = if (family == SubtitleFontFamily.Custom) {
                        style.customFontName?.takeIf { it.isNotBlank() }
                            ?: stringResource(Res.string.compose_player_font_custom)
                    } else {
                        family.displayLabel()
                    }
                    SubtitleFontFamilyChip(
                        label = label,
                        selected = style.fontFamily == family,
                        onClick = {
                            if (family != SubtitleFontFamily.Custom || !style.customFontPath.isNullOrBlank()) {
                                onStyleChanged(style.copy(fontFamily = family))
                            }
                        },
                        isCompact = isCompact,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SubtitleFontActionChip(
                    label = stringResource(Res.string.compose_player_font_import),
                    onClick = {
                        SubtitleFontFileBridge.importFont { result ->
                            result.onSuccess { font ->
                                onStyleChanged(
                                    style.copy(
                                        fontFamily = SubtitleFontFamily.Custom,
                                        customFontName = font.displayName,
                                        customFontPath = font.path,
                                    ),
                                )
                            }
                        }
                    },
                    isCompact = isCompact,
                )
                if (!style.customFontPath.isNullOrBlank()) {
                    SubtitleFontActionChip(
                        label = stringResource(Res.string.compose_player_font_clear_custom),
                        onClick = {
                            onStyleChanged(
                                style.copy(
                                    fontFamily = SubtitleFontFamily.System,
                                    customFontName = null,
                                    customFontPath = null,
                                ),
                            )
                        },
                        isCompact = isCompact,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.compose_player_outline),
                color = colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (style.outlineEnabled) colorScheme.primaryContainer
                        else colorScheme.surface.copy(alpha = 0.8f)
                    )
                    .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                    .clickable { onStyleChanged(style.copy(outlineEnabled = !style.outlineEnabled)) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = if (style.outlineEnabled) stringResource(Res.string.compose_action_on)
                    else stringResource(Res.string.compose_action_off),
                    color = if (style.outlineEnabled) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }

        ToggleRow(
            label = stringResource(Res.string.compose_player_bold),
            enabled = style.bold,
            onToggle = { onStyleChanged(style.copy(bold = !style.bold)) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.compose_player_bottom_offset),
                color = colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            StepperControl(
                value = style.bottomOffset.toString(),
                onMinus = { onStyleChanged(style.copy(bottomOffset = (style.bottomOffset - 5).coerceAtLeast(0))) },
                onPlus = { onStyleChanged(style.copy(bottomOffset = (style.bottomOffset + 5).coerceAtMost(200))) },
                buttonSize = btnSize,
                buttonRadius = btnRadius,
                minWidth = 46.dp,
                minusIcon = Icons.Rounded.KeyboardArrowDown,
                plusIcon = Icons.Rounded.KeyboardArrowUp,
            )
        }

        ColorPickerRow(
            label = stringResource(Res.string.compose_player_color),
            colors = SubtitleColorSwatches,
            selectedColor = style.textColor,
            onColorSelected = { onStyleChanged(style.copy(textColor = it)) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val currentAlphaPercent = (style.textColor.alpha * 100f).roundToInt().coerceIn(0, 100)
            Text(
                text = stringResource(Res.string.compose_player_text_opacity),
                color = colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            StepperControl(
                value = "$currentAlphaPercent%",
                onMinus = {
                    val newAlpha = (currentAlphaPercent - 10).coerceAtLeast(0) / 100f
                    onStyleChanged(style.copy(textColor = style.textColor.copy(alpha = newAlpha)))
                },
                onPlus = {
                    val newAlpha = (currentAlphaPercent + 10).coerceAtMost(100) / 100f
                    onStyleChanged(style.copy(textColor = style.textColor.copy(alpha = newAlpha)))
                },
                buttonSize = btnSize,
                buttonRadius = btnRadius,
                minWidth = 58.dp,
            )
        }

        ColorPickerRow(
            label = stringResource(Res.string.compose_player_outline_color),
            colors = SubtitleColorSwatches,
            selectedColor = style.outlineColor,
            onColorSelected = { onStyleChanged(style.copy(outlineColor = it)) },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.surface.copy(alpha = 0.82f))
                    .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .clickable { onStyleChanged(SubtitleStyleState.DEFAULT) }
                    .padding(horizontal = if (isCompact) 8.dp else 12.dp, vertical = if (isCompact) 6.dp else 8.dp),
            ) {
                Text(
                    text = stringResource(Res.string.compose_player_reset_defaults),
                    color = colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (isCompact) 12.sp else 14.sp,
                )
            }
        }
    }
}

@Composable
private fun SubtitleFontFamilyChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isCompact: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) colorScheme.primaryContainer
                else colorScheme.surface.copy(alpha = 0.8f),
            )
            .border(
                width = 1.dp,
                color = if (selected) colorScheme.primary.copy(alpha = 0.65f)
                else colorScheme.outlineVariant.copy(alpha = 0.75f),
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (isCompact) 10.dp else 12.dp, vertical = if (isCompact) 7.dp else 8.dp),
    ) {
        Text(
            text = label,
            color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
            fontSize = if (isCompact) 12.sp else 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private fun SubtitleFontFamily.displayLabel(): String =
    when (this) {
        SubtitleFontFamily.System -> "System"
        SubtitleFontFamily.SansSerif -> "Sans"
        SubtitleFontFamily.Serif -> "Serif"
        SubtitleFontFamily.Monospace -> "Mono"
        SubtitleFontFamily.Rounded -> "Rounded"
        SubtitleFontFamily.Custom -> "Custom"
    }

@Composable
private fun SubtitleFontActionChip(
    label: String,
    onClick: () -> Unit,
    isCompact: Boolean,
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colorScheme.surface.copy(alpha = 0.82f))
            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = if (isCompact) 10.dp else 12.dp, vertical = if (isCompact) 7.dp else 8.dp),
    ) {
        Text(
            text = label,
            color = colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (isCompact) 12.sp else 13.sp,
            maxLines = 1,
        )
    }
}

@Composable
fun SubtitleSyncPanel(
    subtitleDelayMs: Int,
    selectedAddonSubtitle: AddonSubtitle?,
    subtitleAutoSyncState: SubtitleAutoSyncUiState,
    isCompact: Boolean,
    isPlaying: Boolean,
    currentPlaybackPositionMs: Long,
    onSubtitleDelayChanged: (Int) -> Unit,
    onSubtitleDelayReset: () -> Unit,
    onAutoSyncCapture: () -> Unit,
    onAutoSyncCueSelected: (SubtitleSyncCue) -> Unit,
    onAutoSyncReload: () -> Unit,
    onTogglePlayback: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val sectionPadding = if (isCompact) 12.dp else 16.dp

    AutoSyncControls(
        subtitleDelayMs = subtitleDelayMs,
        selectedAddonSubtitle = selectedAddonSubtitle,
        state = subtitleAutoSyncState,
        isCompact = isCompact,
        sectionPadding = sectionPadding,
        colorScheme = colorScheme,
        isPlaying = isPlaying,
        currentPlaybackPositionMs = currentPlaybackPositionMs,
        onSubtitleDelayChanged = onSubtitleDelayChanged,
        onSubtitleDelayReset = onSubtitleDelayReset,
        onCapture = onAutoSyncCapture,
        onCueSelected = onAutoSyncCueSelected,
        onReload = onAutoSyncReload,
        onTogglePlayback = onTogglePlayback,
    )
}

@Composable
private fun AutoSyncControls(
    subtitleDelayMs: Int,
    selectedAddonSubtitle: AddonSubtitle?,
    state: SubtitleAutoSyncUiState,
    isCompact: Boolean,
    sectionPadding: androidx.compose.ui.unit.Dp,
    colorScheme: androidx.compose.material3.ColorScheme,
    isPlaying: Boolean,
    currentPlaybackPositionMs: Long,
    onSubtitleDelayChanged: (Int) -> Unit,
    onSubtitleDelayReset: () -> Unit,
    onCapture: () -> Unit,
    onCueSelected: (SubtitleSyncCue) -> Unit,
    onReload: () -> Unit,
    onTogglePlayback: () -> Unit,
) {
    val capturedPositionMs = state.capturedPositionMs
    val sortedCues = state.cues.sortedBy { it.startTimeMs }
    val liveSubtitlePositionMs = (currentPlaybackPositionMs - subtitleDelayMs).coerceAtLeast(0L)
    val visibleCues = subtitleSyncCueWindow(sortedCues, liveSubtitlePositionMs)
    val currentCue = activeSubtitleSyncCue(sortedCues, liveSubtitlePositionMs)
    val suggestedCue = capturedPositionMs?.let { position ->
        sortedCues.minByOrNull { cue -> abs(cue.startTimeMs - position) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(sectionPadding),
        verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 14.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.compose_player_auto_sync),
                color = colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
            ) {
                SmallActionPill(
                    text = if (isPlaying) stringResource(Res.string.compose_action_pause)
                    else stringResource(Res.string.action_play),
                    enabled = selectedAddonSubtitle != null,
                    selected = isPlaying,
                    onClick = onTogglePlayback,
                )
                SmallActionPill(
                    text = stringResource(Res.string.compose_player_reload),
                    enabled = selectedAddonSubtitle != null,
                    onClick = onReload,
                )
                SmallActionPill(
                    text = stringResource(Res.string.compose_player_capture_line),
                    enabled = selectedAddonSubtitle != null,
                    onClick = onCapture,
                )
            }
        }

        if (selectedAddonSubtitle == null) {
            Text(
                text = stringResource(Res.string.compose_player_select_addon_subtitle_first),
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            return@Column
        }

        if (state.isLoading) {
            Text(
                text = stringResource(Res.string.compose_player_loading_lines),
                color = colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }

        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = colorScheme.error,
                fontSize = 12.sp,
            )
        }

        if (isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SubtitleSyncCuePreview(
                    cues = visibleCues,
                    anchorPositionMs = liveSubtitlePositionMs,
                    currentCue = currentCue,
                    suggestedCue = suggestedCue,
                    isCompact = true,
                    onCueSelected = onCueSelected,
                )
                SubtitleSyncDelayPanel(
                    subtitleDelayMs = subtitleDelayMs,
                    capturedPositionMs = capturedPositionMs,
                    isCompact = true,
                    onSubtitleDelayChanged = onSubtitleDelayChanged,
                    onSubtitleDelayReset = onSubtitleDelayReset,
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SubtitleSyncCuePreview(
                    cues = visibleCues,
                    anchorPositionMs = liveSubtitlePositionMs,
                    currentCue = currentCue,
                    suggestedCue = suggestedCue,
                    isCompact = false,
                    modifier = Modifier.weight(1f),
                    onCueSelected = onCueSelected,
                )
                SubtitleSyncDelayPanel(
                    subtitleDelayMs = subtitleDelayMs,
                    capturedPositionMs = capturedPositionMs,
                    isCompact = false,
                    modifier = Modifier.width(190.dp),
                    onSubtitleDelayChanged = onSubtitleDelayChanged,
                    onSubtitleDelayReset = onSubtitleDelayReset,
                )
            }
        }
    }
}

internal fun activeSubtitleSyncCue(
    sortedCues: List<SubtitleSyncCue>,
    positionMs: Long,
): SubtitleSyncCue? {
    for (index in sortedCues.indices.reversed()) {
        val cue = sortedCues[index]
        if (cue.startTimeMs > positionMs) continue

        val nextStartTimeMs = sortedCues.getOrNull(index + 1)?.startTimeMs
        val endTimeMs = cue.endTimeMs
            ?.takeIf { it > cue.startTimeMs }
            ?: nextStartTimeMs?.takeIf { it > cue.startTimeMs }
            ?: (cue.startTimeMs + 10_000L)
        if (positionMs < endTimeMs) return cue
    }
    return null
}

@Composable
private fun SubtitleSyncCuePreview(
    cues: List<SubtitleSyncCue>,
    anchorPositionMs: Long?,
    currentCue: SubtitleSyncCue?,
    suggestedCue: SubtitleSyncCue?,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    onCueSelected: (SubtitleSyncCue) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val minHeight = if (isCompact) 220.dp else 300.dp
    val maxHeight = if (isCompact) 420.dp else 520.dp
    val listState = rememberLazyListState()
    val currentIndex = cues.indexOf(currentCue)

    LaunchedEffect(currentIndex, cues.size) {
        if (currentIndex >= 0) {
            listState.scrollToItem((currentIndex - 1).coerceAtLeast(0))
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight, max = maxHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.86f))
            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
            .padding(if (isCompact) 8.dp else 10.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (anchorPositionMs == null) {
            item {
                Text(
                    text = stringResource(Res.string.compose_player_auto_sync_capture_hint),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                )
            }
            return@LazyColumn
        }

        if (cues.isEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.compose_player_no_subtitle_lines_found),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                )
            }
            return@LazyColumn
        }

        items(
            items = cues,
            key = { cue -> "${cue.startTimeMs}:${cue.text.hashCode()}" },
        ) { cue ->
            val current = cue == currentCue
            val suggested = cue == suggestedCue
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            current -> colorScheme.primary.copy(alpha = 0.30f)
                            suggested -> colorScheme.primary.copy(alpha = 0.14f)
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        1.dp,
                        when {
                            current -> colorScheme.primary.copy(alpha = 0.92f)
                            suggested -> colorScheme.primary.copy(alpha = 0.58f)
                            else -> Color.White.copy(alpha = 0.08f)
                        },
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onCueSelected(cue) }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = formatCueTimestamp(cue.startTimeMs),
                    color = if (current || suggested) colorScheme.primary else Color.White.copy(alpha = 0.54f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = cue.text,
                    color = Color.White,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    textDecoration = if (current) TextDecoration.Underline else null,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun SubtitleSyncDelayPanel(
    subtitleDelayMs: Int,
    capturedPositionMs: Long?,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    onSubtitleDelayChanged: (Int) -> Unit,
    onSubtitleDelayReset: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val btnSize = if (isCompact) 28.dp else 32.dp
    val btnRadius = if (isCompact) 14.dp else 16.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.surface.copy(alpha = 0.68f))
            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.72f), RoundedCornerShape(12.dp))
            .padding(if (isCompact) 10.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.compose_player_subtitle_delay),
            color = colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        StepperControl(
            value = formatSubtitleDelay(subtitleDelayMs),
            onMinus = {
                onSubtitleDelayChanged((subtitleDelayMs - SUBTITLE_DELAY_STEP_MS).coerceAtLeast(SUBTITLE_DELAY_MIN_MS))
            },
            onPlus = {
                onSubtitleDelayChanged((subtitleDelayMs + SUBTITLE_DELAY_STEP_MS).coerceAtMost(SUBTITLE_DELAY_MAX_MS))
            },
            buttonSize = btnSize,
            buttonRadius = btnRadius,
            minWidth = 82.dp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            SmallActionPill(
                text = stringResource(Res.string.compose_player_reset),
                onClick = onSubtitleDelayReset,
            )
        }
    }
}

private fun subtitleSyncCueWindow(
    cues: List<SubtitleSyncCue>,
    anchorPositionMs: Long?,
): List<SubtitleSyncCue> {
    if (anchorPositionMs == null || cues.isEmpty()) return emptyList()
    val sortedCues = cues.sortedBy { it.startTimeMs }
    val anchorIndex = sortedCues
        .indexOfLast { it.startTimeMs <= anchorPositionMs }
        .let { if (it >= 0) it else 0 }
    val startIndex = (anchorIndex - 20).coerceAtLeast(0)
    return sortedCues.drop(startIndex).take(41)
}

@Composable
private fun ToggleRow(
    label: String,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        SmallActionPill(
            text = if (enabled) stringResource(Res.string.compose_action_on)
            else stringResource(Res.string.compose_action_off),
            selected = enabled,
            onClick = onToggle,
        )
    }
}

@Composable
private fun ColorPickerRow(
    label: String,
    colors: List<Color>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            color = colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            colors.forEach { color ->
                val isSelected = selectedColor == color
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (color.alpha == 0f) colorScheme.surface else color)
                        .border(
                            2.dp,
                            if (isSelected) colorScheme.primary else colorScheme.outlineVariant,
                            CircleShape,
                        )
                        .clickable { onColorSelected(color) },
                )
            }
        }
    }
}

@Composable
private fun SmallActionPill(
    text: String,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    selected -> colorScheme.primaryContainer
                    enabled -> colorScheme.surface.copy(alpha = 0.82f)
                    else -> colorScheme.surfaceVariant.copy(alpha = 0.48f)
                }
            )
            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            color = when {
                selected -> colorScheme.onPrimaryContainer
                enabled -> colorScheme.onSurface
                else -> colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
            },
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun StepperControl(
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    buttonSize: androidx.compose.ui.unit.Dp,
    buttonRadius: androidx.compose.ui.unit.Dp,
    minWidth: androidx.compose.ui.unit.Dp = 42.dp,
    minusIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Remove,
    plusIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.KeyboardArrowUp,
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(RoundedCornerShape(buttonRadius))
                .background(colorScheme.primaryContainer)
                .clickable(onClick = onMinus),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = minusIcon,
                contentDescription = null,
                tint = colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp),
            )
        }

        Box(
            modifier = Modifier
                .widthIn(min = minWidth)
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.surface.copy(alpha = 0.82f))
                .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = value,
                color = colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
        }

        Box(
            modifier = Modifier
                .size(buttonSize)
                .clip(RoundedCornerShape(buttonRadius))
                .background(colorScheme.primaryContainer)
                .clickable(onClick = onPlus),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = plusIcon,
                contentDescription = null,
                tint = colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            color = colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun formatSubtitleDelay(delayMs: Int): String {
    val sign = if (delayMs >= 0) "+" else "-"
    val absMs = abs(delayMs)
    val seconds = absMs / 1000
    val millis = absMs % 1000
    return "$sign$seconds.${millis.toString().padStart(3, '0')}s"
}

private fun formatCueTimestamp(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}
