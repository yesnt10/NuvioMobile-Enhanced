package com.nuvio.app.features.catalog

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.network.NetworkCondition
import com.nuvio.app.core.network.NetworkStatusRepository
import com.nuvio.app.core.ui.NuvioNetworkOfflineCard
import coil3.compose.AsyncImage
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.ui.NuvioBackButton
import com.nuvio.app.core.ui.NuvioCardDepthSurface
import com.nuvio.app.core.ui.NuvioPosterWatchedOverlay
import com.nuvio.app.core.ui.nuvioCardDepth
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.core.ui.posterCardClickable
import com.nuvio.app.core.ui.nuvioSafeBottomPadding
import com.nuvio.app.core.ui.withDuplicateSafeLazyKeys
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.home.stableKey
import com.nuvio.app.features.watched.WatchedRepository
import com.nuvio.app.features.watching.application.WatchingState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun CatalogScreen(
    title: String,
    subtitle: String,
    target: CatalogTarget,
    onBack: () -> Unit,
    onPosterClick: ((MetaPreview) -> Unit)? = null,
    onPosterLongClick: ((MetaPreview) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by CatalogRepository.uiState.collectAsStateWithLifecycle()
    val homeCatalogSettingsUiState by HomeCatalogSettingsRepository.uiState.collectAsStateWithLifecycle()
    val posterCardStyle = rememberPosterCardStyleUiState()
    val networkStatusUiState by NetworkStatusRepository.uiState.collectAsStateWithLifecycle()
    val watchedUiState by remember {
        WatchedRepository.ensureLoaded()
        WatchedRepository.uiState
    }.collectAsStateWithLifecycle()
    val libraryGroupAllTitle = stringResource(Res.string.library_group_all)
    val libraryGroupWatchedTitle = stringResource(Res.string.library_group_watched)
    val libraryGroupUnwatchedTitle = stringResource(Res.string.library_group_unwatched)
    var selectedLibraryFilterName by remember { mutableStateOf(LibraryFilter.All.name) }
    val selectedLibraryFilter = remember(selectedLibraryFilterName) {
        runCatching { LibraryFilter.valueOf(selectedLibraryFilterName) }.getOrDefault(LibraryFilter.All)
    }
    val fullyWatchedSeriesKeys by WatchedRepository.fullyWatchedSeriesKeys.collectAsStateWithLifecycle()
    val initialScrollPosition = remember(
        target,
        homeCatalogSettingsUiState.hideUnreleasedContent,
    ) {
        CatalogRepository.scrollPosition(
            target = target,
        )
    }
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = initialScrollPosition.firstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialScrollPosition.firstVisibleItemScrollOffset,
    )
    var headerHeightPx by remember { mutableIntStateOf(0) }
    var observedOfflineState by remember { mutableStateOf(false) }

    LaunchedEffect(target) {
        if (target is CatalogTarget.Library) {
            selectedLibraryFilterName = LibraryFilter.All.name
        }
    }

    LaunchedEffect(target, homeCatalogSettingsUiState.hideUnreleasedContent) {
        CatalogRepository.load(
            target = target,
        )
    }

    LaunchedEffect(gridState, target, homeCatalogSettingsUiState.hideUnreleasedContent) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                CatalogRepository.saveScrollPosition(
                    target = target,
                    firstVisibleItemIndex = index,
                    firstVisibleItemScrollOffset = offset,
                )
            }
    }

    LaunchedEffect(gridState, uiState.canLoadMore, uiState.isLoading) {
        snapshotFlow { gridState.layoutInfo }
            .map { layoutInfo ->
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisible >= layoutInfo.totalItemsCount - 6
            }
            .distinctUntilChanged()
            .filter { it && uiState.canLoadMore && !uiState.isLoading }
            .collect {
                CatalogRepository.loadMore()
            }
    }

    LaunchedEffect(networkStatusUiState.condition, target) {
        when (networkStatusUiState.condition) {
            NetworkCondition.NoInternet,
            NetworkCondition.ServersUnreachable,
            -> {
                observedOfflineState = true
            }

            NetworkCondition.Online -> {
                if (!observedOfflineState) return@LaunchedEffect
                observedOfflineState = false
                CatalogRepository.load(
                    target = target,
                    force = true,
                )
            }

            NetworkCondition.Unknown,
            NetworkCondition.Checking,
            -> Unit
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val columns = remember(maxWidth) { catalogGridColumnsForWidth(maxWidth) }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = with(androidx.compose.ui.platform.LocalDensity.current) { headerHeightPx.toDp() } + 12.dp,
                    end = 16.dp,
                    bottom = nuvioSafeBottomPadding(28.dp),
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                if (uiState.items.isEmpty() && uiState.isLoading) {
                    items(columns * 3) {
                        CatalogSkeletonTile(cornerRadiusDp = posterCardStyle.cornerRadiusDp)
                    }
                } else if (uiState.items.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        CatalogEmptyState(
                            errorMessage = uiState.errorMessage,
                            networkCondition = networkStatusUiState.condition,
                            onRetry = {
                                NetworkStatusRepository.requestRefresh(force = true)
                                CatalogRepository.load(
                                    target = target,
                                    force = true,
                                )
                            },
                        )
                    }
                } else {
                    if (target is CatalogTarget.Library) {
                        val watchedItems = uiState.items.filter { item ->
                            WatchingState.isPosterWatched(
                                watchedKeys = watchedUiState.watchedKeys,
                                item = item,
                                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                            )
                        }
                        val unwatchedItems = uiState.items.filterNot { item ->
                            WatchingState.isPosterWatched(
                                watchedKeys = watchedUiState.watchedKeys,
                                item = item,
                                fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                            )
                        }
                        val filteredItems = when (selectedLibraryFilter) {
                            LibraryFilter.All -> uiState.items
                            LibraryFilter.Watched -> watchedItems
                            LibraryFilter.Unwatched -> unwatchedItems
                        }
                        items(
                            items = filteredItems.withDuplicateSafeLazyKeys { item -> item.stableKey() },
                            key = { item -> item.lazyKey },
                        ) { keyedItem ->
                            val item = keyedItem.value
                            CatalogPosterTile(
                                item = item,
                                cornerRadiusDp = posterCardStyle.cornerRadiusDp,
                                hideLabels = posterCardStyle.hideLabelsEnabled,
                                isWatched = WatchingState.isPosterWatched(
                                    watchedKeys = watchedUiState.watchedKeys,
                                    item = item,
                                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                ),
                                onClick = onPosterClick?.let { { it(item) } },
                                onLongClick = onPosterLongClick?.let { { it(item) } },
                            )
                        }
                    } else {
                        items(
                            items = uiState.items.withDuplicateSafeLazyKeys { item -> item.stableKey() },
                            key = { item -> item.lazyKey },
                        ) { keyedItem ->
                            val item = keyedItem.value
                            CatalogPosterTile(
                                item = item,
                                cornerRadiusDp = posterCardStyle.cornerRadiusDp,
                                hideLabels = posterCardStyle.hideLabelsEnabled,
                                isWatched = WatchingState.isPosterWatched(
                                    watchedKeys = watchedUiState.watchedKeys,
                                    item = item,
                                    fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                                ),
                                onClick = onPosterClick?.let { { it(item) } },
                                onLongClick = onPosterLongClick?.let { { it(item) } },
                            )
                        }
                    }
                    if (uiState.isLoading) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CatalogLoadingFooter()
                        }
                    }
                }
            }

            CatalogHeader(
                title = title,
                subtitle = subtitle,
                modifier = Modifier.onSizeChanged { headerHeightPx = it.height },
                showLibraryFilters = target is CatalogTarget.Library,
                selectedLibraryFilter = selectedLibraryFilter,
                onBack = onBack,
                onLibraryFilterSelected = { selectedLibraryFilterName = it.name },
                allLabel = "$libraryGroupAllTitle (${uiState.items.size})",
                watchedLabel = "$libraryGroupWatchedTitle (${
                    uiState.items.count {
                        WatchingState.isPosterWatched(
                            watchedKeys = watchedUiState.watchedKeys,
                            item = it,
                            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        )
                    }
                })",
                unwatchedLabel = "$libraryGroupUnwatchedTitle (${
                    uiState.items.count {
                        !WatchingState.isPosterWatched(
                            watchedKeys = watchedUiState.watchedKeys,
                            item = it,
                            fullyWatchedSeriesKeys = fullyWatchedSeriesKeys,
                        )
                    }
                })",
            )
        }
    }
}

