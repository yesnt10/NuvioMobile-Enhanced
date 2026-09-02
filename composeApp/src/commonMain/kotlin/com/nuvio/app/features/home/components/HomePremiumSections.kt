package com.nuvio.app.features.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.app.core.format.formatReleaseDateWithoutYear
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.core.ui.NuvioTokens
import com.nuvio.app.core.ui.NuvioViewAllPillSize
import com.nuvio.app.core.ui.landscapePosterWidth
import com.nuvio.app.core.ui.nuvio
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.home.HomeConciergeCard
import com.nuvio.app.features.home.HomeConciergeChip
import com.nuvio.app.features.home.HomeConciergeChipType
import com.nuvio.app.features.home.HomeConciergeHeadlineType
import com.nuvio.app.features.home.HomeConciergeReason
import com.nuvio.app.features.home.HomeConciergeStat
import com.nuvio.app.features.home.HomeConciergeStatType
import com.nuvio.app.features.home.HomeConciergeUiState
import com.nuvio.app.features.home.HomeReleaseRadarCategory
import com.nuvio.app.features.home.HomeReleaseRadarItem
import com.nuvio.app.features.home.HomeSmartShelf
import com.nuvio.app.features.home.HomeSmartResumeSignal
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import kotlin.math.absoluteValue
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeConciergeSection(
    state: HomeConciergeUiState,
    modifier: Modifier = Modifier,
    sectionPadding: Dp? = null,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)? = null,
) {
    if (state.cards.isEmpty() && state.stats.isEmpty()) return

    if (sectionPadding != null) {
        HomeConciergeSectionContent(
            state = state,
            modifier = modifier.fillMaxWidth(),
            sectionPadding = sectionPadding,
            onPosterClick = onPosterClick,
            onContinueWatchingClick = onContinueWatchingClick,
        )
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            HomeConciergeSectionContent(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                sectionPadding = homeSectionHorizontalPaddingForWidth(maxWidth.value),
                onPosterClick = onPosterClick,
                onContinueWatchingClick = onContinueWatchingClick,
            )
        }
    }
}

@Composable
private fun HomeConciergeSectionContent(
    state: HomeConciergeUiState,
    modifier: Modifier,
    sectionPadding: Dp,
    onPosterClick: ((MetaPreview) -> Unit)?,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val panelShape = RoundedCornerShape(30.dp)
    val accent = tokens.colors.accent

    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = sectionPadding)
            .clip(panelShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF101827),
                        Color(0xFF172033),
                        tokens.colors.surface,
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.11f),
                shape = panelShape,
            ),
    ) {
        val isWidePanel = maxWidth >= 760.dp
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.28f),
                            Color.Transparent,
                        ),
                        radius = 760f,
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            HomeConciergeHeader(state = state)
            if (state.chips.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 6.dp),
                ) {
                    items(state.chips, key = { chip -> chip.type.name }) { chip ->
                        HomeConciergeChipPill(chip = chip)
                    }
                }
            }

            val primary = state.cards.firstOrNull()
            val secondary = state.cards.drop(1)
            if (primary != null) {
                if (isWidePanel) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        HomeConciergePrimaryCard(
                            card = primary,
                            modifier = Modifier.weight(1.25f),
                            onPosterClick = onPosterClick,
                            onContinueWatchingClick = onContinueWatchingClick,
                        )
                        HomeConciergeSideRail(
                            cards = secondary,
                            stats = state.stats,
                            modifier = Modifier.weight(0.85f),
                            onPosterClick = onPosterClick,
                            onContinueWatchingClick = onContinueWatchingClick,
                        )
                    }
                } else {
                    HomeConciergePrimaryCard(
                        card = primary,
                        modifier = Modifier.fillMaxWidth(),
                        onPosterClick = onPosterClick,
                        onContinueWatchingClick = onContinueWatchingClick,
                    )
                    if (secondary.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(end = 6.dp),
                        ) {
                            items(secondary, key = HomeConciergeCard::key) { card ->
                                HomeConciergeMiniCard(
                                    card = card,
                                    modifier = Modifier.width(236.dp),
                                    onPosterClick = onPosterClick,
                                    onContinueWatchingClick = onContinueWatchingClick,
                                )
                            }
                        }
                    }
                    if (state.stats.isNotEmpty()) {
                        HomeConciergeStatsRow(stats = state.stats)
                    }
                }
            } else if (state.stats.isNotEmpty()) {
                HomeConciergeStatsRow(stats = state.stats)
            }
        }
    }
}

