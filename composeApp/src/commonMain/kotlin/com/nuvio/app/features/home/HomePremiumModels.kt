package com.nuvio.app.features.home

import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaVideo
import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.library.toMetaPreview
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import kotlin.math.absoluteValue

internal data class HomeConciergeUiState(
    val profileName: String?,
    val headlineType: HomeConciergeHeadlineType,
    val cards: List<HomeConciergeCard>,
    val stats: List<HomeConciergeStat>,
    val chips: List<HomeConciergeChip>,
)

internal enum class HomeConciergeHeadlineType {
    Resume,
    Release,
    NextUp,
    Library,
    Discovery,
}

internal data class HomeConciergeCard(
    val key: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val logoUrl: String?,
    val reason: HomeConciergeReason,
    val preview: MetaPreview?,
    val continueWatchingItem: ContinueWatchingItem?,
    val smartSignal: HomeSmartResumeSignal? = null,
)

internal enum class HomeConciergeReason {
    Resume,
    NextUp,
    ReleaseRadar,
    LibraryPick,
    CatalogSignal,
}

internal enum class HomeSmartResumeSignal {
    ContinueNow,
    NextEpisodeReady,
    AlmostFinished,
    QuickResume,
    NewEpisode,
    NewSeason,
    Scheduled,
    ResumeReady,
}

internal data class HomeConciergeStat(
    val type: HomeConciergeStatType,
    val value: Int,
)

internal enum class HomeConciergeStatType {
    ContinueWatching,
    ReleaseRadar,
    Library,
    HighSignal,
}

internal data class HomeConciergeChip(
    val type: HomeConciergeChipType,
)

internal enum class HomeConciergeChipType {
    ProfileAware,
    WatchProgress,
    LibraryAware,
    ReleaseAware,
}

internal data class HomeReleaseRadarItem(
    val key: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: String?,
    val logoUrl: String?,
    val releaseIsoDate: String?,
    val daysFromToday: Int?,
    val category: HomeReleaseRadarCategory,
    val preview: MetaPreview,
    val continueWatchingItem: ContinueWatchingItem? = null,
)

internal enum class HomeReleaseRadarCategory {
    Episode,
    Movie,
    Series,
    NextUp,
}

