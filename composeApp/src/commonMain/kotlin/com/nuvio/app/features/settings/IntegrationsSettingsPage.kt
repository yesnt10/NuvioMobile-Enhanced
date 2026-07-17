package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nuvio.composeapp.generated.resources.compose_settings_page_debrid
import nuvio.composeapp.generated.resources.compose_settings_page_ai_assistant
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.logo_anilist
import nuvio.composeapp.generated.resources.logo_mal
import nuvio.composeapp.generated.resources.compose_settings_page_mdblist_ratings
import nuvio.composeapp.generated.resources.compose_settings_page_tmdb_enrichment
import nuvio.composeapp.generated.resources.compose_settings_page_trakt
import nuvio.composeapp.generated.resources.compose_settings_page_anilist
import nuvio.composeapp.generated.resources.compose_settings_page_mal
import nuvio.composeapp.generated.resources.compose_settings_root_trakt_description
import nuvio.composeapp.generated.resources.compose_settings_root_anilist_description
import nuvio.composeapp.generated.resources.compose_settings_root_mal_description
import nuvio.composeapp.generated.resources.settings_integrations_mdblist_description
import nuvio.composeapp.generated.resources.settings_integrations_ai_description
import nuvio.composeapp.generated.resources.settings_integrations_debrid_description
import nuvio.composeapp.generated.resources.settings_integrations_section_title
import nuvio.composeapp.generated.resources.settings_integrations_tmdb_description
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.integrationsContent(
    isTablet: Boolean,
    onTraktClick: () -> Unit,
    onAniListClick: () -> Unit,
    onMalClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onTmdbClick: () -> Unit,
    onMdbListClick: () -> Unit,
    onDebridClick: () -> Unit,
) {
    item {
        SettingsSection(
            title = stringResource(Res.string.settings_integrations_section_title),
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_ai_assistant),
                    description = stringResource(Res.string.settings_integrations_ai_description),
                    icon = Icons.Rounded.AutoAwesome,
                    isTablet = isTablet,
                    onClick = onAiAssistantClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_trakt),
                    description = stringResource(Res.string.compose_settings_root_trakt_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.Trakt),
                    isTablet = isTablet,
                    onClick = onTraktClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_tmdb_enrichment),
                    description = stringResource(Res.string.settings_integrations_tmdb_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.Tmdb),
                    isTablet = isTablet,
                    onClick = onTmdbClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_mdblist_ratings),
                    description = stringResource(Res.string.settings_integrations_mdblist_description),
                    iconPainter = integrationLogoPainter(IntegrationLogo.MdbList),
                    isTablet = isTablet,
                    onClick = onMdbListClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_debrid),
                    description = stringResource(Res.string.settings_integrations_debrid_description),
                    isTablet = isTablet,
                    onClick = onDebridClick,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        SettingsSection(
            title = "Tracking",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_anilist),
                    description = stringResource(Res.string.compose_settings_root_anilist_description),
                    iconPainter = painterResource(Res.drawable.logo_anilist),
                    isTablet = isTablet,
                    onClick = onAniListClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = stringResource(Res.string.compose_settings_page_mal),
                    description = stringResource(Res.string.compose_settings_root_mal_description),
                    iconPainter = painterResource(Res.drawable.logo_mal),
                    isTablet = isTablet,
                    onClick = onMalClick,
                )
            }
        }
    }
}