@Composable
private fun HomeConciergeHeader(state: HomeConciergeUiState) {
    val tokens = MaterialTheme.nuvio
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.profileName?.let { profileName ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.home_concierge_profile_badge, profileName),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = state.headlineType.localizedHeadline(),
            style = MaterialTheme.typography.headlineSmall,
            color = tokens.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(Res.string.home_concierge_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeConciergeChipPill(chip: HomeConciergeChip) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = chip.type.localizedChip(),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.colors.textPrimary,
            maxLines = 1,
        )
    }
}

@Composable
private fun HomeConciergePrimaryCard(
    card: HomeConciergeCard,
    modifier: Modifier = Modifier,
    onPosterClick: ((MetaPreview) -> Unit)?,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)?,
) {
    val clickAction = card.clickAction(onPosterClick, onContinueWatchingClick)
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .heightIn(min = 190.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .then(if (clickAction != null) Modifier.clickable(onClick = clickAction) else Modifier),
    ) {
        HomeConciergeArtwork(
            imageUrl = card.imageUrl,
            title = card.title,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.12f),
                            Color.Black.copy(alpha = 0.76f),
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (card.smartSignal != null) {
                    val signal = card.smartSignal
                    HomeConciergeSmartSignalPill(signal = signal)
                } else {
                    HomeConciergeReasonPill(reason = card.reason)
                }
            }
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            card.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.76f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HomeConciergeActionPill(card = card)
        }
    }
}

@Composable
private fun HomeConciergeSideRail(
    cards: List<HomeConciergeCard>,
    stats: List<HomeConciergeStat>,
    modifier: Modifier = Modifier,
    onPosterClick: ((MetaPreview) -> Unit)?,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)?,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        cards.take(2).forEach { card ->
            HomeConciergeMiniCard(
                card = card,
                modifier = Modifier.fillMaxWidth(),
                onPosterClick = onPosterClick,
                onContinueWatchingClick = onContinueWatchingClick,
            )
        }
        if (stats.isNotEmpty()) {
            HomeConciergeStatsRow(stats = stats)
        }
    }
}

@Composable
private fun HomeConciergeMiniCard(
    card: HomeConciergeCard,
    modifier: Modifier = Modifier,
    onPosterClick: ((MetaPreview) -> Unit)?,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val clickAction = card.clickAction(onPosterClick, onContinueWatchingClick)
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .height(104.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .then(if (clickAction != null) Modifier.clickable(onClick = clickAction) else Modifier)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(82.dp)
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(14.dp))
                .background(tokens.colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            HomeConciergeArtwork(
                imageUrl = card.imageUrl,
                title = card.title,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = card.smartSignal?.localizedSmartSignal() ?: card.reason.localizedReason(),
                style = MaterialTheme.typography.labelSmall,
                color = tokens.colors.accent,
                maxLines = 1,
            )
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                color = tokens.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            card.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeConciergeStatsRow(stats: List<HomeConciergeStat>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 6.dp),
    ) {
        items(stats, key = { stat -> stat.type.name }) { stat ->
            HomeConciergeStatPill(stat = stat)
        }
    }
}