internal fun buildHomeConciergeState(
    profileName: String?,
    continueWatchingItems: List<ContinueWatchingItem>,
    releaseRadarItems: List<HomeReleaseRadarItem>,
    libraryItems: List<LibraryItem>,
    catalogSections: List<HomeCatalogSection>,
    smartResumeEnabled: Boolean = true,
    releaseRadarEnabled: Boolean = true,
    profileStatsEnabled: Boolean = true,
): HomeConciergeUiState? {
    val contentContinueWatchingItems = continueWatchingItems.filterNot(ContinueWatchingItem::isLiveTvConciergeItem)
    val contentReleaseRadarItems = releaseRadarItems.filterNot { item ->
        item.continueWatchingItem?.isLiveTvConciergeItem() == true || item.preview.isLiveTvConciergeItem()
    }
    val contentLibraryItems = libraryItems.filterNot(LibraryItem::isLiveTvConciergeItem)

    val cards = buildList {
        if (smartResumeEnabled) {
            contentContinueWatchingItems
                .smartResumeCandidates()
                .take(HomeSmartResumeCandidateLimit)
                .forEach { item ->
                    add(
                        item.toConciergeCard(
                            reason = if (item.isNextUp) HomeConciergeReason.NextUp else HomeConciergeReason.Resume,
                            keyPrefix = "smart_resume",
                            smartSignal = item.smartResumeSignal(),
                        )
                    )
                }
        }

        if (releaseRadarEnabled) {
            contentReleaseRadarItems
                .firstOrNull { item -> item.daysFromToday != null && item.daysFromToday in 0..7 }
                ?.let { item ->
                    add(
                        HomeConciergeCard(
                            key = "release:${item.key}",
                            title = item.title,
                            subtitle = item.subtitle,
                            imageUrl = item.imageUrl,
                            logoUrl = item.logoUrl,
                            reason = HomeConciergeReason.ReleaseRadar,
                            preview = item.preview,
                            continueWatchingItem = item.continueWatchingItem,
                        )
                    )
                }
        }

        contentLibraryItems
            .asSequence()
            .sortedWith(
                compareByDescending<LibraryItem> { item -> item.imdbRating.ratingScoreOrNull() ?: -1.0 }
                    .thenByDescending { item -> item.savedAtEpochMs }
            )
            .firstOrNull()
            ?.let { item ->
                val preview = item.toMetaPreview()
                add(
                    HomeConciergeCard(
                        key = "library:${preview.stableKey()}",
                        title = preview.name,
                        subtitle = preview.conciergeMetaLine(),
                        imageUrl = firstNonBlank(preview.banner, preview.poster),
                        logoUrl = preview.logo,
                        reason = HomeConciergeReason.LibraryPick,
                        preview = preview,
                        continueWatchingItem = null,
                    )
                )
            }

        catalogSections
            .flatMap { section -> section.items }
            .distinctBy(MetaPreview::stableKey)
            .sortedWith(
                compareByDescending<MetaPreview> { item -> item.imdbRating.ratingScoreOrNull() ?: -1.0 }
                    .thenByDescending { item -> item.popularity ?: -1.0 }
                    .thenByDescending { item -> item.voteCount ?: -1 }
            )
            .firstOrNull()
            ?.let { preview ->
                add(
                    HomeConciergeCard(
                        key = "catalog:${preview.stableKey()}",
                        title = preview.name,
                        subtitle = preview.conciergeMetaLine(),
                        imageUrl = firstNonBlank(preview.banner, preview.poster),
                        logoUrl = preview.logo,
                        reason = HomeConciergeReason.CatalogSignal,
                        preview = preview,
                        continueWatchingItem = null,
                    )
                )
            }
    }
        .distinctBy { card -> card.preview?.stableKey() ?: card.continueWatchingItem?.videoId ?: card.key }
        .take(HomeConciergeCardLimit)

    val highSignalCount = catalogSections
        .flatMap { section -> section.items }
        .count { item -> (item.imdbRating.ratingScoreOrNull() ?: 0.0) >= HomeConciergeHighSignalRating }

    val stats = if (profileStatsEnabled) {
        listOfNotNull(
            contentContinueWatchingItems.size.takeIf { it > 0 }?.let {
                HomeConciergeStat(HomeConciergeStatType.ContinueWatching, it)
            },
            contentReleaseRadarItems.size.takeIf { releaseRadarEnabled && it > 0 }?.let {
                HomeConciergeStat(HomeConciergeStatType.ReleaseRadar, it)
            },
            contentLibraryItems.size.takeIf { it > 0 }?.let {
                HomeConciergeStat(HomeConciergeStatType.Library, it)
            },
            highSignalCount.takeIf { it > 0 }?.let {
                HomeConciergeStat(HomeConciergeStatType.HighSignal, it)
            },
        ).take(HomeConciergeStatLimit)
    } else {
        emptyList()
    }

    if (cards.isEmpty() && stats.isEmpty()) return null

    val headlineType = when (cards.firstOrNull()?.reason) {
        HomeConciergeReason.Resume -> HomeConciergeHeadlineType.Resume
        HomeConciergeReason.ReleaseRadar -> HomeConciergeHeadlineType.Release
        HomeConciergeReason.NextUp -> HomeConciergeHeadlineType.NextUp
        HomeConciergeReason.LibraryPick -> HomeConciergeHeadlineType.Library
        HomeConciergeReason.CatalogSignal,
        null,
        -> HomeConciergeHeadlineType.Discovery
    }

    val chips = buildList {
        if (!profileName.isNullOrBlank()) add(HomeConciergeChip(HomeConciergeChipType.ProfileAware))
        if (smartResumeEnabled && contentContinueWatchingItems.isNotEmpty()) add(HomeConciergeChip(HomeConciergeChipType.WatchProgress))
        if (contentLibraryItems.isNotEmpty()) add(HomeConciergeChip(HomeConciergeChipType.LibraryAware))
        if (releaseRadarEnabled && contentReleaseRadarItems.isNotEmpty()) add(HomeConciergeChip(HomeConciergeChipType.ReleaseAware))
    }.take(HomeConciergeChipLimit)

    return HomeConciergeUiState(
        profileName = profileName?.trim()?.takeIf { it.isNotBlank() },
        headlineType = headlineType,
        cards = cards,
        stats = stats,
        chips = chips,
    )
}

