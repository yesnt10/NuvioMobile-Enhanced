package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.nuvio.app.features.telegram.TelegramAuthorizationMode
import com.nuvio.app.features.telegram.TelegramRepository
import com.nuvio.app.features.telegram.TelegramUiState
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.telegram_cache_clear
import nuvio.composeapp.generated.resources.telegram_cache_title
import nuvio.composeapp.generated.resources.telegram_code_label
import nuvio.composeapp.generated.resources.telegram_connect
import nuvio.composeapp.generated.resources.telegram_connected_as
import nuvio.composeapp.generated.resources.telegram_disconnect
import nuvio.composeapp.generated.resources.telegram_email_code_label
import nuvio.composeapp.generated.resources.telegram_email_label
import nuvio.composeapp.generated.resources.telegram_missing_credentials
import nuvio.composeapp.generated.resources.telegram_password_label
import nuvio.composeapp.generated.resources.telegram_phone_label
import nuvio.composeapp.generated.resources.telegram_section_account
import nuvio.composeapp.generated.resources.telegram_section_storage
import nuvio.composeapp.generated.resources.telegram_starting
import nuvio.composeapp.generated.resources.telegram_unsupported
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.telegramSettingsContent(
    isTablet: Boolean,
    uiState: TelegramUiState,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.telegram_section_account),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                TelegramAccountCard(isTablet = isTablet, uiState = uiState)
            }
        }
    }

    if (uiState.isConnected) {
        item {
            SettingsSection(
                title = stringResource(Res.string.telegram_section_storage),
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    TelegramCacheCard(isTablet = isTablet, bytes = uiState.cacheSizeBytes)
                }
            }
        }
    }
}

@Composable
private fun TelegramAccountCard(
    isTablet: Boolean,
    uiState: TelegramUiState,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    var input by rememberSaveable(uiState.mode) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (uiState.mode) {
            TelegramAuthorizationMode.Ready -> {
                Text(
                    text = stringResource(
                        Res.string.telegram_connected_as,
                        uiState.displayName ?: uiState.username ?: "Telegram",
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Button(onClick = TelegramRepository::logOut, enabled = !uiState.isBusy) {
                    Text(stringResource(Res.string.telegram_disconnect))
                }
            }
            TelegramAuthorizationMode.PhoneNumber,
            TelegramAuthorizationMode.Code,
            TelegramAuthorizationMode.EmailAddress,
            TelegramAuthorizationMode.EmailCode,
            TelegramAuthorizationMode.Password -> {
                val isPassword = uiState.mode == TelegramAuthorizationMode.Password
                if (isPassword) {
                    SettingsSecretTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = stringResource(Res.string.telegram_password_label),
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.errorMessage != null,
                    )
                } else {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = uiState.errorMessage != null,
                        label = {
                            Text(
                                stringResource(
                                    when (uiState.mode) {
                                        TelegramAuthorizationMode.PhoneNumber -> Res.string.telegram_phone_label
                                        TelegramAuthorizationMode.EmailAddress -> Res.string.telegram_email_label
                                        TelegramAuthorizationMode.EmailCode -> Res.string.telegram_email_code_label
                                        else -> Res.string.telegram_code_label
                                    },
                                ),
                            )
                        },
                    )
                }
                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    enabled = input.isNotBlank() && !uiState.isBusy,
                    onClick = {
                        when (uiState.mode) {
                            TelegramAuthorizationMode.PhoneNumber -> TelegramRepository.submitPhoneNumber(input)
                            TelegramAuthorizationMode.Code -> TelegramRepository.submitCode(input)
                            TelegramAuthorizationMode.EmailAddress -> TelegramRepository.submitEmailAddress(input)
                            TelegramAuthorizationMode.EmailCode -> TelegramRepository.submitEmailCode(input)
                            TelegramAuthorizationMode.Password -> TelegramRepository.submitPassword(input)
                            else -> Unit
                        }
                    },
                ) {
                    Text(stringResource(Res.string.telegram_connect))
                }
            }
            TelegramAuthorizationMode.MissingCredentials -> TelegramInfoText(
                stringResource(Res.string.telegram_missing_credentials),
            )
            TelegramAuthorizationMode.Unsupported -> TelegramInfoText(
                stringResource(Res.string.telegram_unsupported),
            )
            else -> TelegramInfoText(
                uiState.errorMessage ?: stringResource(Res.string.telegram_starting),
            )
        }
    }
}

@Composable
private fun TelegramCacheCard(isTablet: Boolean, bytes: Long) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(stringResource(Res.string.telegram_cache_title), fontWeight = FontWeight.Medium)
            Text(
                text = formatBytes(bytes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = TelegramRepository::clearCache) {
            Text(stringResource(Res.string.telegram_cache_clear))
        }
    }
}

@Composable
private fun TelegramInfoText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "${oneDecimal(bytes / 1_073_741_824.0)} GB"
    bytes >= 1_048_576L -> "${oneDecimal(bytes / 1_048_576.0)} MB"
    bytes >= 1_024L -> "${oneDecimal(bytes / 1_024.0)} KB"
    else -> "$bytes B"
}

private fun oneDecimal(value: Double): String {
    val tenths = (value * 10.0).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}
