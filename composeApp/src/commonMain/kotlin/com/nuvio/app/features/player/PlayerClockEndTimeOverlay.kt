package com.nuvio.app.features.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.currentAnimatedThemeVisuals
import com.nuvio.app.core.ui.nuvio
import kotlinx.coroutines.delay
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.player_clock_ends_at
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToLong

@Composable
internal fun PlayerClockEndTimeOverlay(
    playbackSnapshot: PlayerPlaybackSnapshot,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    val animatedTheme = currentAnimatedThemeVisuals
    val remainingWallClockMs = remember(
        playbackSnapshot.durationMs,
        playbackSnapshot.positionMs,
        playbackSnapshot.playbackSpeed,
    ) {
        playbackSnapshot.remainingWallClockMs()
    }
    if (remainingWallClockMs == null) return

    var clockSnapshot by remember(remainingWallClockMs) {
        mutableStateOf(PlayerWallClock.snapshotForRemaining(remainingWallClockMs))
    }

    LaunchedEffect(remainingWallClockMs) {
        while (true) {
            clockSnapshot = PlayerWallClock.snapshotForRemaining(remainingWallClockMs)
            delay(1_000L)
        }
    }

    Column(
        modifier = modifier
            .shadow(18.dp, RoundedCornerShape(12.dp), clip = false)
            .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .border(
                1.dp,
                animatedTheme?.lineBrush ?: SolidColor(tokens.colors.accent.copy(alpha = 0.46f)),
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = clockSnapshot.currentTime,
            color = Color.White,
            fontSize = 19.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(Res.string.player_clock_ends_at, clockSnapshot.endTime),
            color = animatedTheme?.accent ?: tokens.colors.accent,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun PlayerPlaybackSnapshot.remainingWallClockMs(): Long? {
    val duration = durationMs.takeIf { it > 0L } ?: return null
    val remainingPlaybackMs = (duration - positionMs).coerceAtLeast(0L)
    val speed = playbackSpeed.takeIf { it > 0f } ?: 1f
    return (remainingPlaybackMs / speed).roundToLong()
}