private fun ContinueWatchingItem.isLiveTvConciergeItem(): Boolean =
    parentMetaType.isLiveTvConciergeValue() ||
        parentMetaId.isLiveTvConciergeValue() ||
        videoId.isLiveTvConciergeValue() ||
        title.isLiveTvConciergeValue()

private fun LibraryItem.isLiveTvConciergeItem(): Boolean =
    type.isLiveTvConciergeValue() ||
        id.isLiveTvConciergeValue() ||
        name.isLiveTvConciergeValue()

private fun MetaPreview.isLiveTvConciergeItem(): Boolean =
    type.isLiveTvConciergeValue() ||
        id.isLiveTvConciergeValue() ||
        name.isLiveTvConciergeValue()

private fun String.isLiveTvConciergeValue(): Boolean {
    val value = trim().lowercase()
    return value in setOf(
        "live-tv",
        "livetv",
        "live_tv",
        "channel",
        "tv-channel",
        "tv_channel",
        "iptv",
        "m3u",
        "stalker",
    ) ||
        value.startsWith("http://") ||
        value.startsWith("https://") ||
        value.startsWith("rtmp://") ||
        value.startsWith("rtsp://") ||
        value.endsWith(".m3u") ||
        value.endsWith(".m3u8")
}

@Suppress("UNUSED_PARAMETER")
internal fun buildHomeReleaseRadarItems(
    todayIsoDate: String,
    continueWatchingItems: List<ContinueWatchingItem>,
    libraryItems: List<LibraryItem>,
    catalogSections: List<HomeCatalogSection>,
    resolvedLibraryDetails: Map<String, MetaDetails>,
    includeRecentPastReleases: Boolean = false,
): List<HomeReleaseRadarItem> {
    val earliestReleaseDay = if (includeRecentPastReleases) {
        -HomeReleaseRadarRecentWindowDays
    } else {
        0
    }
    val releaseItems = buildList {
        continueWatchingItems
            .asSequence()
            .filter { item -> item.isNextUp && !item.released.isNullOrBlank() }
            .mapNotNull { item ->
                val releaseIso = item.releaseIsoDateOrNull() ?: return@mapNotNull null
                val days = daysBetweenIsoDates(todayIsoDate, releaseIso) ?: return@mapNotNull null
                if (days !in earliestReleaseDay..HomeReleaseRadarUpcomingWindowDays) return@mapNotNull null
                item.toReleaseRadarItem(
                    releaseIsoDate = releaseIso,
                    daysFromToday = days,
                    category = HomeReleaseRadarCategory.NextUp,
                )
            }
            .forEach(::add)

        val libraryItemsByRadarKey = libraryItems.associateBy(::libraryItemKeyForHomeRadar)
        resolvedLibraryDetails.forEach { (radarKey, details) ->
            val libraryItem = libraryItemsByRadarKey[radarKey] ?: return@forEach
            details.videos
                .asSequence()
                .mapNotNull { video ->
                    val releaseIso = video.released.releaseIsoDateOrNull() ?: return@mapNotNull null
                    val days = daysBetweenIsoDates(todayIsoDate, releaseIso) ?: return@mapNotNull null
                    if (days !in earliestReleaseDay..HomeReleaseRadarUpcomingWindowDays) {
                        return@mapNotNull null
                    }
                    details.toReleaseRadarItem(
                        video = video,
                        libraryItem = libraryItem,
                        releaseIsoDate = releaseIso,
                        daysFromToday = days,
                    )
                }
                .forEach(::add)
        }

        libraryItems
            .asSequence()
            .mapNotNull { item ->
                val releaseIso = item.releaseIsoDateOrNull() ?: return@mapNotNull null
                val days = daysBetweenIsoDates(todayIsoDate, releaseIso) ?: return@mapNotNull null
                if (days !in 0..HomeReleaseRadarUpcomingWindowDays) return@mapNotNull null
                item.toReleaseRadarItem(
                    releaseIsoDate = releaseIso,
                    daysFromToday = days,
                    category = if (item.type.isSeriesLikeType()) HomeReleaseRadarCategory.Series else HomeReleaseRadarCategory.Movie,
                )
            }
            .forEach(::add)

    }

    return releaseItems
        .distinctBy { item -> item.key }
        .sortedWith(
            compareBy<HomeReleaseRadarItem> { item -> item.daysFromToday?.releaseRadarSortWeight() ?: Int.MAX_VALUE }
                .thenByDescending { item -> item.releaseRadarSignalWeight() }
                .thenBy { item -> item.title.lowercase() }
        )
        .take(HomeReleaseRadarItemLimit)
}