@Composable
private fun HomeConciergeStatPill(stat: HomeConciergeStat) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = Modifier
            .widthIn(min = 104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stat.value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = tokens.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stat.type.localizedStat(),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeConciergeReasonPill(reason: HomeConciergeReason) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.36f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = reason.localizedReason(),
            style = MaterialTheme.typography.labelMedium,
            color = tokens.colors.accent,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HomeConciergeSmartSignalPill(signal: HomeSmartResumeSignal) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tokens.colors.accent.copy(alpha = 0.22f))
            .border(1.dp, tokens.colors.accent.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = signal.localizedSmartSignal(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HomeConciergeActionPill(card: HomeConciergeCard) {
    val tokens = MaterialTheme.nuvio
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tokens.colors.accent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.Black,
        )
        Text(
            text = if (card.continueWatchingItem != null) {
                stringResource(Res.string.home_concierge_action_resume)
            } else {
                stringResource(Res.string.home_concierge_action_open)
            },
            style = MaterialTheme.typography.labelMedium,
            color = Color.Black,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HomeConciergeArtwork(
    imageUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier.background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.take(2).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun HomeSmartShelfComposerSection(
    shelves: List<HomeSmartShelf>,
    modifier: Modifier = Modifier,
    sectionPadding: Dp? = null,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
) {
    if (shelves.isEmpty()) return

    if (sectionPadding != null) {
        HomeSmartShelfComposerContent(
            shelves = shelves,
            modifier = modifier.fillMaxWidth(),
            sectionPadding = sectionPadding,
            onPosterClick = onPosterClick,
        )
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            HomeSmartShelfComposerContent(
                shelves = shelves,
                modifier = Modifier.fillMaxWidth(),
                sectionPadding = homeSectionHorizontalPaddingForWidth(maxWidth.value),
                onPosterClick = onPosterClick,
            )
        }
    }
}

@Composable
private fun HomeSmartShelfComposerContent(
    shelves: List<HomeSmartShelf>,
    modifier: Modifier,
    sectionPadding: Dp,
    onPosterClick: ((MetaPreview) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(30.dp)
    Column(
        modifier = modifier
            .padding(horizontal = sectionPadding)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF101726),
                        tokens.colors.accent.copy(alpha = 0.20f),
                        tokens.colors.surface.copy(alpha = 0.88f),
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.11f), shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(tokens.colors.accent.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = tokens.colors.accent,
                    )
                    Text(
                        text = "Smart Shelf Composer",
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = "Shelves built from your signals",
                style = MaterialTheme.typography.titleLarge,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Progress, library, catalog quality and taste clusters are composed into opt-in discovery rails.",
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(shelves, key = HomeSmartShelf::key) { shelf ->
                HomeSmartShelfCard(
                    shelf = shelf,
                    modifier = Modifier.width(258.dp),
                    onPosterClick = onPosterClick,
                )
            }
        }
    }
}

@Composable
private fun HomeSmartShelfCard(
    shelf: HomeSmartShelf,
    modifier: Modifier = Modifier,
    onPosterClick: ((MetaPreview) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(24.dp)
    val primary = shelf.items.firstOrNull()
    Column(
        modifier = modifier
            .heightIn(min = 238.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.20f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .then(
                if (primary != null && onPosterClick != null) {
                    Modifier.clickable { onPosterClick(primary) }
                } else {
                    Modifier
                },
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .background(tokens.colors.surface),
        ) {
            HomeConciergeArtwork(
                imageUrl = primary?.banner ?: primary?.poster,
                title = primary?.name ?: shelf.title,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.70f),
                            ),
                        ),
                    ),
            )
            Text(
                text = shelf.signal,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(tokens.colors.accent)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = shelf.title,
                style = MaterialTheme.typography.titleMedium,
                color = tokens.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = shelf.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            shelf.items.take(3).forEach { item ->
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.84f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun HomeReleaseRadarSection(
    items: List<HomeReleaseRadarItem>,
    modifier: Modifier = Modifier,
    sectionPadding: Dp? = null,
    windowDays: Int = 30,
    isRefreshing: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    if (sectionPadding != null) {
        HomeReleaseRadarSectionContent(
            items = items,
            modifier = modifier.fillMaxWidth(),
            sectionPadding = sectionPadding,
            windowDays = windowDays,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            onPosterClick = onPosterClick,
            onContinueWatchingClick = onContinueWatchingClick,
        )
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            HomeReleaseRadarSectionContent(
                items = items,
                modifier = Modifier.fillMaxWidth(),
                sectionPadding = homeSectionHorizontalPaddingForWidth(maxWidth.value),
                windowDays = windowDays,
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                onPosterClick = onPosterClick,
                onContinueWatchingClick = onContinueWatchingClick,
            )
        }
    }
}

@Composable
private fun HomeReleaseRadarSectionContent(
    items: List<HomeReleaseRadarItem>,
    modifier: Modifier,
    sectionPadding: Dp,
    windowDays: Int,
    isRefreshing: Boolean,
    onRefresh: (() -> Unit)?,
    onPosterClick: ((MetaPreview) -> Unit)?,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.controlGap + NuvioTokens.Space.s2),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = sectionPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.home_release_radar_title),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    color = tokens.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(tokens.colors.accent.copy(alpha = 0.16f))
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = tokens.colors.accent,
                        )
                        Text(
                            text = stringResource(Res.string.home_release_radar_window_days, windowDays),
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.colors.textPrimary,
                            maxLines = 1,
                        )
                    }
                }
                if (onRefresh != null) {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(Res.string.home_release_radar_refresh),
                            modifier = Modifier.size(18.dp),
                            tint = if (isRefreshing) tokens.colors.textMuted else tokens.colors.accent,
                        )
                    }
                }
            }
            Text(
                text = stringResource(Res.string.home_release_radar_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = tokens.colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .width(NuvioTokens.Space.s64 - NuvioTokens.Space.s4)
                    .height(NuvioTokens.Space.s4)
                    .background(tokens.colors.accent, RoundedCornerShape(999.dp)),
            )
        }

        val posterCardStyle = rememberPosterCardStyleUiState()
        NuvioShelfSection(
            title = "",
            entries = items,
            modifier = Modifier.fillMaxWidth(),
            headerHorizontalPadding = sectionPadding,
            rowContentPadding = PaddingValues(horizontal = sectionPadding),
            itemSpacing = 10.dp,
            showHeaderAccent = false,
            viewAllPillSize = NuvioViewAllPillSize.Compact,
            key = HomeReleaseRadarItem::key,
        ) { item ->
            HomeReleaseRadarCard(
                item = item,
                width = landscapePosterWidth(posterCardStyle.widthDp).coerceAtLeast(212.dp),
                onPosterClick = onPosterClick,
                onContinueWatchingClick = onContinueWatchingClick,
            )
        }
    }
}

