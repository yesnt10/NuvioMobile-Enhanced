package com.nuvio.app.features.player.skip

import com.nuvio.app.features.details.MetaVideo
import kotlin.random.Random

object PlayerNextEpisodeRules {
    private const val RANDOM_HISTORY_LIMIT = 8
    private val randomEpisodeHistory = mutableMapOf<String, ArrayDeque<String>>()

    fun resolveNextEpisode(
        videos: List<MetaVideo>,
        currentSeason: Int?,
        currentEpisode: Int?,
        randomMode: Boolean = false,
        randomHistoryKey: String? = null,
    ): MetaVideo? {
        val sortedEpisodes = videos
            .filter { (it.season ?: 0) > 0 && it.episode != null }
            .sortedWith(
                compareBy<MetaVideo> { it.season ?: Int.MAX_VALUE }
                    .thenBy { it.episode ?: Int.MAX_VALUE }
            )

        if (sortedEpisodes.isEmpty()) return null

        if (randomMode) {
            val regularEpisodes = sortedEpisodes.filter { episode ->
                val season = episode.season ?: return@filter false
                val episodeNumber = episode.episode ?: return@filter false
                season > 0 && episodeNumber > 0 && episode.available
            }
            val airedCandidates = regularEpisodes.filter { hasEpisodeAired(it.released) }.ifEmpty { regularEpisodes }
            val candidates = airedCandidates
                .filterNot { episode ->
                    currentSeason != null && currentEpisode != null &&
                        episode.season == currentSeason && episode.episode == currentEpisode
                }
                .ifEmpty { airedCandidates.ifEmpty { regularEpisodes } }

            if (candidates.size == 1) return candidates.first()
            val historyKey = randomHistoryKey?.takeIf { it.isNotBlank() } ?: "global"
            val recent = randomEpisodeHistory.getOrPut(historyKey) { ArrayDeque() }
            val freshCandidates = candidates
                .filterNot { episode -> episode.randomEpisodeKey() in recent }
                .ifEmpty {
                    if (candidates.size > 2 && recent.isNotEmpty()) {
                        recent.removeFirst()
                        candidates.filterNot { episode -> episode.randomEpisodeKey() in recent }
                    } else {
                        candidates
                    }
                }
                .ifEmpty { candidates }

            val episodesBySeason = freshCandidates.groupBy { it.season }
            val seasons = episodesBySeason.keys.filterNotNull().let { availableSeasons ->
                availableSeasons.filter { it != currentSeason }.ifEmpty { availableSeasons }
            }
            if (seasons.isEmpty()) return null
            val selectedSeason = seasons.random()
            val seasonEpisodes = episodesBySeason.getValue(selectedSeason)
            val selected = seasonEpisodes[Random.nextInt(seasonEpisodes.size)]
            recent.addLast(selected.randomEpisodeKey())
            while (recent.size > RANDOM_HISTORY_LIMIT) {
                recent.removeFirst()
            }
            return selected
        }

        if (currentSeason == null || currentEpisode == null) return null
        val currentIndex = sortedEpisodes.indexOfFirst {
            it.season == currentSeason && it.episode == currentEpisode
        }
        if (currentIndex < 0) return null
        return sortedEpisodes.getOrNull(currentIndex + 1)
    }

    fun shouldShowNextEpisodeCard(
        positionMs: Long,
        durationMs: Long,
        skipIntervals: List<SkipInterval>,
        thresholdMode: NextEpisodeThresholdMode,
        thresholdPercent: Float,
        thresholdMinutesBeforeEnd: Float,
    ): Boolean {
        val outroSegments = skipIntervals.filter { it.type in OUTRO_SEGMENT_TYPES }

        if (outroSegments.isNotEmpty()) {
            if (durationMs <= 0L) return false
            val latestOutroEndMs = (outroSegments.maxOf { it.endTime } * 1_000.0).toLong()
            val postOutroGapMs = durationMs - latestOutroEndMs

            // Calculate the user's configured threshold as milliseconds from end.
            val userThresholdMs = when (thresholdMode) {
                NextEpisodeThresholdMode.PERCENTAGE -> {
                    val clampedPercent = thresholdPercent.coerceIn(97f, 100f)
                    ((1.0 - clampedPercent / 100.0) * durationMs).toLong()
                }
                NextEpisodeThresholdMode.MINUTES_BEFORE_END -> {
                    val clampedMinutes = thresholdMinutesBeforeEnd.coerceIn(0f, 3.5f)
                    (clampedMinutes * 60_000f).toLong()
                }
            }

            return if (postOutroGapMs > userThresholdMs) {
                when (thresholdMode) {
                    NextEpisodeThresholdMode.PERCENTAGE -> {
                        val clampedPercent = thresholdPercent.coerceIn(97f, 100f)
                        (positionMs.toDouble() / durationMs.toDouble()) >= (clampedPercent / 100.0)
                    }
                    NextEpisodeThresholdMode.MINUTES_BEFORE_END -> {
                        val clampedMinutes = thresholdMinutesBeforeEnd.coerceIn(0f, 3.5f)
                        val remainingMs = durationMs - positionMs
                        remainingMs <= (clampedMinutes * 60_000f).toLong()
                    }
                }
            } else {
                // Outro ends close to the file end — fire at earliest outro start.
                positionMs / 1_000.0 >= outroSegments.minOf { it.startTime }
            }
        }

        // Fallback to the settings threshold when no outro data exists.
        if (durationMs <= 0L) return false
        return when (thresholdMode) {
            NextEpisodeThresholdMode.PERCENTAGE -> {
                val clampedPercent = thresholdPercent.coerceIn(97f, 100f)
                (positionMs.toDouble() / durationMs.toDouble()) >= (clampedPercent / 100.0)
            }
            NextEpisodeThresholdMode.MINUTES_BEFORE_END -> {
                val clampedMinutes = thresholdMinutesBeforeEnd.coerceIn(0f, 3.5f)
                val remainingMs = durationMs - positionMs
                remainingMs <= (clampedMinutes * 60_000f).toLong()
            }
        }
    }

    fun hasEpisodeAired(raw: String?): Boolean {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return true
        val dateStr = when {
            value.length >= 10 -> value.substring(0, 10)
            else -> return true
        }
        // Parse YYYY-MM-DD
        val parts = dateStr.split("-")
        if (parts.size != 3) return true
        val year = parts[0].toIntOrNull() ?: return true
        val month = parts[1].toIntOrNull() ?: return true
        val day = parts[2].toIntOrNull() ?: return true

        val today = currentDateComponents()
        return compareDate(year, month, day, today.year, today.month, today.day) <= 0
    }

    private fun compareDate(
        y1: Int, m1: Int, d1: Int,
        y2: Int, m2: Int, d2: Int,
    ): Int {
        if (y1 != y2) return y1.compareTo(y2)
        if (m1 != m2) return m1.compareTo(m2)
        return d1.compareTo(d2)
    }

    val OUTRO_SEGMENT_TYPES = setOf("outro", "ed", "mixed-ed")

    private fun MetaVideo.randomEpisodeKey(): String =
        "${season ?: -1}x${episode ?: -1}:${id}"
}

internal expect fun currentDateComponents(): DateComponents

data class DateComponents(val year: Int, val month: Int, val day: Int)