@Composable
private fun CatalogHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    showLibraryFilters: Boolean = false,
    selectedLibraryFilter: LibraryFilter = LibraryFilter.All,
    allLabel: String = "",
    watchedLabel: String = "",
    unwatchedLabel: String = "",
    onLibraryFilterSelected: ((LibraryFilter) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .padding(top = 52.dp, bottom = 12.dp),
    ) {
        NuvioBackButton(
            onClick = onBack,
            modifier = Modifier
                .size(40.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            iconSize = 24.dp,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showLibraryFilters) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                ),
            ) {
                LibraryFilterRow(
                    selectedFilter = selectedLibraryFilter,
                    allLabel = allLabel,
                    watchedLabel = watchedLabel,
                    unwatchedLabel = unwatchedLabel,
                    onSelected = { onLibraryFilterSelected?.invoke(it) },
                )
            }
        }
    }
}

@Composable
private fun CatalogPosterTile(
    item: MetaPreview,
    cornerRadiusDp: Int,
    hideLabels: Boolean,
    isWatched: Boolean,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(item.posterShape.catalogAspectRatio())
                .clip(RoundedCornerShape(cornerRadiusDp.dp))
                .background(MaterialTheme.colorScheme.surface)
                .nuvioCardDepth(
                    shape = RoundedCornerShape(cornerRadiusDp.dp),
                    surface = NuvioCardDepthSurface.Posters,
                )
                .posterCardClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    zoomImageUrl = item.poster,
                    zoomCornerRadius = cornerRadiusDp.dp,
                ),
        ) {
            if (item.poster != null) {
                AsyncImage(
                    model = item.poster,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            NuvioPosterWatchedOverlay(isWatched = isWatched)
        }
        if (!hideLabels) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val detail = item.releaseInfo?.let { formatReleaseDateForDisplay(it) }
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CatalogSkeletonTile(cornerRadiusDp: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .background(MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun CatalogEmptyState(
    errorMessage: String?,
    networkCondition: NetworkCondition,
    onRetry: (() -> Unit)? = null,
) {
    if (networkCondition == NetworkCondition.NoInternet || networkCondition == NetworkCondition.ServersUnreachable) {
        NuvioNetworkOfflineCard(
            condition = networkCondition,
            onRetry = onRetry,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.catalog_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = errorMessage ?: stringResource(Res.string.catalog_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CatalogLoadingFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        NuvioLoadingIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun PosterShape.catalogAspectRatio(): Float =
    when (this) {
        PosterShape.Poster -> 0.68f
        PosterShape.Square -> 1f
        PosterShape.Landscape -> 1.78f
    }

private fun catalogGridColumnsForWidth(screenWidth: Dp): Int =
    when {
        screenWidth >= 1400.dp -> 7
        screenWidth >= 1200.dp -> 6
        screenWidth >= 1000.dp -> 5
        screenWidth >= 840.dp -> 4
        else -> 3
    }

private enum class LibraryFilter {
    All,
    Watched,
    Unwatched,
}

@Composable
private fun LibraryFilterRow(
    selectedFilter: LibraryFilter,
    allLabel: String,
    watchedLabel: String,
    unwatchedLabel: String,
    onSelected: (LibraryFilter) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            LibraryFilterChip(
                label = allLabel,
                selected = selectedFilter == LibraryFilter.All,
                onClick = { onSelected(LibraryFilter.All) },
            )
        }
        item {
            LibraryFilterChip(
                label = watchedLabel,
                selected = selectedFilter == LibraryFilter.Watched,
                onClick = { onSelected(LibraryFilter.Watched) },
            )
        }
        item {
            LibraryFilterChip(
                label = unwatchedLabel,
                selected = selectedFilter == LibraryFilter.Unwatched,
                onClick = { onSelected(LibraryFilter.Unwatched) },
            )
        }
    }
}

@Composable
private fun LibraryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.78f),
        tonalElevation = 0.dp,
        shadowElevation = if (selected) 2.dp else 0.dp,
        border = if (selected) null else androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