internal fun libraryItemKeyForHomeRadar(item: LibraryItem): String =
    "${item.type.trim().lowercase()}:${item.id.trim()}"

internal fun List<LibraryItem>.homeRadarDetailsRequestKey(): String =
    asSequence()
        .filter { item -> item.type.isSeriesLikeType() }
        .map(::libraryItemKeyForHomeRadar)
        .sorted()
        .joinToString(separator = "|")

private fun ContinueWatchingItem.toConciergeCard(
    reason: HomeConciergeReason,
    keyPrefix: String,
    smartSignal: HomeSmartResumeSignal? = null,
): HomeConciergeCard =
    HomeConciergeCard(
        key = listOfNotNull(keyPrefix, smartSignal?.name, videoId).joinToString(separator = ":"),
        title = title,
        subtitle = subtitle.takeIf { it.isNotBlank() },
        imageUrl = firstNonBlank(background, episodeThumbnail, imageUrl, poster),
        logoUrl = logo,
        reason = reason,
        preview = MetaPreview(
            id = parentMetaId,
            type = parentMetaType,
            name = title,
            poster = poster ?: imageUrl,
            banner = background ?: imageUrl,
            logo = logo,
            releaseInfo = released,
        ),
        continueWatchingItem = this,
        smartSignal = smartSignal,
    )

private fun List<ContinueWatchingItem>.smartResumeCandidates(): List<ContinueWatchingItem> =
    mapIndexed { index, item -> index to item }
        .filter { (_, item) ->
            item.isReleaseAlert ||
                item.isNextUp ||
                (!item.isNextUp && item.progressFraction in 0.05f..0.95f)
        }
        .sortedWith(
            compareByDescending<Pair<Int, ContinueWatchingItem>> { (index, item) -> item.smartResumeScore(index) }
                .thenBy { (index, _) -> index }
        )
        .map { (_, item) -> item }

private fun ContinueWatchingItem.smartResumeSignal(): HomeSmartResumeSignal =
    when {
        isReleaseAlert && isNewSeasonRelease -> HomeSmartResumeSignal.NewSeason
        isReleaseAlert -> HomeSmartResumeSignal.NewEpisode
        isNextUp && !released.isNullOrBlank() -> HomeSmartResumeSignal.Scheduled
        isNextUp -> HomeSmartResumeSignal.NextEpisodeReady
        remainingMinutesOrNull()?.let { it <= HomeSmartResumeAlmostFinishedMinutes } == true -> {
            HomeSmartResumeSignal.AlmostFinished
        }
        remainingMinutesOrNull()?.let { it <= HomeSmartResumeQuickResumeMinutes } == true -> {
            HomeSmartResumeSignal.QuickResume
        }
        progressFraction >= HomeSmartResumeContinueNowProgress -> HomeSmartResumeSignal.ContinueNow
        else -> HomeSmartResumeSignal.ResumeReady
    }

private fun ContinueWatchingItem.smartResumeScore(index: Int): Int {
    var score = HomeSmartResumeBaseScore - (index * HomeSmartResumeIndexPenalty)
    if (isReleaseAlert) score += HomeSmartResumeReleaseScore
    if (isNewSeasonRelease) score += HomeSmartResumeNewSeasonScore
    if (isNextUp) score += HomeSmartResumeNextUpScore
    if (!isNextUp && progressFraction in 0.05f..0.95f) score += HomeSmartResumeInProgressScore
    val remainingMinutes = remainingMinutesOrNull()
    if (remainingMinutes != null) {
        score += when {
            remainingMinutes <= HomeSmartResumeAlmostFinishedMinutes -> HomeSmartResumeAlmostFinishedScore
            remainingMinutes <= HomeSmartResumeQuickResumeMinutes -> HomeSmartResumeQuickResumeScore
            else -> 0
        }
    }
    if (progressFraction >= HomeSmartResumeContinueNowProgress) score += HomeSmartResumeContinueNowScore
    if (progressFraction < 0.05f && !isNextUp) score -= HomeSmartResumeColdStartPenalty
    if (progressFraction > 0.95f && !isNextUp) score -= HomeSmartResumeCompletedPenalty
    return score
}

