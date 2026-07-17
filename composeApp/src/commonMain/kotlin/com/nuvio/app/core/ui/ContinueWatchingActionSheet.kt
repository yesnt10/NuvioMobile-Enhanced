package com.nuvio.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.features.cloud.CloudLibraryContentType
import com.nuvio.app.features.cloud.cloudLibraryDisplayArtworkUrl
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.cw_action_go_to_details
import nuvio.composeapp.generated.resources.cw_action_remove
import nuvio.composeapp.generated.resources.cw_action_start_from_beginning
import nuvio.composeapp.generated.resources.play_manually
import org.jetbrains.compose.resources.stringResource

@Composable
fun NuvioContinueWatchingActionSheet(
    item: ContinueWatchingItem?,
    showManualPlayOption: Boolean,
    showDetailsOption: Boolean = true,
    onDismiss: () -> Unit,
    onOpenDetails: () -> Unit,
    onStartFromBeginning: (() -> Unit)? = null,
    onPlayManually: (() -> Unit)? = null,
    onRemove: () -> Unit,
    onTracking: (() -> Unit)? = null,
) {
    if (item == null) return
    val artwork = continueWatchingSheetArtwork(item)

    fun dismissAfter(action: () -> Unit) {
        action()
        onDismiss()
    }

    NuvioMediaActionOverlay(
        artworkUrl = artwork?.let { cloudLibraryDisplayArtworkUrl(it) },
        contentDescription = item.title,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ContinueWatchingSheetHeader(item = item, artwork = artwork)
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .widthIn(max = 390.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = Color(0xFF171717).copy(alpha = 0.96f),
                shadowElevation = 18.dp,
                tonalElevation = 6.dp,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (showDetailsOption) {
                        ContinueWatchingSheetActionRow(
                            icon = Icons.Default.Info,
                            title = stringResource(Res.string.cw_action_go_to_details),
                            onClick = { dismissAfter(onOpenDetails) },
                        )
                    }
                    if (onTracking != null) {
                        if (showDetailsOption) {
                            ContinueWatchingSheetDivider()
                        }
                        ContinueWatchingSheetActionRow(
                            icon = Icons.Default.Edit,
                            title = "Tracking",
                            onClick = { dismissAfter(onTracking) },
                        )
                    }
                    if (showManualPlayOption && onPlayManually != null) {
                        if (showDetailsOption || onTracking != null) {
                            ContinueWatchingSheetDivider()
                        }
                        ContinueWatchingSheetActionRow(
                            icon = Icons.Default.PlayArrow,
                            title = stringResource(Res.string.play_manually),
                            onClick = { dismissAfter(onPlayManually) },
                        )
                    }
                    if (!item.isNextUp && onStartFromBeginning != null) {
                        if (showDetailsOption || onTracking != null || (showManualPlayOption && onPlayManually != null)) {
                            ContinueWatchingSheetDivider()
                        }
                        ContinueWatchingSheetActionRow(
                            icon = Icons.Default.Replay,
                            title = stringResource(Res.string.cw_action_start_from_beginning),
                            onClick = { dismissAfter(onStartFromBeginning) },
                        )
                    }
                    if (showDetailsOption || onTracking != null || (showManualPlayOption && onPlayManually != null) || (!item.isNextUp && onStartFromBeginning != null)) {
                        ContinueWatchingSheetDivider()
                    }
                    ContinueWatchingSheetActionRow(
                        icon = Icons.Default.DeleteOutline,
                        title = stringResource(Res.string.cw_action_remove),
                        onClick = { dismissAfter(onRemove) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingSheetHeader(
    item: ContinueWatchingItem,
    artwork: String?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 430.dp)
                .aspectRatio(16f / 9f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center,
        ) {
            if (artwork != null) {
                AsyncImage(
                    model = cloudLibraryDisplayArtworkUrl(artwork),
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .matchParentSize(),
                    contentScale = if (item.isCloudLibraryItem()) ContentScale.Fit else ContentScale.Crop,
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.12f),
                                    Color.Black.copy(alpha = 0.50f),
                                ),
                            ),
                        ),
                )
            } else {
                Text(
                    text = item.title,
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = localizedContinueWatchingSubtitle(item),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.64f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun ContinueWatchingItem.isCloudLibraryItem(): Boolean =
    parentMetaType.equals(CloudLibraryContentType, ignoreCase = true)

private fun continueWatchingSheetArtwork(item: ContinueWatchingItem): String? =
    item.imageUrl?.takeIf { it.isNotBlank() }
        ?: item.poster?.takeIf { it.isNotBlank() }

@Composable
private fun ContinueWatchingSheetActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = Color.White,
        )
    }
}

@Composable
private fun ContinueWatchingSheetDivider() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.06f),
        thickness = 1.dp,
    )
}
