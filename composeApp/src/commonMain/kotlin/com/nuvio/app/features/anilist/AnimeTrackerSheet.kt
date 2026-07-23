package com.nuvio.app.features.anilist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.nuvio.app.core.ui.NuvioLoadingIndicator
import com.nuvio.app.features.mal.MalApiClient
import com.nuvio.app.features.mal.MalAuthRepository
import com.nuvio.app.features.mal.MalConnectionMode
import com.nuvio.app.features.mal.resolveToMalId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.monthNumber
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AnimeTrackerSheet(
    contentId: String,
    videoId: String?,
    title: String,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current

    var isResolvingIds by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }

    var aniListId by remember { mutableStateOf<Int?>(null) }
    var malId by remember { mutableStateOf<Int?>(null) }
    var aniListEntryId by remember { mutableStateOf<Int?>(null) }

    var aniListTitle by remember { mutableStateOf<String?>(null) }
    var aniListImageUrl by remember { mutableStateOf<String?>(null) }
    var malTitle by remember { mutableStateOf<String?>(null) }
    var malImageUrl by remember { mutableStateOf<String?>(null) }

    var aniListStatus by remember { mutableStateOf("Watching") }
    var aniListScore by remember { mutableFloatStateOf(0f) }
    var aniListProgress by remember { mutableFloatStateOf(0f) }
    var showAniListInput by remember { mutableStateOf(false) }
    var aniListInputText by remember { mutableStateOf("") }
    var isAniListFavourite by remember { mutableStateOf(false) }

    var malStatus by remember { mutableStateOf("Watching") }
    var showMalInput by remember { mutableStateOf(false) }
    var malInputText by remember { mutableStateOf("") }
    var malScore by remember { mutableFloatStateOf(0f) }
    var malProgress by remember { mutableFloatStateOf(0f) }
    var totalRewatches by remember { mutableIntStateOf(0) }
    var maxEpisodes by remember { mutableIntStateOf(2000) }
    var notes by remember { mutableStateOf("") }

    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var finishDateMillis by remember { mutableStateOf<Long?>(null) }

    var isPrivate by remember { mutableStateOf(false) }
    var hideFromStatusLists by remember { mutableStateOf(false) }
    var storyScore by remember { mutableFloatStateOf(0f) }
    var charScore by remember { mutableFloatStateOf(0f) }
    var visualScore by remember { mutableFloatStateOf(0f) }
    var audioScore by remember { mutableFloatStateOf(0f) }
    var enjoymentScore by remember { mutableFloatStateOf(0f) }

    var priority by remember { mutableIntStateOf(0) }
    var rewatchValue by remember { mutableIntStateOf(0) }

    val aniListState by AniListAuthRepository.uiState.collectAsState()
    val malState by MalAuthRepository.uiState.collectAsState()

    var aniListStatusExpanded by remember { mutableStateOf(false) }
    var malStatusExpanded by remember { mutableStateOf(false) }
    val aniListStatuses = listOf("Watching", "Plan to Watch", "Completed", "Rewatching", "Paused", "Dropped")
    val malStatuses = listOf("Watching", "Completed", "On Hold", "Dropped", "Plan to Watch")

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showFinishDatePicker by remember { mutableStateOf(false) }
    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = startDateMillis)
    val finishDatePickerState = rememberDatePickerState(initialSelectedDateMillis = finishDateMillis)

    LaunchedEffect(contentId, videoId) {
        isResolvingIds = true
        isLoading = true
        aniListId = resolveToAniListId(contentId, videoId)
        malId = resolveToMalId(contentId, videoId)
        isResolvingIds = false
    }

    LaunchedEffect(aniListId) {
        storyScore = 0f
        charScore = 0f
        visualScore = 0f
        audioScore = 0f
        enjoymentScore = 0f
    }

    LaunchedEffect(malId) {
        priority = 0
        rewatchValue = 0
    }

    LaunchedEffect(aniListId, malId, isResolvingIds) {
        if (isResolvingIds) return@LaunchedEffect
        isLoading = true

        var isUiInitialized = false

        val aniJob = launch {
            if (aniListState.credentialsConfigured && aniListId != null) {
                val token = AniListAuthRepository.getAccessToken()
                if (!token.isNullOrBlank()) {
                    val entry = AniListApiClient.fetchListEntry(token, aniListId!!)
                    if (entry != null) {
                        aniListEntryId = entry.id
                        aniListTitle = entry.title
                        aniListImageUrl = entry.imageUrl
                        val fetchedStatus = when (entry.status) {
                            "CURRENT" -> "Watching"
                            "COMPLETED" -> "Completed"
                            "PAUSED" -> "Paused"
                            "DROPPED" -> "Dropped"
                            "PLANNING" -> "Plan to Watch"
                            "REPEATING" -> "Rewatching"
                            else -> "Watching"
                        }
                        if (aniListStatus == "Watching") aniListStatus = fetchedStatus
                        val rawScore = entry.score ?: 0.0
                        if (aniListScore == 0f) aniListScore = rawScore.toFloat()
                        isAniListFavourite = entry.isFavourite
                        val newProg = (entry.progress ?: 0).toFloat()
                        if (newProg > aniListProgress) aniListProgress = newProg
                        if (!isUiInitialized) {
                            totalRewatches = entry.repeat ?: 0
                            notes = entry.notes ?: ""
                            isPrivate = entry.private ?: false
                            hideFromStatusLists = entry.hiddenFromStatusLists ?: false
                            if (entry.startedAt != null && entry.startedAt.year != null && entry.startedAt.month != null && entry.startedAt.day != null) {
                                try {
                                    val ld = LocalDate(entry.startedAt.year, entry.startedAt.month, entry.startedAt.day)
                                    startDateMillis = ld.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                                } catch (_: Exception) {}
                            }
                            if (entry.completedAt != null && entry.completedAt.year != null && entry.completedAt.month != null && entry.completedAt.day != null) {
                                try {
                                    val ld = LocalDate(entry.completedAt.year, entry.completedAt.month, entry.completedAt.day)
                                    finishDateMillis = ld.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                                } catch (_: Exception) {}
                            }
                            isUiInitialized = true
                        }
                        isLoading = false
                        if (entry.maxEpisodes != null && entry.maxEpisodes > 0) maxEpisodes = entry.maxEpisodes
                        if (entry.advancedScores != null && entry.advancedScores.size >= 5) {
                            storyScore = entry.advancedScores[0].toFloat()
                            charScore = entry.advancedScores[1].toFloat()
                            visualScore = entry.advancedScores[2].toFloat()
                            audioScore = entry.advancedScores[3].toFloat()
                            enjoymentScore = entry.advancedScores[4].toFloat()
                        }
                    }
                }
            }
        }

        val malJob = launch {
            if (malState.credentialsConfigured && malId != null) {
                val token = MalAuthRepository.getAccessToken()
                if (!token.isNullOrBlank()) {
                    val entry = MalApiClient.fetchListEntry(token, malId!!)
                    if (entry != null) {
                        malTitle = entry.title
                        malImageUrl = entry.imageUrl
                        val fetchedStatusMal = when (entry.status) {
                            "watching" -> "Watching"
                            "completed" -> "Completed"
                            "on_hold" -> "On Hold"
                            "dropped" -> "Dropped"
                            "plan_to_watch" -> "Plan to Watch"
                            else -> "Watching"
                        }
                        if (malStatus == "Watching") malStatus = fetchedStatusMal
                        if (malScore == 0f) malScore = (entry.score ?: 0.0).toFloat()
                        val newProgMal = (entry.progress ?: 0).toFloat()
                        if (newProgMal > malProgress) malProgress = newProgMal
                        if (!isUiInitialized) {
                            totalRewatches = entry.numTimesRewatched ?: 0
                            notes = entry.comments ?: ""
                            if (!entry.startDate.isNullOrBlank()) {
                                try {
                                    val ld = LocalDate.parse(entry.startDate)
                                    startDateMillis = ld.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                                } catch (_: Exception) {}
                            }
                            if (!entry.finishDate.isNullOrBlank()) {
                                try {
                                    val ld = LocalDate.parse(entry.finishDate)
                                    finishDateMillis = ld.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                                } catch (_: Exception) {}
                            }
                            isUiInitialized = true
                        }
                        isLoading = false
                        if (entry.maxEpisodes != null && entry.maxEpisodes > 0 && maxEpisodes == 2000) maxEpisodes = entry.maxEpisodes
                        if (priority == 0) priority = entry.priority ?: 0
                        if (rewatchValue == 0) rewatchValue = entry.rewatchValue ?: 0
                    }
                }
            }
        }

        withTimeoutOrNull(5000) { joinAll(aniJob, malJob) }
        isLoading = false
    }

    LaunchedEffect(startDateMillis, finishDateMillis) {
        startDatePickerState.selectedDateMillis = startDateMillis
        finishDatePickerState.selectedDateMillis = finishDateMillis
    }

    var showSearchModal by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf("AniList") }

    if (showSearchModal) {
        var searchQuery by remember { mutableStateOf(title) }
        var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
        var isSearching by remember { mutableStateOf(false) }

        val performSearch: () -> Unit = {
            if (searchQuery.isNotBlank() && !isSearching) {
                scope.launch {
                    isSearching = true
                    if (searchMode == "AniList") {
                        val token = AniListAuthRepository.getAccessToken()
                        if (token != null) searchResults = AniListApiClient.searchAnime(token, searchQuery)
                    } else {
                        val token = MalAuthRepository.getAccessToken()
                        if (token != null) searchResults = MalApiClient.searchAnime(token, searchQuery)
                    }
                    isSearching = false
                }
            }
        }

        LaunchedEffect(showSearchModal) {
            if (showSearchModal) performSearch()
        }

        Dialog(onDismissRequest = { showSearchModal = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showSearchModal = false }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Back")
                        }
                        Text("Link $searchMode", style = MaterialTheme.typography.titleLarge)
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search Anime") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                performSearch()
                                focusManager.clearFocus()
                            }) {
                                Icon(Icons.Default.Search, contentDescription = null)
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    if (isSearching) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { NuvioLoadingIndicator() }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(searchResults.size) { i ->
                                val result = searchResults[i]
                                SearchItemCard(
                                    result = result,
                                    onClick = {
                                        if (searchMode == "AniList") {
                                            aniListId = result.id
                                            aniListTitle = result.title
                                            aniListImageUrl = result.imageUrl
                                        } else {
                                            malId = result.id
                                            malTitle = result.title
                                            malImageUrl = result.imageUrl
                                        }
                                        showSearchModal = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDateMillis = startDatePickerState.selectedDateMillis
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = startDatePickerState) }
    }

    if (showFinishDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showFinishDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    finishDateMillis = finishDatePickerState.selectedDateMillis
                    showFinishDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showFinishDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = finishDatePickerState) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Track Anime", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    NuvioLoadingIndicator()
                }
            } else {
                TrackerHeroCard(
                    title = aniListTitle ?: malTitle ?: title,
                    imageUrl = aniListImageUrl ?: malImageUrl,
                    subtitle = when {
                        aniListId != null && malId != null -> "Tracked on AniList and MAL"
                        aniListId != null -> "AniList linked"
                        malId != null -> "MyAnimeList linked"
                        else -> "No tracker linked yet"
                    },
                    scoreText = when {
                        aniListId != null -> renderAniListScore(aniListState.scoreFormat, aniListScore)
                        malId != null -> renderMalScore(malScore)
                        else -> "Unrated"
                    },
                    progressText = renderProgressText(aniListProgress, malProgress, maxEpisodes),
                    statusText = aniListStatus.takeIf { aniListId != null } ?: malStatus,
                    onOpen = {
                        when {
                            aniListId != null -> uriHandler.openUri("https://anilist.co/anime/$aniListId")
                            malId != null -> uriHandler.openUri("https://myanimelist.net/anime/$malId")
                        }
                    }
                )

                if (aniListState.mode == AniListConnectionMode.CONNECTED) {
                    if (aniListId != null) {
                        TrackingBlock(
                            label = "AniList",
                            linkedTitle = aniListTitle ?: "Linked",
                            imageUrl = aniListImageUrl,
                            onOpen = { uriHandler.openUri("https://anilist.co/anime/$aniListId") },
                            onChange = { searchMode = "AniList"; showSearchModal = true },
                            onUntrack = {
                                aniListId = null
                                scope.launch { AnimeTrackerMappingStorage.removeAniListOverride(contentId) }
                            },
                            onDelete = {
                                val currentId = aniListId
                                val currentEntryId = aniListEntryId
                                aniListId = null
                                scope.launch {
                                    AnimeTrackerMappingStorage.removeAniListOverride(contentId)
                                    if (currentId != null && currentEntryId != null) {
                                        val token = AniListAuthRepository.getAccessToken()
                                        if (!token.isNullOrBlank()) AniListApiClient.deleteEntry(token, currentEntryId)
                                    }
                                }
                            }
                        )
                    } else {
                        OutlinedButton(onClick = { searchMode = "AniList"; showSearchModal = true }, modifier = Modifier.fillMaxWidth()) { Text("Link AniList") }
                    }
                }

                if (malState.mode == MalConnectionMode.CONNECTED) {
                    if (malId != null) {
                        TrackingBlock(
                            label = "MyAnimeList",
                            linkedTitle = malTitle ?: "Linked",
                            imageUrl = malImageUrl,
                            onOpen = { uriHandler.openUri("https://myanimelist.net/anime/$malId") },
                            onChange = { searchMode = "MAL"; showSearchModal = true },
                            onUntrack = {
                                malId = null
                                scope.launch { AnimeTrackerMappingStorage.removeMalOverride(contentId) }
                            },
                            onDelete = {
                                val currentId = malId
                                malId = null
                                scope.launch {
                                    AnimeTrackerMappingStorage.removeMalOverride(contentId)
                                    if (currentId != null) {
                                        val token = MalAuthRepository.getAccessToken()
                                        if (!token.isNullOrBlank()) MalApiClient.deleteEntry(token, currentId)
                                    }
                                }
                            }
                        )
                    } else {
                        OutlinedButton(onClick = { searchMode = "MAL"; showSearchModal = true }, modifier = Modifier.fillMaxWidth()) { Text("Link MAL") }
                    }
                }

                if (aniListState.mode != AniListConnectionMode.CONNECTED && malState.mode != MalConnectionMode.CONNECTED) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Please connect AniList or MyAnimeList in Settings to track your anime.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    if (aniListState.mode == AniListConnectionMode.CONNECTED && aniListId != null) {
                        QuickActionRow(
                            onFavorite = {
                                isAniListFavourite = !isAniListFavourite
                                scope.launch {
                                    val token = AniListAuthRepository.getAccessToken()
                                    if (!token.isNullOrBlank()) {
                                        val success = AniListApiClient.toggleFavourite(token, aniListId!!)
                                        if (!success) isAniListFavourite = !isAniListFavourite
                                    }
                                }
                            },
                            favoriteSelected = isAniListFavourite,
                            onMinusOne = { if (aniListProgress > 0) aniListProgress -= 1f },
                            onPlusOne = { aniListProgress += 1f },
                            onComplete = {
                                aniListStatus = "Completed"
                                if (maxEpisodes > 0 && maxEpisodes != 2000) aniListProgress = maxEpisodes.toFloat()
                            }
                        )

                        StatusSection(
                            label = "Status",
                            value = aniListStatus,
                            expanded = aniListStatusExpanded,
                            onExpandedChange = { aniListStatusExpanded = it },
                            options = aniListStatuses,
                            onSelect = { s ->
                                aniListStatus = s
                                if (s == "Completed" && maxEpisodes < 2000) aniListProgress = maxEpisodes.toFloat()
                            }
                        )

                        ProgressSection(
                            label = "AniList Progress",
                            current = aniListProgress,
                            max = if (maxEpisodes > 0 && maxEpisodes != 2000) maxEpisodes.toFloat() else null,
                            showInlineInput = showAniListInput,
                            inputText = aniListInputText,
                            onInputTextChange = {
                                aniListInputText = it
                                it.toFloatOrNull()?.let { p -> aniListProgress = if (maxEpisodes > 0 && maxEpisodes != 2000) p.coerceIn(0f, maxEpisodes.toFloat()) else p }
                            },
                            onToggleInput = {
                                aniListInputText = aniListProgress.roundToInt().toString()
                                showAniListInput = !showAniListInput
                            },
                            onMinus = { if (aniListProgress > 0) aniListProgress -= 1f },
                            onPlus = { aniListProgress += 1f }
                        )

                        ScoreSection(
                            scoreFormat = aniListState.scoreFormat,
                            score = aniListScore,
                            onScoreChange = { aniListScore = it }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    if (malState.mode == MalConnectionMode.CONNECTED && malId != null) {
                        QuickActionRow(
                            onFavorite = null,
                            favoriteSelected = false,
                            onMinusOne = { if (malProgress > 0) malProgress -= 1f },
                            onPlusOne = { malProgress += 1f },
                            onComplete = {
                                malStatus = "Completed"
                                if (maxEpisodes > 0 && maxEpisodes != 2000) malProgress = maxEpisodes.toFloat()
                            }
                        )

                        StatusSection(
                            label = "Status",
                            value = malStatus,
                            expanded = malStatusExpanded,
                            onExpandedChange = { malStatusExpanded = it },
                            options = malStatuses,
                            onSelect = { s ->
                                malStatus = s
                                if (s == "Completed" && maxEpisodes < 2000) malProgress = maxEpisodes.toFloat()
                            }
                        )

                        ProgressSection(
                            label = "MAL Progress",
                            current = malProgress,
                            max = if (maxEpisodes > 0 && maxEpisodes != 2000) maxEpisodes.toFloat() else null,
                            showInlineInput = showMalInput,
                            inputText = malInputText,
                            onInputTextChange = {
                                malInputText = it
                                it.toFloatOrNull()?.let { p -> malProgress = if (maxEpisodes > 0 && maxEpisodes != 2000) p.coerceIn(0f, maxEpisodes.toFloat()) else p }
                            },
                            onToggleInput = {
                                malInputText = malProgress.roundToInt().toString()
                                showMalInput = !showMalInput
                            },
                            onMinus = { if (malProgress > 0) malProgress -= 1f },
                            onPlus = { malProgress += 1f }
                        )

                        Text("Score: ${if (malScore == 0f) "Unrated" else malScore.roundToInt().toString()}", style = MaterialTheme.typography.labelLarge)
                        Slider(value = malScore, onValueChange = { malScore = it.roundToInt().toFloat() }, valueRange = 0f..10f, steps = 9)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    }

                    if (aniListId != null || malId != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { showStartDatePicker = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (startDateMillis != null) Instant.fromEpochMilliseconds(startDateMillis!!).toLocalDateTime(TimeZone.UTC).date.toString() else "Start Date")
                            }
                            OutlinedButton(onClick = { showFinishDatePicker = true }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (finishDateMillis != null) Instant.fromEpochMilliseconds(finishDateMillis!!).toLocalDateTime(TimeZone.UTC).date.toString() else "Finish Date")
                            }
                        }

                        if (startDateMillis != null || finishDateMillis != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (startDateMillis != null) {
                                    TextButton(onClick = { startDateMillis = null; startDatePickerState.selectedDateMillis = null }, modifier = Modifier.weight(1f)) { Text("Clear Start Date") }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                                if (finishDateMillis != null) {
                                    TextButton(onClick = { finishDateMillis = null; finishDatePickerState.selectedDateMillis = null }, modifier = Modifier.weight(1f)) { Text("Clear Finish Date") }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Total Rewatches: $totalRewatches", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = { if (totalRewatches > 0) totalRewatches-- }) { Text("-") }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { totalRewatches++ }) { Text("+") }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        if (aniListId != null && aniListState.mode == AniListConnectionMode.CONNECTED) {
                            var anilistExpanded by remember { mutableStateOf(false) }
                            ElevatedCard(modifier = Modifier.clickable { anilistExpanded = !anilistExpanded }.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("AniList Advanced Options", style = MaterialTheme.typography.titleSmall)
                                    AnimatedVisibility(visible = anilistExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                                        Column {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                FilterChip(selected = isPrivate, onClick = { isPrivate = !isPrivate }, label = { Text("Private") })
                                                FilterChip(selected = hideFromStatusLists, onClick = { hideFromStatusLists = !hideFromStatusLists }, label = { Text("Hide from status lists") })
                                            }
                                            if (aniListState.advancedScoringEnabled) {
                                                val scoreItems = listOf(
                                                    "Story" to storyScore,
                                                    "Characters" to charScore,
                                                    "Visuals" to visualScore,
                                                    "Audio" to audioScore,
                                                    "Enjoyment" to enjoymentScore
                                                )
                                                scoreItems.forEachIndexed { i, (label, value) ->
                                                    val v = (value * 10).roundToInt()
                                                    val displayVal = "${v / 10}.${v % 10}"
                                                    Text("$label: ${if (value == 0f) "Unrated" else displayVal}", style = MaterialTheme.typography.bodySmall)
                                                    Slider(value = value, onValueChange = {
                                                        when (i) {
                                                            0 -> storyScore = it
                                                            1 -> charScore = it
                                                            2 -> visualScore = it
                                                            3 -> audioScore = it
                                                            4 -> enjoymentScore = it
                                                        }
                                                    }, valueRange = 0f..10f, steps = 99)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (malId != null && malState.mode == MalConnectionMode.CONNECTED) {
                            var malExpanded by remember { mutableStateOf(false) }
                            ElevatedCard(modifier = Modifier.clickable { malExpanded = !malExpanded }.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("MyAnimeList Advanced Options", style = MaterialTheme.typography.titleSmall)
                                    AnimatedVisibility(visible = malExpanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                                        Column {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Priority", style = MaterialTheme.typography.bodySmall)
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf("Low", "Medium", "High").forEachIndexed { i, p ->
                                                    FilterChip(selected = priority == i, onClick = { priority = i }, label = { Text(p) })
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Rewatch Value", style = MaterialTheme.typography.bodySmall)
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf("Very Low", "Low", "Medium", "High", "Very High").forEachIndexed { i, p ->
                                                    FilterChip(selected = rewatchValue == i + 1, onClick = { rewatchValue = i + 1 }, label = { Text(p) })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onDismiss()
                                CoroutineScope(Dispatchers.Default).launch {
                                    val startLd = startDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }
                                    val finishLd = finishDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }

                                    if (aniListId != null && aniListState.mode == AniListConnectionMode.CONNECTED) {
                                        AnimeTrackerMappingStorage.saveAniListOverride(contentId, aniListId!!)
                                        val token = AniListAuthRepository.getAccessToken()
                                        if (!token.isNullOrBlank()) {
                                            val aniStatus = when (aniListStatus) {
                                                "Watching" -> "CURRENT"
                                                "Completed" -> "COMPLETED"
                                                "On Hold", "Paused" -> "PAUSED"
                                                "Dropped" -> "DROPPED"
                                                "Plan to Watch" -> "PLANNING"
                                                "Rewatching" -> "REPEATING"
                                                else -> "CURRENT"
                                            }
                                            val advancedScoresList = listOf(storyScore, charScore, visualScore, audioScore, enjoymentScore).map { (kotlin.math.round(it.toDouble() * 10.0) / 10.0) }
                                            AniListApiClient.saveProgress(
                                                accessToken = token,
                                                mediaId = aniListId!!,
                                                progress = aniListProgress.roundToInt(),
                                                status = aniStatus,
                                                score = if (aniListScore > 0f) aniListScore.toDouble() else 0.0,
                                                repeat = totalRewatches,
                                                private = isPrivate,
                                                notes = notes,
                                                hiddenFromStatusLists = hideFromStatusLists,
                                                startedAt = startLd?.let { FuzzyDate(it.year, it.monthNumber, it.dayOfMonth) },
                                                completedAt = finishLd?.let { FuzzyDate(it.year, it.monthNumber, it.dayOfMonth) },
                                                clearStartDate = startLd == null,
                                                clearFinishDate = finishLd == null,
                                                advancedScores = advancedScoresList
                                            )
                                        }
                                    }

                                    if (malId != null && malState.mode == MalConnectionMode.CONNECTED) {
                                        AnimeTrackerMappingStorage.saveMalOverride(contentId, malId!!)
                                        val token = MalAuthRepository.getAccessToken()
                                        if (!token.isNullOrBlank()) {
                                            val malStatusString = when (malStatus) {
                                                "Watching" -> "watching"
                                                "Completed" -> "completed"
                                                "On Hold" -> "on_hold"
                                                "Dropped" -> "dropped"
                                                "Plan to Watch" -> "plan_to_watch"
                                                "Rewatching" -> "watching"
                                                else -> "watching"
                                            }
                                            MalApiClient.saveProgress(
                                                accessToken = token,
                                                animeId = malId!!,
                                                numWatchedEpisodes = malProgress.roundToInt(),
                                                status = malStatusString,
                                                score = if (malScore > 0f) malScore.toDouble() else 0.0,
                                                startDate = startLd?.toString() ?: "",
                                                finishDate = finishLd?.toString() ?: "",
                                                numTimesRewatched = totalRewatches,
                                                comments = notes,
                                                priority = priority,
                                                rewatchValue = rewatchValue
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save Tracking") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerHeroCard(
    title: String,
    imageUrl: String?,
    subtitle: String,
    scoreText: String,
    progressText: String,
    statusText: String,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.size(width = 88.dp, height = 124.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = true, onClick = {}, label = { Text(statusText) })
                        FilterChip(selected = true, onClick = {}, label = { Text(scoreText) })
                        FilterChip(selected = true, onClick = {}, label = { Text(progressText) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onOpen) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Open")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingBlock(
    label: String,
    linkedTitle: String,
    imageUrl: String?,
    onOpen: () -> Unit,
    onChange: () -> Unit,
    onUntrack: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Card(modifier = Modifier.size(56.dp, 80.dp)) {
                    AsyncImage(model = imageUrl, contentDescription = linkedTitle, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(linkedTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Linked entry", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen) { Icon(Icons.Default.OpenInNew, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Open") }
                OutlinedButton(onClick = onChange) { Icon(Icons.Default.Search, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Change") }
                OutlinedButton(onClick = onUntrack) { Icon(Icons.Default.Remove, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Untrack") }
                OutlinedButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    onFavorite: (() -> Unit)?,
    favoriteSelected: Boolean,
    onMinusOne: () -> Unit,
    onPlusOne: () -> Unit,
    onComplete: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onFavorite != null) {
            FilterChip(selected = favoriteSelected, onClick = onFavorite, label = { Text(if (favoriteSelected) "Favourited" else "Favourite") }, leadingIcon = {
                Icon(if (favoriteSelected) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null)
            })
        }
        OutlinedButton(onClick = onMinusOne) { Text("-1") }
        Button(onClick = onPlusOne) { Text("+1 Episode") }
        Button(onClick = onComplete) { Text("Complete") }
    }
}

@Composable
private fun StatusSection(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    Text(label, style = MaterialTheme.typography.labelLarge)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            options.forEach { s ->
                DropdownMenuItem(text = { Text(s) }, onClick = {
                    onSelect(s)
                    onExpandedChange(false)
                })
            }
        }
    }
}

@Composable
private fun ProgressSection(
    label: String,
    current: Float,
    max: Float?,
    showInlineInput: Boolean,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onToggleInput: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    val progressPercent = remember(current, max) {
        if (max != null && max > 0f) (current / max).coerceIn(0f, 1f) else 0f
    }
    Text(label, style = MaterialTheme.typography.labelLarge)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (showInlineInput) inputText else current.roundToInt().toString(),
                    modifier = Modifier.clickable { onToggleInput() },
                    fontWeight = FontWeight.Bold
                )
                Text(text = if (max != null) " / ${max.roundToInt()}" else " / ?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(text = if (max != null) "${(progressPercent * 100).roundToInt()}%" else "", color = MaterialTheme.colorScheme.primary)
            }
            Slider(value = current, onValueChange = { onInputTextChange(it.roundToInt().toString()) }, valueRange = 0f..(max ?: 2000f), steps = if (max != null && max > 1f) max.roundToInt() - 1 else 0)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onMinus) { Text("-1") }
                OutlinedButton(onClick = onToggleInput) { Text(if (showInlineInput) "Done" else "Edit") }
                Button(onClick = onPlus) { Text("+1") }
            }
        }
    }
}

@Composable
private fun ScoreSection(
    scoreFormat: String?,
    score: Float,
    onScoreChange: (Float) -> Unit,
) {
    val scoreRange = when (scoreFormat) {
        "POINT_100" -> 0f..100f
        "POINT_10_DECIMAL", "POINT_10", null -> 0f..10f
        "POINT_5" -> 0f..5f
        "POINT_3" -> 0f..3f
        else -> 0f..10f
    }
    val scoreText = when (scoreFormat) {
        "POINT_100" -> if (score == 0f) "Unrated" else score.roundToInt().toString()
        "POINT_10_DECIMAL" -> if (score == 0f) "Unrated" else "${(score * 10).roundToInt() / 10}.${(score * 10).roundToInt() % 10}"
        "POINT_5" -> if (score == 0f) "Unrated" else "${score.roundToInt()} Stars"
        "POINT_3" -> if (score == 0f) "Unrated" else when (score.roundToInt()) { 1 -> ":("; 2 -> ":|"; 3 -> ":)"; else -> "Unrated" }
        else -> if (score == 0f) "Unrated" else score.roundToInt().toString()
    }
    Text("Score: $scoreText", style = MaterialTheme.typography.labelLarge)
    Slider(value = score, onValueChange = { onScoreChange(it.roundToInt().toFloat()) }, valueRange = scoreRange, steps = when (scoreFormat) { "POINT_5" -> 4; "POINT_3" -> 2; "POINT_100" -> 99; else -> 9 })
}

private fun renderProgressText(aniListProgress: Float, malProgress: Float, maxEpisodes: Int): String {
    val progress = when {
        aniListProgress > 0f -> aniListProgress.roundToInt()
        malProgress > 0f -> malProgress.roundToInt()
        else -> 0
    }
    return if (maxEpisodes > 0 && maxEpisodes != 2000) {
        "$progress / $maxEpisodes"
    } else {
        "$progress / ?"
    }
}

private fun renderAniListScore(scoreFormat: String?, score: Float): String = when (scoreFormat) {
    "POINT_100" -> if (score == 0f) "Unrated" else score.roundToInt().toString()
    "POINT_10_DECIMAL" -> if (score == 0f) "Unrated" else "${(score * 10).roundToInt() / 10}.${(score * 10).roundToInt() % 10}"
    "POINT_5" -> if (score == 0f) "Unrated" else "${score.roundToInt()} Stars"
    "POINT_3" -> if (score == 0f) "Unrated" else when (score.roundToInt()) { 1 -> ":("; 2 -> ":|"; 3 -> ":)"; else -> "Unrated" }
    else -> if (score == 0f) "Unrated" else score.roundToInt().toString()
}

private fun renderMalScore(score: Float): String = if (score == 0f) "Unrated" else score.roundToInt().toString()