private fun ContinueWatchingItem.remainingMinutesOrNull(): Long? {
    if (durationMs <= 0L || progressFraction <= 0f) return null
    val progressPositionMs = (durationMs * progressFraction.coerceIn(0f, 1f)).toLong()
    val effectivePositionMs = if (resumePositionMs > 0L) resumePositionMs else progressPositionMs
    return ((durationMs - effectivePositionMs).coerceAtLeast(0L) / 60_000L).coerceAtLeast(1L)
}

private fun ContinueWatchingItem.toReleaseRadarItem(
    releaseIsoDate: String,
    daysFromToday: Int,
    category: HomeReleaseRadarCategory,
): HomeReleaseRadarItem {
    val preview = MetaPreview(
        id = parentMetaId,
        type = parentMetaType,
        name = title,
        poster = poster ?: imageUrl,
        banner = background ?: imageUrl,
        logo = logo,
        releaseInfo = released,
    )
    return HomeReleaseRadarItem(
        key = "cw:${parentMetaType}:${parentMetaId}:${seasonNumber ?: -1}:${episodeNumber ?: -1}:$releaseIsoDate",
        title = title,
        subtitle = subtitle.takeIf { it.isNotBlank() },
        imageUrl = firstNonBlank(episodeThumbnail, background, imageUrl, poster),
        logoUrl = logo,
        releaseIsoDate = releaseIsoDate,
        daysFromToday = daysFromToday,
        category = category,
        preview = preview,
        continueWatchingItem = this,
    )
}

private fun MetaDetails.toReleaseRadarItem(
    video: MetaVideo,
    libraryItem: LibraryItem?,
    releaseIsoDate: String,
    daysFromToday: Int,
): HomeReleaseRadarItem {
    val preview = MetaPreview(
        id = id,
        type = type,
        name = name,
        poster = poster ?: libraryItem?.poster,
        banner = background ?: libraryItem?.banner,
        logo = logo ?: libraryItem?.logo,
        description = description ?: libraryItem?.description,
        releaseInfo = releaseInfo ?: libraryItem?.releaseInfo,
        imdbRating = imdbRating ?: libraryItem?.imdbRating,
        genres = genres.ifEmpty { libraryItem?.genres.orEmpty() },
    )
    return HomeReleaseRadarItem(
        key = "episode:${type}:${id}:${video.season ?: -1}:${video.episode ?: -1}:$releaseIsoDate",
        title = name,
        subtitle = buildEpisodeSubtitle(video),
        imageUrl = firstNonBlank(video.thumbnail, background, poster, libraryItem?.banner, libraryItem?.poster),
        logoUrl = logo ?: libraryItem?.logo,
        releaseIsoDate = releaseIsoDate,
        daysFromToday = daysFromToday,
        category = HomeReleaseRadarCategory.Episode,
        preview = preview,
    )
}

private fun LibraryItem.toReleaseRadarItem(
    releaseIsoDate: String,
    daysFromToday: Int,
    category: HomeReleaseRadarCategory,
): HomeReleaseRadarItem {
    val preview = toMetaPreview()
    return HomeReleaseRadarItem(
        key = "library:${preview.stableKey()}:$releaseIsoDate",
        title = preview.name,
        subtitle = preview.conciergeMetaLine(),
        imageUrl = firstNonBlank(preview.banner, preview.poster),
        logoUrl = preview.logo,
        releaseIsoDate = releaseIsoDate,
        daysFromToday = daysFromToday,
        category = category,
        preview = preview,
    )
}

private fun ContinueWatchingItem.releaseIsoDateOrNull(): String? =
    released.releaseIsoDateOrNull()

private fun LibraryItem.releaseIsoDateOrNull(): String? =
    firstNonBlank(releaseInfo)?.releaseIsoDateOrNull()

private fun MetaPreview.releaseIsoDateOrNull(): String? =
    firstNonBlank(rawReleaseDate, releaseInfo)?.releaseIsoDateOrNull()

