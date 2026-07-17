package com.nuvio.app.features.anilist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import kotlinx.datetime.*
import kotlinx.datetime.TimeZone
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import com.nuvio.app.features.anilist.SearchResult
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

    var isResolvingIds by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var aniListId by remember { mutableStateOf<Int?>(null) }
    var malId by remember { mutableStateOf<Int?>(null) }
    var aniListEntryId by remember { mutableStateOf<Int?>(null) }

    var aniListTitle by remember { mutableStateOf<String?>(null) }
    var aniListImageUrl by remember { mutableStateOf<String?>(null) }
    var malTitle by remember { mutableStateOf<String?>(null) }
    var malImageUrl by remember { mutableStateOf<String?>(null) }

    // User editable state
    var aniListStatus by remember { mutableStateOf("Watching") }
    var aniListScore by remember { mutableStateOf(0f) }
    var aniListProgress by remember { mutableStateOf(0f) }
    var showAniListInput by remember { mutableStateOf(false) }
    var aniListInputText by remember { mutableStateOf("") }
    var isAniListFavourite by remember { mutableStateOf(false) }
    
    var malStatus by remember { mutableStateOf("Watching") }
    var showMalInput by remember { mutableStateOf(false) }
    var malInputText by remember { mutableStateOf("") }
    var malScore by remember { mutableStateOf(0f) }
    var malProgress by remember { mutableStateOf(0f) }
    var totalRewatches by remember { mutableStateOf(0) }
    var maxEpisodes by remember { mutableStateOf(2000) }
    var notes by remember { mutableStateOf("") }
    
    var startDateMillis by remember { mutableStateOf<Long?>(null) }
    var finishDateMillis by remember { mutableStateOf<Long?>(null) }
    
    // AniList Specific
    var isPrivate by remember { mutableStateOf(false) }
    var hideFromStatusLists by remember { mutableStateOf(false) }
    var storyScore by remember { mutableStateOf(0f) }
    var charScore by remember { mutableStateOf(0f) }
    var visualScore by remember { mutableStateOf(0f) }
    var audioScore by remember { mutableStateOf(0f) }
    var enjoymentScore by remember { mutableStateOf(0f) }

    // MAL Specific
    var priority by remember { mutableStateOf(0) }
    var rewatchValue by remember { mutableStateOf(0) }

    val aniListState by AniListAuthRepository.uiState.collectAsState()
    val malState by MalAuthRepository.uiState.collectAsState()

    var aniListStatusExpanded by remember { mutableStateOf(false) }
    var malStatusExpanded by remember { mutableStateOf(false) }
    val aniListStatuses = listOf("Watching", "Plan to Watch", "Completed", "Rewatching", "Paused", "Dropped")
    val malStatuses = listOf("Watching", "Completed", "On Hold", "Dropped", "Plan to Watch")

    // Date Picker state
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
                                } catch (e: Exception) {}
                            }
                            if (entry.completedAt != null && entry.completedAt.year != null && entry.completedAt.month != null && entry.completedAt.day != null) {
                                try {
                                    val ld = LocalDate(entry.completedAt.year, entry.completedAt.month, entry.completedAt.day)
                                    finishDateMillis = ld.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                                } catch (e: Exception) {}
                            }
                            
                            isUiInitialized = true
                        }
                        isLoading = false

                        if (entry.maxEpisodes != null && entry.maxEpisodes > 0) {
                            maxEpisodes = entry.maxEpisodes
                        }
                        
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
                                } catch (e: Exception) {}
                            }
                            if (!entry.finishDate.isNullOrBlank()) {
                                try {
                                    val ld = LocalDate.parse(entry.finishDate)
                                    finishDateMillis = ld.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                                } catch (e: Exception) {}
                            }
                            isUiInitialized = true
                        }
                        isLoading = false

                        if (entry.maxEpisodes != null && entry.maxEpisodes > 0 && maxEpisodes == 2000) {
                            maxEpisodes = entry.maxEpisodes
                        }
                        
                        // MAL advanced options
                        if (priority == 0) priority = entry.priority ?: 0
                        if (rewatchValue == 0) rewatchValue = entry.rewatchValue ?: 0
                    }
                }
            }
        }
        
        withTimeoutOrNull(5000) {
            joinAll(aniJob, malJob)
        }
        isLoading = false
    }
    
    LaunchedEffect(startDateMillis, finishDateMillis) {
        startDatePickerState.selectedDateMillis = startDateMillis
        finishDatePickerState.selectedDateMillis = finishDateMillis
    }

    var showSearchModal by remember { mutableStateOf(false) }
    var searchMode by remember { mutableStateOf("AniList") } // "AniList" or "MAL"

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
            if (showSearchModal) {
                performSearch()
            }
        }
        
        Dialog(
            onDismissRequest = { showSearchModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showSearchModal = false }) {
                            Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown, contentDescription = "Back") // Placeholder for back
                        }
                        Text("Link $searchMode", style = MaterialTheme.typography.titleLarge)
                    }
                    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search Anime") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { 
                            performSearch() 
                            focusManager.clearFocus()
                        }),
                        trailingIcon = {
                            IconButton(onClick = {
                                performSearch()
                                focusManager.clearFocus()
                            }) {
                                Text("Go")
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    if (isSearching) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { NuvioLoadingIndicator() }
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
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
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
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
            dismissButton = {
                TextButton(onClick = { showFinishDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = finishDatePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Track Anime", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    NuvioLoadingIndicator()
                }
            } else {
                if (aniListState.mode == AniListConnectionMode.CONNECTED) {
                    if (aniListId != null) {
                        TrackedItemPreview(
                            label = "AniList",
                            title = aniListTitle ?: "Linked",
                            imageUrl = aniListImageUrl,
                            onOpen = { uriHandler.openUri("https://anilist.co/anime/$aniListId") },
                            onChange = { searchMode = "AniList"; showSearchModal = true },
                            onUntrack = {
                                aniListId = null
                                scope.launch {
                                    com.nuvio.app.features.anilist.AnimeTrackerMappingStorage.removeAniListOverride(contentId)
                                }
                            },
                            onDelete = {
                                val currentId = aniListId
                                val currentEntryId = aniListEntryId
                                aniListId = null
                                scope.launch {
                                    com.nuvio.app.features.anilist.AnimeTrackerMappingStorage.removeAniListOverride(contentId)
                                    if (currentId != null && currentEntryId != null) {
                                        val token = AniListAuthRepository.getAccessToken()
                                        if (!token.isNullOrBlank()) {
                                            AniListApiClient.deleteEntry(token, currentEntryId)
                                        }
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
                        TrackedItemPreview(
                            label = "MyAnimeList",
                            title = malTitle ?: "Linked",
                            imageUrl = malImageUrl,
                            onOpen = { uriHandler.openUri("https://myanimelist.net/anime/$malId") },
                            onChange = { searchMode = "MAL"; showSearchModal = true },
                            onUntrack = {
                                malId = null
                                scope.launch {
                                    com.nuvio.app.features.anilist.AnimeTrackerMappingStorage.removeMalOverride(contentId)
                                }
                            },
                            onDelete = {
                                val currentId = malId
                                malId = null
                                scope.launch {
                                    com.nuvio.app.features.anilist.AnimeTrackerMappingStorage.removeMalOverride(contentId)
                                    if (currentId != null) {
                                        val token = MalAuthRepository.getAccessToken()
                                        if (!token.isNullOrBlank()) {
                                            MalApiClient.deleteEntry(token, currentId)
                                        }
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("AniList Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.weight(1f))
                        androidx.compose.material3.IconToggleButton(
                            checked = isAniListFavourite,
                            onCheckedChange = { checked ->
                                isAniListFavourite = checked
                                scope.launch {
                                    val token = com.nuvio.app.features.anilist.AniListAuthRepository.getAccessToken()
                                    if (!token.isNullOrBlank()) {
                                        val success = com.nuvio.app.features.anilist.AniListApiClient.toggleFavourite(token, aniListId!!)
                                        if (!success) {
                                            isAniListFavourite = !checked // Revert on failure
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isAniListFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favourite",
                                tint = if (isAniListFavourite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Text("Status", style = MaterialTheme.typography.labelLarge)
                    Box {
                        OutlinedButton(onClick = { aniListStatusExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(aniListStatus)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = aniListStatusExpanded, onDismissRequest = { aniListStatusExpanded = false }) {
                            aniListStatuses.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s) },
                                    onClick = { 
                                        aniListStatus = s
                                        if (s == "Completed" && maxEpisodes < 2000) {
                                            aniListProgress = maxEpisodes.toFloat()
                                        }
                                        aniListStatusExpanded = false 
                                    }
                                ) 
                            }
                        }
                    }

                    if (maxEpisodes > 0 && maxEpisodes != 2000) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Progress: ", style = MaterialTheme.typography.labelLarge)
                            if (showAniListInput) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = aniListInputText,
                                    onValueChange = { 
                                        aniListInputText = it
                                        it.toFloatOrNull()?.let { p -> aniListProgress = p.coerceIn(0f, maxEpisodes.toFloat()) } 
                                    },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onDone = { showAniListInput = false }
                                    ),
                                    textStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Min)
                                )
                            } else {
                                Text(
                                    text = aniListProgress.roundToInt().toString(),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    ),
                                    modifier = Modifier.clickable { 
                                        aniListInputText = aniListProgress.roundToInt().toString()
                                        showAniListInput = true 
                                    }
                                )
                            }
                            Text(
                                text = " / $maxEpisodes",
                                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { if (aniListProgress > 0) aniListProgress -= 1f }) { Text("-1") }
                            Slider(
                                value = aniListProgress, 
                                onValueChange = { aniListProgress = it.roundToInt().toFloat() }, 
                                valueRange = 0f..maxEpisodes.toFloat(),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            OutlinedButton(onClick = { aniListProgress += 1f }) { Text("+1") }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            OutlinedButton(onClick = { if (aniListProgress > 0) aniListProgress -= 1f }) { Text("-1") }
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            if (showAniListInput) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = aniListInputText,
                                    onValueChange = { 
                                        aniListInputText = it
                                        it.toFloatOrNull()?.let { p -> aniListProgress = p } 
                                    },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onDone = { showAniListInput = false }
                                    ),
                                    textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                                    modifier = Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Min)
                                )
                            } else {
                                Text(
                                    text = aniListProgress.roundToInt().toString(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    ),
                                    modifier = Modifier.clickable { 
                                        aniListInputText = aniListProgress.roundToInt().toString()
                                        showAniListInput = true 
                                    }
                                )
                            }
                            Text(
                                text = " / ?",
                                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )

                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedButton(onClick = { aniListProgress += 1f }) { Text("+1") }
                        }
                    }

                    val format = aniListState.scoreFormat
                    val scoreRange: ClosedFloatingPointRange<Float>
                    val scoreSteps: Int
                    val scoreText: String
                    when (format) {
                        "POINT_100" -> {
                            scoreRange = 0f..100f
                            scoreSteps = 99
                            scoreText = if (aniListScore == 0f) "Unrated" else "${aniListScore.roundToInt()}"
                        }
                        "POINT_10_DECIMAL" -> {
                            scoreRange = 0f..10f
                            scoreSteps = 99
                            val ds = (aniListScore * 10).roundToInt()
                            scoreText = if (aniListScore == 0f) "Unrated" else "${ds / 10}.${ds % 10}"
                        }
                        "POINT_5" -> {
                            scoreRange = 0f..5f
                            scoreSteps = 4
                            scoreText = if (aniListScore == 0f) "Unrated" else "${aniListScore.roundToInt()} Stars"
                        }
                        "POINT_3" -> {
                            scoreRange = 0f..3f
                            scoreSteps = 2
                            scoreText = if (aniListScore == 0f) "Unrated" else when(aniListScore.roundToInt()) {
                                1 -> ":("
                                2 -> ":|"
                                3 -> ":)"
                                else -> "Unrated"
                            }
                        }
                        else -> { // POINT_10 or null
                            scoreRange = 0f..10f
                            scoreSteps = 99
                            val ds = (aniListScore * 10).roundToInt()
                            scoreText = if (aniListScore == 0f) "Unrated" else "${ds / 10}.${ds % 10}"
                        }
                    }

                    Text("Score: $scoreText", style = MaterialTheme.typography.labelLarge)
                    Slider(value = aniListScore, onValueChange = { aniListScore = it }, valueRange = scoreRange, steps = scoreSteps)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                if (malState.mode == MalConnectionMode.CONNECTED && malId != null) {
                    Text("MyAnimeList Tracking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    Text("Status", style = MaterialTheme.typography.labelLarge)
                    Box {
                        OutlinedButton(onClick = { malStatusExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(malStatus)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = malStatusExpanded, onDismissRequest = { malStatusExpanded = false }) {
                            malStatuses.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s) },
                                    onClick = { 
                                        malStatus = s
                                        if (s == "Completed" && maxEpisodes < 2000) {
                                            malProgress = maxEpisodes.toFloat()
                                        }
                                        malStatusExpanded = false 
                                    }
                                ) 
                            }
                        }
                    }

                    if (maxEpisodes > 0 && maxEpisodes != 2000) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Progress: ", style = MaterialTheme.typography.labelLarge)
                            if (showMalInput) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = malInputText,
                                    onValueChange = { 
                                        malInputText = it
                                        it.toFloatOrNull()?.let { p -> malProgress = p.coerceIn(0f, maxEpisodes.toFloat()) } 
                                    },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onDone = { showMalInput = false }
                                    ),
                                    textStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Min)
                                )
                            } else {
                                Text(
                                    text = malProgress.roundToInt().toString(),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    ),
                                    modifier = Modifier.clickable { 
                                        malInputText = malProgress.roundToInt().toString()
                                        showMalInput = true 
                                    }
                                )
                            }
                            Text(
                                text = " / $maxEpisodes",
                                style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { if (malProgress > 0) malProgress -= 1f }) { Text("-1") }
                            Slider(
                                value = malProgress, 
                                onValueChange = { malProgress = it.roundToInt().toFloat() }, 
                                valueRange = 0f..maxEpisodes.toFloat(),
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                            )
                            OutlinedButton(onClick = { malProgress += 1f }) { Text("+1") }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            OutlinedButton(onClick = { if (malProgress > 0) malProgress -= 1f }) { Text("-1") }
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            if (showMalInput) {
                                androidx.compose.foundation.text.BasicTextField(
                                    value = malInputText,
                                    onValueChange = { 
                                        malInputText = it
                                        it.toFloatOrNull()?.let { p -> malProgress = p } 
                                    },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                    ),
                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                        onDone = { showMalInput = false }
                                    ),
                                    textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                                    modifier = Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Min)
                                )
                            } else {
                                Text(
                                    text = malProgress.roundToInt().toString(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                    ),
                                    modifier = Modifier.clickable { 
                                        malInputText = malProgress.roundToInt().toString()
                                        showMalInput = true 
                                    }
                                )
                            }
                            Text(
                                text = " / ?",
                                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )

                            Spacer(modifier = Modifier.width(16.dp))
                            OutlinedButton(onClick = { malProgress += 1f }) { Text("+1") }
                        }
                    }

                    Text("Score: ${if (malScore == 0f) "Unrated" else malScore.roundToInt().toString()}", style = MaterialTheme.typography.labelLarge)
                    Slider(value = malScore, onValueChange = { malScore = it.roundToInt().toFloat() }, valueRange = 0f..10f, steps = 9)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                }

                if (aniListId != null || malId != null) {
                // Dates
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
                            TextButton(onClick = { startDateMillis = null; startDatePickerState.selectedDateMillis = null }, modifier = Modifier.weight(1f)) {
                                Text("Clear Start Date")
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        if (finishDateMillis != null) {
                            TextButton(onClick = { finishDateMillis = null; finishDatePickerState.selectedDateMillis = null }, modifier = Modifier.weight(1f)) {
                                Text("Clear Finish Date")
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Total Rewatches
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

                // AniList Advanced
                if (aniListId != null && aniListState.mode == AniListConnectionMode.CONNECTED) {
                    var anilistExpanded by remember { mutableStateOf(false) }
                    ElevatedCard(modifier = Modifier.clickable { anilistExpanded = !anilistExpanded }.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("AniList Advanced Options", style = MaterialTheme.typography.titleSmall)
                            if (anilistExpanded) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = isPrivate,
                                        onClick = { isPrivate = !isPrivate },
                                        label = { Text("Private") }
                                    )
                                    FilterChip(
                                        selected = hideFromStatusLists,
                                        onClick = { hideFromStatusLists = !hideFromStatusLists },
                                        label = { Text("Hide from status lists") }
                                    )
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
                                        Text("$label: ${if(value == 0f) "Unrated" else displayVal}", style = MaterialTheme.typography.bodySmall)
                                        Slider(value = value, onValueChange = {
                                            when(i) {
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

                // MAL Advanced
                if (malId != null && malState.mode == MalConnectionMode.CONNECTED) {
                    var malExpanded by remember { mutableStateOf(false) }
                    ElevatedCard(modifier = Modifier.clickable { malExpanded = !malExpanded }.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("MyAnimeList Advanced Options", style = MaterialTheme.typography.titleSmall)
                            if (malExpanded) {
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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onDismiss() // Dismiss instantly
                        CoroutineScope(Dispatchers.Default).launch {
                            val startLd = startDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }
                            val finishLd = finishDateMillis?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date }

                            if (aniListId != null && aniListState.mode == AniListConnectionMode.CONNECTED) {
                                com.nuvio.app.features.anilist.AnimeTrackerMappingStorage.saveAniListOverride(contentId, aniListId!!)
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
                                    val advancedScoresList = listOf(storyScore, charScore, visualScore, audioScore, enjoymentScore)
                                        .map { (kotlin.math.round(it.toDouble() * 10.0) / 10.0) }
                                        
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
                                com.nuvio.app.features.anilist.AnimeTrackerMappingStorage.saveMalOverride(contentId, malId!!)
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
                ) {
                    Text("Save All")
                }
                }
            }
        }
    }
}
}

@Composable
private fun TrackedItemPreview(
    label: String,
    title: String,
    imageUrl: String?,
    onOpen: () -> Unit,
    onChange: () -> Unit,
    onUntrack: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        modifier = Modifier.size(width = 48.dp, height = 72.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    title, 
                    style = MaterialTheme.typography.titleMedium, 
                    maxLines = 2, 
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) { Text("Open") }
                TextButton(onClick = onChange, modifier = Modifier.weight(1f)) { Text("Change") }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onUntrack, modifier = Modifier.weight(1f)) { Text("Untrack") }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete from $label")
                }
            }
        }
    }
}

@Composable
private fun SearchItemCard(
    result: SearchResult,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            if (result.imageUrl != null) {
                AsyncImage(
                    model = result.imageUrl,
                    contentDescription = result.title,
                    modifier = Modifier.size(width = 80.dp, height = 120.dp),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(result.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (result.type != null) Text("Type: ${result.type}", style = MaterialTheme.typography.bodySmall)
                if (result.startDate != null) Text("Started: ${result.startDate}", style = MaterialTheme.typography.bodySmall)
                if (result.status != null) Text("Status: ${result.status}", style = MaterialTheme.typography.bodySmall)
                if (result.score != null) Text("Score: ${result.score}", style = MaterialTheme.typography.bodySmall)
                if (!result.description.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        // Remove HTML tags often present in Anilist descriptions
                        result.description.replace(Regex("<.*?>"), "").replace("\n", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
