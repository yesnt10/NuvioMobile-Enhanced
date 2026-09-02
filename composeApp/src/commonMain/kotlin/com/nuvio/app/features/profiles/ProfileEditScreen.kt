package com.nuvio.app.features.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.auth.AuthRepository
import com.nuvio.app.core.auth.AuthState
import com.nuvio.app.core.ui.NuvioInputField
import com.nuvio.app.core.ui.NuvioPrimaryButton
import com.nuvio.app.core.ui.NuvioScreen
import com.nuvio.app.core.ui.NuvioScreenHeader
import com.nuvio.app.core.ui.NuvioStatusModal
import com.nuvio.app.core.ui.NuvioSurfaceCard
import com.nuvio.app.features.home.components.CollectionCardRemoteImage
import kotlinx.coroutines.launch
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreen(
    profile: NuvioProfile? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isNew = profile == null
    val scope = rememberCoroutineScope()
    val profileState by ProfileRepository.state.collectAsStateWithLifecycle()
    val currentProfile = remember(profile?.profileIndex, profileState.profiles, profile) {
        profile?.let { snapshot ->
            profileState.profiles.find { it.profileIndex == snapshot.profileIndex } ?: snapshot
        }
    }
    val fallbackColorHex = currentProfile?.avatarColorHex ?: PROFILE_COLORS.first()

    var name by rememberSaveable { mutableStateOf(currentProfile?.name ?: "") }
    var selectedAvatarId by rememberSaveable { mutableStateOf(currentProfile?.avatarId) }
    var avatarUrl by rememberSaveable { mutableStateOf(currentProfile?.avatarUrl.orEmpty()) }
    var backgroundUrl by rememberSaveable { mutableStateOf(currentProfile?.backgroundUrl.orEmpty()) }
    var usesPrimaryAddons by rememberSaveable { mutableStateOf(currentProfile?.usesPrimaryAddons ?: false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveErrorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showPinClear by remember { mutableStateOf(false) }
    var showGifSearch by remember { mutableStateOf(false) }
    val authState by AuthRepository.state.collectAsStateWithLifecycle()

    val avatars by AvatarRepository.avatars.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        AvatarRepository.fetchAvatars()
        AvatarRepository.refreshAvatars()
    }
    LaunchedEffect(isNew, avatars, selectedAvatarId, avatarUrl, currentProfile?.avatarId, currentProfile?.avatarUrl) {
        if (
            avatarUrl.isBlank() &&
            selectedAvatarId == null &&
            avatars.isNotEmpty() &&
            (isNew || !currentProfile?.avatarUrl.isNullOrBlank())
        ) {
            selectedAvatarId = currentProfile
                ?.avatarId
                ?.takeIf { avatarId -> avatars.any { it.id == avatarId } }
                ?: avatars.first().id
        }
    }

    val customAvatarUrl = remember(avatarUrl) { normalizedAvatarUrl(avatarUrl) }
    val avatarUrlIsInvalid = avatarUrl.isNotBlank() && customAvatarUrl == null
    val customBackgroundUrl = remember(backgroundUrl) { normalizedProfileBackgroundUrl(backgroundUrl) }
    val backgroundUrlIsInvalid = backgroundUrl.isNotBlank() && customBackgroundUrl == null
    val genericSaveErrorMessage = stringResource(Res.string.profile_save_failed)
    val selectedAvatarItem = remember(selectedAvatarId, avatars) {
        selectedAvatarId?.let { id -> avatars.find { it.id == id } }
    }
    val visibleAvatarItem = if (customAvatarUrl == null) selectedAvatarItem else null
    val previewAccent = remember(visibleAvatarItem, fallbackColorHex) {
        parseHexColor(visibleAvatarItem?.bgColor ?: fallbackColorHex)
    }

    NuvioScreen(modifier = modifier) {
        stickyHeader {
            NuvioScreenHeader(
                title = if (isNew) {
                    stringResource(Res.string.profile_edit_add_title)
                } else {
                    stringResource(Res.string.profile_edit_edit_title)
                },
                onBack = onBack,
            )
        }

        item {
            ProfileIdentityCard(
                name = name,
                isNew = isNew,
                profileIndex = currentProfile?.profileIndex,
                usesPrimaryAddons = usesPrimaryAddons,
                onNameChange = { name = it },
                onUsesPrimaryAddonsChange = { usesPrimaryAddons = it },
                selectedAvatar = visibleAvatarItem,
                customAvatarUrl = customAvatarUrl,
                accentColor = previewAccent,
                hasAvatarChoices = avatars.isNotEmpty(),
            )
        }

        item {
            NuvioSurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(Res.string.profile_custom_avatar_url),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.profile_custom_avatar_url_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NuvioInputField(
                        value = avatarUrl,
                        onValueChange = { value ->
                            avatarUrl = value
                            if (value.isNotBlank()) {
                                selectedAvatarId = null
                            } else if (selectedAvatarId == null && avatars.isNotEmpty()) {
                                selectedAvatarId = currentProfile
                                    ?.avatarId
                                    ?.takeIf { avatarId -> avatars.any { it.id == avatarId } }
                                    ?: avatars.first().id
                            }
                        },
                        placeholder = stringResource(Res.string.profile_custom_avatar_url_placeholder),
                    )
                    Button(
                        onClick = { showGifSearch = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.profile_gif_search_button),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (avatarUrlIsInvalid) {
                        Text(
                            text = stringResource(Res.string.profile_avatar_url_invalid),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        item {
            NuvioSurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(Res.string.profile_custom_background_url),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.profile_custom_background_url_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NuvioInputField(
                        value = backgroundUrl,
                        onValueChange = { backgroundUrl = it },
                        placeholder = stringResource(Res.string.profile_custom_background_url_placeholder),
                    )
                    if (backgroundUrlIsInvalid) {
                        Text(
                            text = stringResource(Res.string.profile_background_url_invalid),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        item {
            NuvioSurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = stringResource(Res.string.profile_choose_avatar),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = selectedAvatarItem?.displayName
                            ?: if (avatars.isEmpty()) {
                                stringResource(Res.string.profile_loading_avatars)
                            } else {
                                stringResource(Res.string.profile_select_avatar)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (avatars.isNotEmpty()) {
                        val avatarSpacing = 10.dp
                        val minAvatarSize = 58.dp
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val columns = (((maxWidth + avatarSpacing) / (minAvatarSize + avatarSpacing)).toInt())
                                .coerceAtLeast(1)
                            val avatarSize = (maxWidth - avatarSpacing * (columns - 1)) / columns

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(avatarSpacing),
                                verticalArrangement = Arrangement.spacedBy(avatarSpacing),
                                maxItemsInEachRow = columns,
                            ) {
                                avatars.forEach { avatar ->
                                    AvatarChoiceItem(
                                        avatar = avatar,
                                        size = avatarSize,
                                        isSelected = customAvatarUrl == null && avatar.id == selectedAvatarId,
                                        onClick = {
                                            avatarUrl = ""
                                            selectedAvatarId = avatar.id
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isNew) {
            item {
                NuvioSurfaceCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = stringResource(Res.string.profile_security),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (currentProfile?.pinEnabled == true) {
                                stringResource(Res.string.profile_security_pin_enabled)
                            } else {
                                stringResource(Res.string.profile_security_pin_disabled)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (currentProfile?.pinEnabled == true) {
                            NuvioPrimaryButton(
                                text = stringResource(Res.string.profile_remove_pin_lock),
                                onClick = { showPinClear = true },
                            )
                        } else {
                            NuvioPrimaryButton(
                                text = stringResource(Res.string.profile_set_pin_lock),
                                onClick = { showPinSetup = true },
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            saveErrorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            NuvioPrimaryButton(
                text = if (isSaving) {
                    stringResource(Res.string.profile_saving)
                } else if (isNew) {
                    stringResource(Res.string.profile_create_profile)
                } else {
                    stringResource(Res.string.collections_editor_save_changes)
                },
                enabled = name.isNotBlank() && !avatarUrlIsInvalid && !backgroundUrlIsInvalid && !isSaving,
                onClick = {
                    saveErrorMessage = null
                    isSaving = true
                    scope.launch {
                        val avatarColorHex = visibleAvatarItem?.bgColor ?: fallbackColorHex
                        val result = if (isNew) {
                            ProfileRepository.createProfile(
                                name = name,
                                avatarColorHex = avatarColorHex,
                                avatarId = if (customAvatarUrl == null) selectedAvatarId else null,
                                avatarUrl = customAvatarUrl,
                                backgroundUrl = customBackgroundUrl,
                                usesPrimaryAddons = usesPrimaryAddons,
                            )
                        } else {
                            ProfileRepository.updateProfile(
                                profileIndex = currentProfile!!.profileIndex,
                                name = name,
                                avatarColorHex = avatarColorHex,
                                avatarId = if (customAvatarUrl == null) selectedAvatarId else null,
                                avatarUrl = customAvatarUrl,
                                backgroundUrl = customBackgroundUrl,
                                usesPrimaryAddons = usesPrimaryAddons,
                            )
                        }
                        isSaving = false
                        if (result.success) {
                            onSaved()
                        } else {
                            saveErrorMessage = result.message
                                ?: genericSaveErrorMessage
                        }
                    }
                },
            )
        }

        if (!isNew && (currentProfile?.profileIndex ?: 0) > 1) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(
                        text = stringResource(Res.string.profile_delete_title),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    NuvioStatusModal(
        title = stringResource(Res.string.profile_delete_title),
        message = stringResource(
            Res.string.profile_delete_confirm_message,
            currentProfile?.name.orEmpty(),
        ),
        isVisible = showDeleteConfirm,
        confirmText = stringResource(Res.string.action_delete),
        dismissText = stringResource(Res.string.action_cancel),
        onConfirm = {
            showDeleteConfirm = false
            scope.launch {
                currentProfile?.let { ProfileRepository.deleteProfile(it.profileIndex) }
                onBack()
            }
        },
        onDismiss = { showDeleteConfirm = false },
    )

    if (showPinSetup && currentProfile != null) {
        PinSetupDialog(
            profileIndex = currentProfile.profileIndex,
            hasExistingPin = currentProfile.pinEnabled,
            onDone = {
                showPinSetup = false
                scope.launch {
                    if (authState is AuthState.Authenticated) {
                        ProfileRepository.pullProfiles()
                    }
                }
            },
            onDismiss = { showPinSetup = false },
        )
    }

    if (showPinClear && currentProfile != null) {
        PinEntryDialog(
            profileName = stringResource(Res.string.profile_remove_pin_for, currentProfile.name),
            onVerify = { pin -> ProfileRepository.clearPin(currentProfile.profileIndex, pin) },
            onVerified = {
                showPinClear = false
            },
            onDismiss = {
                showPinClear = false
            },
        )
    }

    if (showGifSearch) {
        ProfileGifSearchDialog(
            onSelect = { item ->
                avatarUrl = item.gifUrl
                selectedAvatarId = null
                showGifSearch = false
            },
            onDismiss = { showGifSearch = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ProfileGifSearchDialog(
    onSelect: (ProfileGifSearchItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ProfileGifSearchItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val searchFailedMessage = stringResource(Res.string.profile_gif_search_error)
    val notConfiguredMessage = stringResource(Res.string.profile_gif_search_not_configured)

    fun runSearch() {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank() || isSearching) return
        isSearching = true
        errorMessage = null
        scope.launch {
            val result = ProfileGifSearchService.search(cleanQuery)
            isSearching = false
            result.fold(
                onSuccess = {
                    results = it
                    errorMessage = null
                },
                onFailure = { throwable ->
                    results = emptyList()
                    errorMessage = if (throwable is ProfileGifSearchNotConfiguredException) {
                        notConfiguredMessage
                    } else {
                        searchFailedMessage
                    }
                },
            )
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 680.dp)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.profile_gif_search_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(Res.string.profile_gif_search_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(Res.string.action_cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    NuvioInputField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = stringResource(Res.string.profile_gif_search_placeholder),
                    )
                    Button(
                        onClick = ::runSearch,
                        enabled = query.isNotBlank() && !isSearching,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSearching) {
                                stringResource(Res.string.profile_gif_search_loading)
                            } else {
                                stringResource(Res.string.profile_gif_search_submit)
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when {
                        !isSearching && results.isEmpty() && errorMessage == null -> {
                            ProfileGifSearchEmptyState(
                                text = if (query.isBlank()) {
                                    stringResource(Res.string.profile_gif_search_empty)
                                } else {
                                    stringResource(Res.string.profile_gif_search_no_results)
                                },
                            )
                        }

                        results.isNotEmpty() -> {
                            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                val spacing = 10.dp
                                val itemWidth = (maxWidth - spacing) / 2
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(spacing),
                                    verticalArrangement = Arrangement.spacedBy(spacing),
                                    maxItemsInEachRow = 2,
                                ) {
                                    results.forEach { item ->
                                        ProfileGifResultCard(
                                            item = item,
                                            modifier = Modifier.width(itemWidth),
                                            onClick = { onSelect(item) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.profile_gif_search_powered_by),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(Res.string.action_cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileGifResultCard(
    item: ProfileGifSearchItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CollectionCardRemoteImage(
                imageUrl = item.previewUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                animateIfPossible = false,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.46f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ProfileGifSearchEmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 18.dp),
        )
    }
}

@Composable
private fun ProfileIdentityCard(
    name: String,
    isNew: Boolean,
    profileIndex: Int?,
    usesPrimaryAddons: Boolean,
    onNameChange: (String) -> Unit,
    onUsesPrimaryAddonsChange: (Boolean) -> Unit,
    selectedAvatar: AvatarCatalogItem?,
    customAvatarUrl: String?,
    accentColor: Color,
    hasAvatarChoices: Boolean,
) {
    NuvioSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(
                            if (selectedAvatar != null || customAvatarUrl != null) {
                                accentColor
                            } else {
                                accentColor.copy(alpha = 0.18f)
                            },
                        )
                        .border(
                            width = 2.dp,
                            color = if (selectedAvatar == null && customAvatarUrl == null) {
                                accentColor.copy(alpha = 0.35f)
                            } else {
                                Color.Transparent
                            },
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (customAvatarUrl != null) {
                        CollectionCardRemoteImage(
                            imageUrl = customAvatarUrl,
                            contentDescription = name,
                            modifier = Modifier.size(88.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            animateIfPossible = true,
                        )
                    } else if (selectedAvatar != null) {
                        CollectionCardRemoteImage(
                            imageUrl = avatarStorageUrl(selectedAvatar.storagePath),
                            contentDescription = selectedAvatar.displayName,
                            modifier = Modifier.size(88.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            animateIfPossible = true,
                        )
                    } else if (name.isNotBlank()) {
                        Text(
                            text = name.take(1).uppercase(),
                            style = MaterialTheme.typography.displayLarge,
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = name.ifBlank {
                            if (isNew) stringResource(Res.string.profile_new)
                            else stringResource(Res.string.profile_unnamed)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = listOf(
                            if (isNew) {
                                stringResource(Res.string.profile_new)
                            } else {
                                profileIndex?.let { stringResource(Res.string.profile_label_number, it) }
                                    ?: stringResource(Res.string.profile_unnamed)
                            },
                            if (usesPrimaryAddons) {
                                stringResource(Res.string.profile_primary_addons_on)
                            } else {
                                stringResource(Res.string.profile_primary_addons_off)
                            },
                        ).joinToString("  |  "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = when {
                            customAvatarUrl != null -> stringResource(Res.string.profile_custom_avatar_selected)
                            selectedAvatar != null -> stringResource(
                                Res.string.profile_avatar_selected,
                                selectedAvatar.displayName,
                            )
                            hasAvatarChoices -> stringResource(Res.string.profile_choose_avatar_below)
                            else -> stringResource(Res.string.profile_avatar_options_pending)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            NuvioInputField(
                value = name,
                onValueChange = onNameChange,
                placeholder = stringResource(Res.string.profile_name_placeholder),
            )

            ProfileOptionRow(
                title = stringResource(Res.string.profile_use_primary_addons),
                description = stringResource(Res.string.profile_use_primary_addons_description),
                checked = usesPrimaryAddons,
                onCheckedChange = onUsesPrimaryAddonsChange,
            )
        }
    }
}

@Composable
private fun AvatarChoiceItem(
    avatar: AvatarCatalogItem,
    size: androidx.compose.ui.unit.Dp,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                avatar.bgColor?.let(::parseHexColor)
                    ?: MaterialTheme.colorScheme.surfaceVariant,
            )
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CollectionCardRemoteImage(
            imageUrl = avatarStorageUrl(avatar.storagePath),
            contentDescription = avatar.displayName,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            contentScale = ContentScale.Crop,
            animateIfPossible = true,
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun ProfileOptionRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
    }
}

@Composable
fun PinSetupDialog(
    profileIndex: Int,
    hasExistingPin: Boolean,
    onDone: () -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableStateOf(if (hasExistingPin) "current" else "new") }
    var currentPin by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    when (step) {
        "current" -> PinEntryDialog(
            profileName = stringResource(Res.string.profile_enter_current_pin),
            onVerify = { pin -> ProfileRepository.verifyPin(profileIndex, pin) },
            onVerified = { pin ->
                currentPin = pin
                step = "new"
            },
            onDismiss = onDismiss,
        )

        "new" -> PinEntryDialog(
            profileName = stringResource(Res.string.profile_enter_new_pin),
            onVerify = { pin ->
                ProfileRepository.setPin(
                    profileIndex = profileIndex,
                    pin = pin,
                    currentPin = currentPin.ifEmpty { null },
                )
            },
            onVerified = {
                onDone()
            },
            onDismiss = onDismiss,
        )
    }
}