private fun String?.releaseIsoDateOrNull(): String? {
    val datePart = this
        ?.trim()
        ?.substringBefore('T')
        ?.takeIf { it.length == 10 }
        ?: return null
    val parts = datePart.split('-')
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val day = parts[2].toIntOrNull()?.takeIf { it in 1..daysInMonth(year, month) } ?: return null
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private fun daysBetweenIsoDates(startIsoDate: String, targetIsoDate: String): Int? {
    val start = startIsoDate.releaseIsoDateOrNull() ?: return null
    val target = targetIsoDate.releaseIsoDateOrNull() ?: return null
    return (isoEpochDay(target) - isoEpochDay(start)).toInt()
}

private fun Int.releaseRadarSortWeight(): Int =
    if (this >= 0) this else HomeReleaseRadarUpcomingWindowDays + absoluteValue

private fun HomeReleaseRadarItem.releaseRadarSignalWeight(): Int =
    when (category) {
        HomeReleaseRadarCategory.NextUp -> 5
        HomeReleaseRadarCategory.Episode -> 4
        HomeReleaseRadarCategory.Series -> 3
        HomeReleaseRadarCategory.Movie -> 2
    }

private fun String?.ratingScoreOrNull(): Double? {
    val normalized = this
        ?.trim()
        ?.replace(',', '.')
        ?.takeIf { it.isNotBlank() }
        ?: return null
    return normalized.substringBefore('/').toDoubleOrNull()
}

private fun MetaPreview.conciergeMetaLine(): String? =
    listOfNotNull(
        imdbRating?.takeIf { it.isNotBlank() }?.let { "IMDb $it" },
        releaseInfo?.takeIf { it.isNotBlank() },
        genres.firstOrNull(),
    ).joinToString(" • ").takeIf { it.isNotBlank() }

private fun buildEpisodeSubtitle(video: MetaVideo): String? {
    val episodeCode = when {
        video.season != null && video.episode != null -> "S${video.season}E${video.episode}"
        video.episode != null -> "E${video.episode}"
        else -> null
    }
    return listOfNotNull(episodeCode, video.title.takeIf { it.isNotBlank() })
        .joinToString(" • ")
        .takeIf { it.isNotBlank() }
}

private fun String.isSeriesLikeType(): Boolean =
    trim().lowercase() in setOf("series", "show", "tv", "tvshow")

private fun firstNonBlank(vararg values: String?): String? =
    values.firstOrNull { value -> !value.isNullOrBlank() }?.trim()

private fun daysInMonth(year: Int, month: Int): Int =
    when (month) {
        2 -> if (isLeapYear(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }

private fun isLeapYear(year: Int): Boolean =
    year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

private fun isoEpochDay(date: String): Long {
    val year = date.substring(0, 4).toLong()
    val month = date.substring(5, 7).toLong()
    val day = date.substring(8, 10).toLong()
    val adjustedYear = year - if (month <= 2L) 1L else 0L
    val era = if (adjustedYear >= 0L) adjustedYear / 400L else (adjustedYear - 399L) / 400L
    val yearOfEra = adjustedYear - era * 400L
    val adjustedMonth = month + if (month > 2L) -3L else 9L
    val dayOfYear = (153L * adjustedMonth + 2L) / 5L + day - 1L
    val dayOfEra = yearOfEra * 365L + yearOfEra / 4L - yearOfEra / 100L + dayOfYear
    return era * 146_097L + dayOfEra - 719_468L
}

private const val HomeConciergeCardLimit = 4
private const val HomeConciergeStatLimit = 4
private const val HomeConciergeChipLimit = 4
private const val HomeConciergeHighSignalRating = 8.0
private const val HomeSmartResumeCandidateLimit = 3
private const val HomeSmartResumeBaseScore = 120
private const val HomeSmartResumeIndexPenalty = 5
private const val HomeSmartResumeReleaseScore = 70
private const val HomeSmartResumeNewSeasonScore = 15
private const val HomeSmartResumeNextUpScore = 55
private const val HomeSmartResumeInProgressScore = 30
private const val HomeSmartResumeAlmostFinishedScore = 34
private const val HomeSmartResumeQuickResumeScore = 24
private const val HomeSmartResumeContinueNowScore = 16
private const val HomeSmartResumeColdStartPenalty = 30
private const val HomeSmartResumeCompletedPenalty = 40
private const val HomeSmartResumeAlmostFinishedMinutes = 30L
private const val HomeSmartResumeQuickResumeMinutes = 75L
private const val HomeSmartResumeContinueNowProgress = 0.70f
internal const val HomeReleaseRadarUpcomingWindowDays = 45
private const val HomeReleaseRadarRecentWindowDays = 7
private const val HomeReleaseRadarItemLimit = 30
