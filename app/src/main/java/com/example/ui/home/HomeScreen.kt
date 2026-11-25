package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CloudStatus
import com.example.model.LightCardConfig
import com.example.model.LightPowerState
import com.example.model.LightRuntimeState
import com.example.ui.DashboardUiState
import com.example.ui.components.ThemeBackground
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberSurfaceTint
import com.example.ui.theme.AmberWarmLight
import com.example.ui.theme.CoralOffline
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OffWhiteText
import com.example.ui.theme.SlateMuted

@Composable
fun HomeScreen(
    uiState: DashboardUiState,
    onToggleLight: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onRefreshNow: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var timerInfoLightName by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        ThemeBackground(theme = uiState.settings.theme)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HomeStatusBar(
                cloudStatus = uiState.cloudStatus,
                onStatusClick = onRefreshNow,
                onSettingsClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )

            AnimatedVisibility(
                visible = uiState.errorBanner != null,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
            ) {
                ErrorStatusBanner(
                    message = uiState.errorBanner.orEmpty(),
                    onDismiss = onDismissError,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (uiState.lights.isEmpty()) {
                EmptyLightsView(
                    onAddLightClick = onOpenSettings,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .padding(24.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .testTag("lights_grid"),
                ) {
                    items(
                        items = uiState.lights,
                        key = { it.id },
                    ) { light ->
                        val runtime = uiState.runtimeStates[light.id] ?: LightRuntimeState()
                        LightControlCard(
                            config = light,
                            runtime = runtime,
                            onToggleClick = { onToggleLight(light.id) },
                            onTimerClick = { timerInfoLightName = light.name },
                        )
                    }
                }
            }
        }
    }

    if (timerInfoLightName != null) {
        AlertDialog(
            onDismissRequest = { timerInfoLightName = null },
            containerColor = ObsidianSurface,
            titleContentColor = OffWhiteText,
            textContentColor = SlateMuted,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = AmberGlow,
                )
            },
            title = {
                Text(
                    text = "${timerInfoLightName} Timer",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.timer_deferred_message),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            confirmButton = {
                TextButton(onClick = { timerInfoLightName = null }) {
                    Text("Got it", color = AmberGlow)
                }
            },
        )
    }
}

@Composable
private fun HomeStatusBar(
    cloudStatus: CloudStatus,
    onStatusClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = ObsidianSurface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: App icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 2.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_art),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp)),
                )
            }

            // Center: Blynk Cloud connection status pill (ONLINE / OFFLINE)
            val statusColor by animateColorAsState(
                targetValue = when (cloudStatus) {
                    CloudStatus.ONLINE -> EmeraldOnline
                    CloudStatus.OFFLINE -> CoralOffline
                    CloudStatus.CHECKING -> AmberGlow
                },
                label = "cloudStatusColor",
            )
            val statusLabel = when (cloudStatus) {
                CloudStatus.ONLINE -> stringResource(R.string.status_online)
                CloudStatus.OFFLINE -> stringResource(R.string.status_offline)
                CloudStatus.CHECKING -> "SYNCING"
            }

            Surface(
                onClick = onStatusClick,
                shape = CircleShape,
                color = statusColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("cloud_status_pill"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                    )
                    Icon(
                        imageVector = if (cloudStatus == CloudStatus.OFFLINE) {
                            Icons.Rounded.CloudOff
                        } else {
                            Icons.Rounded.Wifi
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Right: Settings icon
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("settings_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Settings",
                    tint = OffWhiteText,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
fun LightControlCard(
    config: LightCardConfig,
    runtime: LightRuntimeState,
    onToggleClick: () -> Unit,
    onTimerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOn = runtime.powerState == LightPowerState.ON
    val isUnknown = runtime.powerState == LightPowerState.UNKNOWN

    val containerColor by animateColorAsState(
        targetValue = if (isOn) {
            AmberSurfaceTint.copy(alpha = 0.86f)
        } else {
            ObsidianSurface.copy(alpha = 0.78f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardBg",
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            runtime.lastError != null -> CoralOffline.copy(alpha = 0.65f)
            isOn -> AmberGlow.copy(alpha = 0.62f)
            else -> Color.White.copy(alpha = 0.12f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardBorder",
    )

    val bulbColor by animateColorAsState(
        targetValue = when {
            isOn -> AmberWarmLight
            isUnknown -> SlateMuted.copy(alpha = 0.65f)
            else -> SlateMuted
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bulbColor",
    )

    val bulbScale by animateFloatAsState(
        targetValue = if (isOn) 1.10f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "bulbScale",
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isOn) 0.34f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "glowAlpha",
    )

    val cardShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(cardShape)
            .background(containerColor)
            .border(1.2.dp, borderColor, cardShape)
            .drawBehind {
                if (glowAlpha > 0.01f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AmberGlow.copy(alpha = glowAlpha),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = size.minDimension * 0.56f,
                        ),
                    )
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = AmberGlow),
                onClick = onToggleClick,
            )
            .padding(14.dp)
            .testTag("light_card_${config.id}"),
    ) {
        // Top-left: Light name + pin subtitle
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.84f),
        ) {
            Text(
                text = config.name,
                style = MaterialTheme.typography.titleMedium,
                color = OffWhiteText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when {
                    runtime.isToggling -> "${config.virtualPin} • ..."
                    isOn -> "${config.virtualPin} • ON"
                    runtime.powerState == LightPowerState.OFF -> "${config.virtualPin} • OFF"
                    else -> "${config.virtualPin} • --"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isOn) AmberWarmLight else SlateMuted,
            )
        }

        // Center: Light bulb icon
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center,
        ) {
            if (runtime.isToggling) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = AmberGlow.copy(alpha = 0.7f),
                    strokeWidth = 2.dp,
                )
            }
            Icon(
                imageVector = if (isOn) Icons.Filled.Lightbulb else Icons.Outlined.Lightbulb,
                contentDescription = "${config.name} light bulb",
                tint = bulbColor,
                modifier = Modifier
                    .size(46.dp)
                    .scale(bulbScale),
            )
        }

        // Bottom-right: Clock icon (isolated touch target, does NOT toggle the light)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(48.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true, color = OffWhiteText),
                    onClick = onTimerClick,
                )
                .testTag("timer_button_${config.id}"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = "Timer settings for ${config.name}",
                tint = if (isOn) AmberWarmLight.copy(alpha = 0.85f) else SlateMuted,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ErrorStatusBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CoralOffline.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, CoralOffline.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = CoralOffline,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OffWhiteText,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss error",
                    tint = OffWhiteText,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyLightsView(
    onAddLightClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ObsidianSurface.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AmberGlow.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = AmberGlow,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Smart Lights Configured",
                    style = MaterialTheme.typography.titleLarge,
                    color = OffWhiteText,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add your ESP32 Blynk AuthTokens and virtual pins in Settings to control your lights.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onAddLightClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGlow,
                        contentColor = Color(0xFF1F1400),
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("empty_configure_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure Lights", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