@Composable
private fun HomeReleaseRadarCard(
    item: HomeReleaseRadarItem,
    width: Dp,
    onPosterClick: ((MetaPreview) -> Unit)?,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)?,
) {
    val tokens = MaterialTheme.nuvio
    val shape = RoundedCornerShape(20.dp)
    val clickAction = item.clickAction(onPosterClick, onContinueWatchingClick)
    Column(
        modifier = Modifier.width(width),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(shape)
                .background(tokens.colors.surface)
                .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
                .then(if (clickAction != null) Modifier.clickable(onClick = clickAction) else Modifier),
        ) {
            HomeConciergeArtwork(
                imageUrl = item.imageUrl,
                title = item.title,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.04f),
                                Color.Black.copy(alpha = 0.64f),
                            )
                        )
                    )
            )
            HomeReleaseTimingBadge(
                item = item,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(9.dp),
            )
            Text(
                text = item.category.localizedCategory(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.34f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.86f),
                maxLines = 1,
            )
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HomeReleaseSignalBadge(item = item)
        item.subtitle?.let { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = tokens.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HomeReleaseTimingBadge(
    item: HomeReleaseRadarItem,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tokens.colors.accent)
            .padding(horizontal = 9.dp, vertical = 6.dp),
    ) {
        Text(
            text = item.localizedTiming(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun HomeReleaseSignalBadge(
    item: HomeReleaseRadarItem,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.nuvio
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tokens.colors.surface.copy(alpha = 0.74f))
            .border(1.dp, tokens.colors.borderSubtle, RoundedCornerShape(999.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(
            text = item.localizedRadarSignal(),
            style = MaterialTheme.typography.labelSmall,
            color = tokens.colors.textMuted,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun HomeConciergeCard.clickAction(
    onPosterClick: ((MetaPreview) -> Unit)?,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)?,
): (() -> Unit)? =
    when {
        continueWatchingItem != null && onContinueWatchingClick != null -> {
            { onContinueWatchingClick(continueWatchingItem) }
        }
        preview != null && onPosterClick != null -> {
            { onPosterClick(preview) }
        }
        else -> null
    }

private fun HomeReleaseRadarItem.clickAction(
    onPosterClick: ((MetaPreview) -> Unit)?,
    onContinueWatchingClick: ((ContinueWatchingItem) -> Unit)?,
): (() -> Unit)? =
    when {
        continueWatchingItem != null &&
            onContinueWatchingClick != null &&
            (daysFromToday ?: 1) <= 0 -> {
            { onContinueWatchingClick(continueWatchingItem) }
        }
        onPosterClick != null -> {
            { onPosterClick(preview) }
        }
        else -> null
    }

@Composable
private fun HomeConciergeHeadlineType.localizedHeadline(): String =
    when (this) {
        HomeConciergeHeadlineType.Resume -> stringResource(Res.string.home_concierge_headline_resume)
        HomeConciergeHeadlineType.Release -> stringResource(Res.string.home_concierge_headline_release)
        HomeConciergeHeadlineType.NextUp -> stringResource(Res.string.home_concierge_headline_next_up)
        HomeConciergeHeadlineType.Library -> stringResource(Res.string.home_concierge_headline_library)
        HomeConciergeHeadlineType.Discovery -> stringResource(Res.string.home_concierge_headline_discovery)
    }

@Composable
private fun HomeConciergeReason.localizedReason(): String =
    when (this) {
        HomeConciergeReason.Resume -> stringResource(Res.string.home_concierge_reason_resume)
        HomeConciergeReason.NextUp -> stringResource(Res.string.home_concierge_reason_next_up)
        HomeConciergeReason.ReleaseRadar -> stringResource(Res.string.home_concierge_reason_release)
        HomeConciergeReason.LibraryPick -> stringResource(Res.string.home_concierge_reason_library)
        HomeConciergeReason.CatalogSignal -> stringResource(Res.string.home_concierge_reason_catalog)
    }

@Composable
private fun HomeSmartResumeSignal.localizedSmartSignal(): String =
    when (this) {
        HomeSmartResumeSignal.ContinueNow -> stringResource(Res.string.home_smart_resume_signal_continue_now)
        HomeSmartResumeSignal.NextEpisodeReady -> stringResource(Res.string.home_smart_resume_signal_next_episode)
        HomeSmartResumeSignal.AlmostFinished -> stringResource(Res.string.home_smart_resume_signal_almost_finished)
        HomeSmartResumeSignal.QuickResume -> stringResource(Res.string.home_smart_resume_signal_quick_resume)
        HomeSmartResumeSignal.NewEpisode -> stringResource(Res.string.home_smart_resume_signal_new_episode)
        HomeSmartResumeSignal.NewSeason -> stringResource(Res.string.home_smart_resume_signal_new_season)
        HomeSmartResumeSignal.Scheduled -> stringResource(Res.string.home_smart_resume_signal_scheduled)
        HomeSmartResumeSignal.ResumeReady -> stringResource(Res.string.home_smart_resume_signal_resume_ready)
    }

@Composable
private fun HomeConciergeStatType.localizedStat(): String =
    when (this) {
        HomeConciergeStatType.ContinueWatching -> stringResource(Res.string.home_concierge_stat_continue)
        HomeConciergeStatType.ReleaseRadar -> stringResource(Res.string.home_concierge_stat_radar)
        HomeConciergeStatType.Library -> stringResource(Res.string.home_concierge_stat_library)
        HomeConciergeStatType.HighSignal -> stringResource(Res.string.home_concierge_stat_high_signal)
    }

@Composable
private fun HomeConciergeChipType.localizedChip(): String =
    when (this) {
        HomeConciergeChipType.ProfileAware -> stringResource(Res.string.home_concierge_chip_profile)
        HomeConciergeChipType.WatchProgress -> stringResource(Res.string.home_concierge_chip_progress)
        HomeConciergeChipType.LibraryAware -> stringResource(Res.string.home_concierge_chip_library)
        HomeConciergeChipType.ReleaseAware -> stringResource(Res.string.home_concierge_chip_release)
    }

@Composable
private fun HomeReleaseRadarCategory.localizedCategory(): String =
    when (this) {
        HomeReleaseRadarCategory.Episode -> stringResource(Res.string.home_release_radar_category_episode)
        HomeReleaseRadarCategory.Movie -> stringResource(Res.string.home_release_radar_category_movie)
        HomeReleaseRadarCategory.Series -> stringResource(Res.string.home_release_radar_category_series)
        HomeReleaseRadarCategory.NextUp -> stringResource(Res.string.home_release_radar_category_next_up)
    }

@Composable
private fun HomeReleaseRadarItem.localizedTiming(): String {
    val days = daysFromToday
    return when {
        days == null -> releaseIsoDate?.let { formatReleaseDateWithoutYear(it) }.orEmpty()
        days == 0 -> stringResource(Res.string.home_release_radar_today)
        days == 1 -> stringResource(Res.string.home_release_radar_tomorrow)
        days > 1 -> stringResource(Res.string.home_release_radar_in_days, days)
        days == -1 -> stringResource(Res.string.home_release_radar_yesterday)
        else -> stringResource(Res.string.home_release_radar_days_ago, days.absoluteValue)
    }
}

@Composable
private fun HomeReleaseRadarItem.localizedRadarSignal(): String =
    when {
        category == HomeReleaseRadarCategory.NextUp -> stringResource(Res.string.home_release_radar_signal_next_up)
        category == HomeReleaseRadarCategory.Episode -> stringResource(Res.string.home_release_radar_signal_library)
        daysFromToday == 0 -> stringResource(Res.string.home_release_radar_signal_today)
        (daysFromToday ?: Int.MAX_VALUE) in 1..7 -> stringResource(Res.string.home_release_radar_signal_week)
        else -> stringResource(Res.string.home_release_radar_signal_upcoming)
    }
