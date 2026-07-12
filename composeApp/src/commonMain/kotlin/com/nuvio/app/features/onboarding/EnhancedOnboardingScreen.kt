package com.nuvio.app.features.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.PlatformBackHandler
import com.nuvio.app.core.ui.nuvio
import kotlinx.coroutines.delay
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.action_back
import nuvio.composeapp.generated.resources.action_next
import nuvio.composeapp.generated.resources.discord_mark
import nuvio.composeapp.generated.resources.nuvio_enhanced_concierge_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_concierge_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_live_tv_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_live_tv_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_community_body
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_community_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_developer_role
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_features_body
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_features_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_join_discord
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_members
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_start
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_team_body
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_team_note
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_team_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_welcome_body
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_welcome_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_release_digest_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_release_digest_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_smart_resume_desc
import nuvio.composeapp.generated.resources.nuvio_enhanced_smart_resume_title
import nuvio.composeapp.generated.resources.nuvio_enhanced_onboarding_logo
import nuvio.composeapp.generated.resources.onboarding_developer_russo
import nuvio.composeapp.generated.resources.onboarding_developer_yesnt
import nuvio.composeapp.generated.resources.settings_nuvio_enhanced_title
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val OnboardingPageCount = 4

@Composable
internal fun EnhancedOnboardingScreen(
    onJoinDiscord: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val tokens = MaterialTheme.nuvio

    PlatformBackHandler(enabled = true) {
        if (page > 0) page--
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.colors.background)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        tokens.colors.accent.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                    radius = 900f,
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            OnboardingProgress(
                currentPage = page,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            AnimatedContent(
                targetState = page,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val movingForward = targetState > initialState
                    (fadeIn(tween(360)) + slideInHorizontally(tween(420)) { width ->
                        if (movingForward) width / 5 else -width / 5
                    }).togetherWith(
                        fadeOut(tween(220)) + slideOutHorizontally(tween(320)) { width ->
                            if (movingForward) -width / 7 else width / 7
                        },
                    )
                },
                label = "enhanced_onboarding_page",
            ) { currentPage ->
                when (currentPage) {
                    0 -> WelcomePage()
                    1 -> FeaturesPage()
                    2 -> TeamPage()
                    else -> CommunityPage(onJoinDiscord = onJoinDiscord)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (page > 0) {
                    OutlinedButton(
                        onClick = { page-- },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(stringResource(Res.string.action_back))
                    }
                }
                Button(
                    onClick = {
                        if (page == OnboardingPageCount - 1) onComplete() else page++
                    },
                    modifier = Modifier.weight(if (page > 0) 1f else 2f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tokens.colors.accent,
                        contentColor = tokens.colors.onAccent,
                    ),
                ) {
                    Text(
                        text = stringResource(
                            if (page == OnboardingPageCount - 1) {
                                Res.string.nuvio_enhanced_onboarding_start
                            } else {
                                Res.string.action_next
                            },
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomePage() {
    val stage = rememberEntranceStage(3)
    OnboardingPageContainer {
        AnimatedVisibility(
            visible = stage >= 1,
            enter = fadeIn(tween(600)) + scaleIn(tween(650), initialScale = 0.78f),
        ) {
            Image(
                painter = painterResource(Res.drawable.nuvio_enhanced_onboarding_logo),
                contentDescription = null,
                modifier = Modifier.size(218.dp),
                contentScale = ContentScale.Fit,
            )
        }
        AnimatedVisibility(
            visible = stage >= 2,
            enter = fadeIn(tween(520)) + slideInVertically(tween(520)) { it / 3 },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(Res.string.settings_nuvio_enhanced_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.nuvio_enhanced_onboarding_welcome_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
        AnimatedVisibility(
            visible = stage >= 3,
            enter = fadeIn(tween(560)) + slideInVertically(tween(560)) { it / 2 },
        ) {
            Text(
                text = stringResource(Res.string.nuvio_enhanced_onboarding_welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FeaturesPage() {
    val stage = rememberEntranceStage(6)
    OnboardingPageContainer(topAligned = true) {
        OnboardingHeading(
            visible = stage >= 1,
            title = stringResource(Res.string.nuvio_enhanced_onboarding_features_title),
            body = stringResource(Res.string.nuvio_enhanced_onboarding_features_body),
        )
        FeatureRow(
            visible = stage >= 2,
            icon = Icons.Rounded.Home,
            title = Res.string.nuvio_enhanced_smart_resume_title,
            description = Res.string.nuvio_enhanced_smart_resume_desc,
        )
        FeatureRow(
            visible = stage >= 3,
            icon = Icons.Rounded.Star,
            title = Res.string.nuvio_enhanced_concierge_title,
            description = Res.string.nuvio_enhanced_concierge_desc,
        )
        FeatureRow(
            visible = stage >= 4,
            icon = Icons.Rounded.Notifications,
            title = Res.string.nuvio_enhanced_release_digest_title,
            description = Res.string.nuvio_enhanced_release_digest_desc,
        )
        FeatureRow(
            visible = stage >= 5,
            icon = Icons.Rounded.LiveTv,
            title = Res.string.nuvio_enhanced_live_tv_title,
            description = Res.string.nuvio_enhanced_live_tv_desc,
        )
    }
}

@Composable
private fun TeamPage() {
    val stage = rememberEntranceStage(5)
    OnboardingPageContainer(topAligned = true) {
        OnboardingHeading(
            visible = stage >= 1,
            title = stringResource(Res.string.nuvio_enhanced_onboarding_team_title),
            body = stringResource(Res.string.nuvio_enhanced_onboarding_team_body),
        )
        DeveloperCard(
            visible = stage >= 2,
            avatar = Res.drawable.onboarding_developer_russo,
            displayName = "Russo",
            handle = "@AKRusso",
        )
        DeveloperCard(
            visible = stage >= 3,
            avatar = Res.drawable.onboarding_developer_yesnt,
            displayName = "yesn't",
            handle = "@yesnt10",
        )
        AnimatedVisibility(
            visible = stage >= 4,
            enter = fadeIn(tween(450)) + slideInVertically(tween(450)) { it / 3 },
        ) {
            Text(
                text = stringResource(Res.string.nuvio_enhanced_onboarding_team_note),
                modifier = Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CommunityPage(onJoinDiscord: () -> Unit) {
    val stage = rememberEntranceStage(4)
    OnboardingPageContainer {
        AnimatedVisibility(
            visible = stage >= 1,
            enter = fadeIn(tween(520)) + scaleIn(tween(560), initialScale = 0.82f),
        ) {
            Surface(
                modifier = Modifier.size(92.dp),
                color = Color(0xFF5865F2),
                shape = RoundedCornerShape(8.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(Res.drawable.discord_mark),
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                    )
                }
            }
        }
        OnboardingHeading(
            visible = stage >= 2,
            title = stringResource(Res.string.nuvio_enhanced_onboarding_community_title),
            body = stringResource(Res.string.nuvio_enhanced_onboarding_community_body),
            centered = true,
        )
        AnimatedVisibility(
            visible = stage >= 3,
            enter = fadeIn(tween(480)) + slideInVertically(tween(480)) { it / 3 },
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFF5865F2).copy(alpha = 0.66f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF23A55A)),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.settings_nuvio_enhanced_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = stringResource(Res.string.nuvio_enhanced_onboarding_members),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = stage >= 4,
            enter = fadeIn(tween(480)) + slideInVertically(tween(480)) { it / 2 },
        ) {
            Button(
                onClick = onJoinDiscord,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5865F2),
                    contentColor = Color.White,
                ),
            ) {
                Image(
                    painter = painterResource(Res.drawable.discord_mark),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(Res.string.nuvio_enhanced_onboarding_join_discord),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContainer(
    topAligned: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = if (topAligned) 28.dp else 12.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (topAligned) {
            Arrangement.spacedBy(12.dp)
        } else {
            Arrangement.spacedBy(18.dp, Alignment.CenterVertically)
        },
        content = content,
    )
}

@Composable
private fun OnboardingHeading(
    visible: Boolean,
    title: String,
    body: String,
    centered: Boolean = false,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun FeatureRow(
    visible: Boolean,
    icon: ImageVector,
    title: StringResource,
    description: StringResource,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(440)) + slideInVertically(tween(440)) { it / 2 },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.17f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(23.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = stringResource(title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperCard(
    visible: Boolean,
    avatar: DrawableResource,
    displayName: String,
    handle: String,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(460)) + slideInVertically(tween(460)) { it / 2 },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E1F22),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box {
                    Image(
                        painter = painterResource(avatar),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E1F22))
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF23A55A)),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF2F3F5),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = handle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB5BAC1),
                    )
                    Surface(
                        color = Color(0xFF5865F2).copy(alpha = 0.22f),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.nuvio_enhanced_onboarding_developer_role),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB9BBFF),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingProgress(
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        repeat(OnboardingPageCount) { index ->
            Box(
                modifier = Modifier
                    .height(5.dp)
                    .width(if (index == currentPage) 26.dp else 7.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                        },
                    ),
            )
        }
    }
}

@Composable
private fun rememberEntranceStage(stageCount: Int): Int {
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(stageCount) {
        repeat(stageCount) { index ->
            delay(if (index == 0) 90 else 130)
            stage = index + 1
        }
    }
    return stage
}
