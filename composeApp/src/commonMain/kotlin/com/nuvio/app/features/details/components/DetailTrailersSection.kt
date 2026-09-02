package com.nuvio.app.features.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.nuvioCardDepth
import com.nuvio.app.core.ui.nuvioHorizontalScrollBleed
import com.nuvio.app.features.details.MetaTrailer
import nuvio.composeapp.generated.resources.*
import nuvio.composeapp.generated.resources.detail_tab_trailer
import nuvio.composeapp.generated.resources.detail_trailer_category_count
import nuvio.composeapp.generated.resources.detail_trailers_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun DetailTrailersSection(
    trailers: List<MetaTrailer>,
    onTrailerClick: (MetaTrailer) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    horizontalScrollPadding: Dp = 0.dp,
) {
    if (trailers.isEmpty()) return

    val trailerLabel = stringResource(Res.string.detail_tab_trailer)
    val grouped = remember(trailers) {
        val rawGroups = linkedMapOf<String, MutableList<MetaTrailer>>().apply {
            trailers.forEach { trailer ->
                val category = trailer.type.ifBlank { trailerLabel }
                getOrPut(category) { mutableListOf() }.add(trailer)
            }
        }
        rawGroups.entries
            .sortedWith(
                compareBy<Map.Entry<String, MutableList<MetaTrailer>>> { entry ->
                    trailerCategoryPriority(entry.key, trailerLabel)
                }.thenBy { entry -> entry.key.lowercase() },
            )
            .associateTo(linkedMapOf()) { it.key to it.value }
    }

    if (grouped.isEmpty()) return

    val initialCategory = remember(grouped) {
        grouped.keys.firstOrNull { it.equals(trailerLabel, ignoreCase = true) }
            ?: grouped.keys.first()
    }
    var selectedCategory by remember(grouped) { mutableStateOf(initialCategory) }

    val selectedTrailers = grouped[selectedCategory].orEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sizing = trailerSectionSizing(maxWidth.value)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (showHeader) {
                    DetailSectionTitle(
                        title = stringResource(Res.string.detail_trailers_title),
                        fullWidth = false,
                    )
                }

                if (grouped.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(grouped.keys.toList(), key = { it }) { category ->
                            TrailerCategoryChip(
                                label = category,
                                count = grouped[category]?.size ?: 0,
                                selected = category == selectedCategory,
                                sizing = sizing,
                                onClick = { selectedCategory = category },
                            )
                        }
                    }
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val sizing = trailerSectionSizing(maxWidth.value)
            LazyRow(
                modifier = Modifier
                    .nuvioHorizontalScrollBleed(horizontalScrollPadding)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = horizontalScrollPadding),
                horizontalArrangement = Arrangement.spacedBy(sizing.cardSpacing),
            ) {
                itemsIndexed(
                    items = selectedTrailers,
                    key = { index, trailer -> "${trailer.type}-${trailer.id}-${trailer.seasonNumber ?: 0}#$index" },
                ) { _, trailer ->
                    TrailerCard(
                        trailer = trailer,
                        cardWidth = sizing.cardWidth,
                        cornerRadius = sizing.cardRadius,
                        titleFontSize = sizing.titleFontSize,
                        metaFontSize = sizing.metaFontSize,
                        onClick = { onTrailerClick(trailer) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrailerCategoryChip(
    label: String,
    count: Int,
    selected: Boolean,
    sizing: TrailerSectionSizing,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(sizing.selectorRadius),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        tonalElevation = 0.dp,
        modifier = Modifier
            .clip(RoundedCornerShape(sizing.selectorRadius))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = stringResource(Res.string.detail_trailer_category_count, label, count),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.padding(
                horizontal = sizing.selectorHorizontalPadding,
                vertical = sizing.selectorVerticalPadding,
            ),
        )
    }
}

@Composable
private fun TrailerCard(
    trailer: MetaTrailer,
    cardWidth: androidx.compose.ui.unit.Dp,
    cornerRadius: androidx.compose.ui.unit.Dp,
    titleFontSize: androidx.compose.ui.unit.TextUnit,
    metaFontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(cardWidth),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .nuvioCardDepth(
                    shape = RoundedCornerShape(cornerRadius),
                    surface = NuvioCardDepthSurface.Trailers,
                )
                .clickable(onClick = onClick),
        ) {
            AsyncImage(
                model = "https://img.youtube.com/vi/${trailer.key}/hqdefault.jpg",
                contentDescription = trailer.displayName ?: trailer.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clip(RoundedCornerShape(cornerRadius)),
                contentScale = ContentScale.Crop,
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.2f))
            )
        }

        Text(
            text = trailer.displayName ?: trailer.name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = titleFontSize,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )

        val year = trailer.publishedAt?.take(4).orEmpty()
        if (year.isNotBlank()) {
            Text(
                text = year,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = metaFontSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class TrailerSectionSizing(
    val cardWidth: androidx.compose.ui.unit.Dp,
    val cardSpacing: androidx.compose.ui.unit.Dp,
    val cardRadius: androidx.compose.ui.unit.Dp,
    val selectorRadius: androidx.compose.ui.unit.Dp,
    val selectorHorizontalPadding: androidx.compose.ui.unit.Dp,
    val selectorVerticalPadding: androidx.compose.ui.unit.Dp,
    val titleFontSize: androidx.compose.ui.unit.TextUnit,
    val metaFontSize: androidx.compose.ui.unit.TextUnit,
)

private fun trailerCategoryPriority(category: String, trailerLabel: String): Int =
    when {
        category.equals(trailerLabel, ignoreCase = true) -> 0
        category.equals("Trailer", ignoreCase = true) -> 0
        category.equals("Teaser", ignoreCase = true) -> 1
        category.equals("Behind The Scenes", ignoreCase = true) -> 2
        category.equals("Behind the Scenes", ignoreCase = true) -> 2
        else -> 3
    }

private fun trailerSectionSizing(maxWidthDp: Float): TrailerSectionSizing =
    when {
        maxWidthDp >= 1200f -> TrailerSectionSizing(
            cardWidth = 280.dp,
            cardSpacing = 16.dp,
            cardRadius = 20.dp,
            selectorRadius = 20.dp,
            selectorHorizontalPadding = 14.dp,
            selectorVerticalPadding = 8.dp,
            titleFontSize = 16.sp,
            metaFontSize = 14.sp,
        )

        maxWidthDp >= 1024f -> TrailerSectionSizing(
            cardWidth = 260.dp,
            cardSpacing = 14.dp,
            cardRadius = 18.dp,
            selectorRadius = 18.dp,
            selectorHorizontalPadding = 12.dp,
            selectorVerticalPadding = 6.dp,
            titleFontSize = 15.sp,
            metaFontSize = 13.sp,
        )

        maxWidthDp >= 768f -> TrailerSectionSizing(
            cardWidth = 240.dp,
            cardSpacing = 12.dp,
            cardRadius = 16.dp,
            selectorRadius = 16.dp,
            selectorHorizontalPadding = 10.dp,
            selectorVerticalPadding = 5.dp,
            titleFontSize = 14.sp,
            metaFontSize = 12.sp,
        )

        else -> TrailerSectionSizing(
            cardWidth = 200.dp,
            cardSpacing = 12.dp,
            cardRadius = 16.dp,
            selectorRadius = 16.dp,
            selectorHorizontalPadding = 10.dp,
            selectorVerticalPadding = 5.dp,
            titleFontSize = 12.sp,
            metaFontSize = 10.sp,
        )
    }
