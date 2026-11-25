package com.example.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppSettings
import com.example.model.LightCardConfig
import com.example.model.ThemeSettings
import com.example.network.ConnectivityTestResult
import com.example.security.CredentialCryptoManager
import com.example.ui.DashboardUiState
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.CoralOffline
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OffWhiteText
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SlateMuted
import java.util.Locale
import kotlin.math.roundToInt

private enum class SettingsTab(val title: String) {
    LIGHTS("Lights"),
    BLYNK_SYNC("Blynk & Sync"),
    THEME("Theme"),
    QUICK_TILES("Quick Tiles"),
}

@Composable
fun SettingsScreen(
    uiState: DashboardUiState,
    connectivityTestResult: ConnectivityTestResult?,
    isTestingConnectivity: Boolean,
    onBack: () -> Unit,
    onAddLight: (name: String, token: String, pin: String) -> Unit,
    onUpdateLight: (LightCardConfig) -> Unit,
    onDeleteLight: (String) -> Unit,
    onUpdateServerUrl: (String) -> Unit,
    onTestConnectivity: (String) -> Unit,
    onUpdateSyncIntervalMs: (Long) -> Unit,
    onUpdateTileAssignment: (Int, String?) -> Unit,
    onSelectBackgroundUri: (Uri, (Result<String>) -> Unit) -> Unit,
    onUpdateTheme: ((ThemeSettings) -> ThemeSettings) -> Unit,
    onClearBackground: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingLight by remember { mutableStateOf<LightCardConfig?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDeleteLight by remember { mutableStateOf<LightCardConfig?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("settings_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = OffWhiteText,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = OffWhiteText,
                )
            }

            val tabs = SettingsTab.entries
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AmberGlow,
                edgePadding = 16.dp,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AmberGlow,
                        )
                    }
                },
            ) {
                tabs.forEachIndexed { index, tab ->
                    val icon = when (tab) {
                        SettingsTab.LIGHTS -> Icons.Outlined.Lightbulb
                        SettingsTab.BLYNK_SYNC -> Icons.Rounded.Sync
                        SettingsTab.THEME -> Icons.Rounded.Palette
                        SettingsTab.QUICK_TILES -> Icons.Rounded.Widgets
                    }
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        selectedContentColor = AmberGlow,
                        unselectedContentColor = SlateMuted,
                        modifier = Modifier.testTag("settings_tab_${tab.name.lowercase(Locale.ROOT)}"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (tabs[selectedTab]) {
                SettingsTab.LIGHTS -> LightsManagementTab(
                    lights = uiState.lights,
                    onAddClick = { showAddDialog = true },
                    onEditClick = { editingLight = it },
                    onDeleteClick = { pendingDeleteLight = it },
                    modifier = Modifier.weight(1f),
                )

                SettingsTab.BLYNK_SYNC -> BlynkAndSyncTab(
                    currentServerUrl = uiState.settings.blynkServerUrl,
                    currentSyncMs = uiState.settings.syncIntervalMs,
                    connectivityTestResult = connectivityTestResult,
                    isTesting = isTestingConnectivity,
                    onSaveServerUrl = onUpdateServerUrl,
                    onTestConnectivity = onTestConnectivity,
                    onSyncIntervalChange = onUpdateSyncIntervalMs,
                    modifier = Modifier.weight(1f),
                )

                SettingsTab.THEME -> ThemeSettingsTab(
                    theme = uiState.settings.theme,
                    onSelectBackgroundUri = onSelectBackgroundUri,
                    onUpdateTheme = onUpdateTheme,
                    onClearBackground = onClearBackground,
                    modifier = Modifier.weight(1f),
                )

                SettingsTab.QUICK_TILES -> QuickTilesTab(
                    lights = uiState.lights,
                    assignments = uiState.settings.tileAssignments,
                    onAssignTile = onUpdateTileAssignment,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (showAddDialog) {
        LightEditorDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, token, pin ->
                onAddLight(name, token, pin)
                showAddDialog = false
            },
        )
    }

    editingLight?.let { light ->
        LightEditorDialog(
            initial = light,
            onDismiss = { editingLight = null },
            onConfirm = { name, token, pin ->
                onUpdateLight(light.copy(name = name, authToken = token, virtualPin = pin))
                editingLight = null
            },
        )
    }

    pendingDeleteLight?.let { light ->
        AlertDialog(
            onDismissRequest = { pendingDeleteLight = null },
            containerColor = ObsidianSurface,
            titleContentColor = OffWhiteText,
            textContentColor = SlateMuted,
            title = { Text("Delete \"${light.name}\"?") },
            text = {
                Text(
                    "This removes the light card and its encrypted Blynk AuthToken (${light.virtualPin}) from your device.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteLight(light.id)
                        pendingDeleteLight = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CoralOffline,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.testTag("confirm_delete_light_button"),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteLight = null }) {
                    Text("Cancel", color = OffWhiteText)
                }
            },
        )
    }
}

@Composable
private fun LightsManagementTab(
    lights: List<LightCardConfig>,
    onAddClick: () -> Unit,
    onEditClick: (LightCardConfig) -> Unit,
    onDeleteClick: (LightCardConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Light Cards (${lights.size})",
                        style = MaterialTheme.typography.titleLarge,
                        color = OffWhiteText,
                    )
                    Text(
                        text = "Tokens are encrypted at rest using Android Keystore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateMuted,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberGlow,
                        contentColor = Color(0xFF1F1400),
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("add_light_button"),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Light", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (lights.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = AmberGlow,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No light cards added yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = OffWhiteText,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap Add Light to bind an ESP32 Blynk AuthToken and Virtual Pin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SlateMuted,
                        )
                    }
                }
            }
        } else {
            items(lights, key = { it.id }) { light ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, ObsidianBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianSurfaceElevated),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lightbulb,
                                contentDescription = null,
                                tint = AmberGlow,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = light.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = OffWhiteText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${light.virtualPin}  •  ${CredentialCryptoManager.maskToken(light.authToken)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = SlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(
                            onClick = { onEditClick(light) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("edit_light_${light.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit ${light.name}",
                                tint = SkyAccent,
                            )
                        }
                        IconButton(
                            onClick = { onDeleteClick(light) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("delete_light_${light.id}"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete ${light.name}",
                                tint = CoralOffline,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BlynkAndSyncTab(
    currentServerUrl: String,
    currentSyncMs: Long,
    connectivityTestResult: ConnectivityTestResult?,
    isTesting: Boolean,
    onSaveServerUrl: (String) -> Unit,
    onTestConnectivity: (String) -> Unit,
    onSyncIntervalChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var serverText by remember(currentServerUrl) { mutableStateOf(currentServerUrl) }
    var syncSeconds by remember(currentSyncMs) {
        mutableFloatStateOf((currentSyncMs / 1000f).coerceIn(0.1f, 5.0f))
    }

    LaunchedEffect(currentSyncMs) {
        syncSeconds = (currentSyncMs / 1000f).coerceIn(0.1f, 5.0f)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDone,
                            contentDescription = null,
                            tint = SkyAccent,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Shared Blynk Cloud Server",
                            style = MaterialTheme.typography.titleLarge,
                            color = OffWhiteText,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Used by all light cards and Quick Settings tiles.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateMuted,
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = serverText,
                        onValueChange = { serverText = it },
                        label = { Text("Blynk Server Endpoint") },
                        placeholder = { Text("https://blynk.cloud") },
                        singleLine = true,
                        colors = darkFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_url_input"),
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Quick Region Presets",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateMuted,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        AppSettings.BLYNK_REGION_PRESETS.forEach { preset ->
                            val selected = serverText.trim().equals(preset, ignoreCase = true)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    serverText = preset
                                    onSaveServerUrl(preset)
                                },
                                label = {
                                    Text(
                                        text = preset.removePrefix("https://"),
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberGlow.copy(alpha = 0.22f),
                                    selectedLabelColor = AmberGlow,
                                    labelColor = SlateMuted,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = { onSaveServerUrl(serverText) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberGlow,
                                contentColor = Color(0xFF1F1400),
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("save_server_button"),
                        ) {
                            Text("Save Endpoint", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                onSaveServerUrl(serverText)
                                onTestConnectivity(serverText)
                            },
                            enabled = !isTesting,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, SkyAccent.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("test_connectivity_button"),
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = SkyAccent,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.NetworkCheck,
                                    contentDescription = null,
                                    tint = SkyAccent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Server", color = SkyAccent)
                        }
                    }

                    if (connectivityTestResult != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val isOk = connectivityTestResult.serverReachable
                        val color = if (isOk) EmeraldOnline else CoralOffline
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = color.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (isOk) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = connectivityTestResult.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OffWhiteText,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = AmberGlow,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Physical Switch Sync",
                                style = MaterialTheme.typography.titleLarge,
                                color = OffWhiteText,
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = AmberGlow.copy(alpha = 0.16f),
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1fs", syncSeconds),
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberGlow,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Detects ESP32 hardware switch changes while the app is open (0.1s to 5.0s). Pins sharing an AuthToken are batched into a single keep-alive HTTP request and pause automatically in the background.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateMuted,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = syncSeconds,
                        onValueChange = { raw ->
                            val snapped = (raw * 10f).roundToInt() / 10f
                            syncSeconds = snapped.coerceIn(0.1f, 5.0f)
                        },
                        onValueChangeFinished = {
                            onSyncIntervalChange((syncSeconds * 1000f).roundToInt().toLong())
                        },
                        valueRange = 0.1f..5.0f,
                        steps = 48,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberGlow,
                            activeTrackColor = AmberGlow,
                            inactiveTrackColor = ObsidianSurfaceElevated,
                        ),
                        modifier = Modifier.testTag("sync_interval_slider"),
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(100L to "0.1s", 500L to "0.5s", 1000L to "1.0s", 2000L to "2.0s", 5000L to "5.0s")
                            .forEach { (ms, label) ->
                                FilterChip(
                                    selected = currentSyncMs == ms,
                                    onClick = {
                                        syncSeconds = ms / 1000f
                                        onSyncIntervalChange(ms)
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberGlow.copy(alpha = 0.22f),
                                        selectedLabelColor = AmberGlow,
                                        labelColor = SlateMuted,
                                    ),
                                )
                            }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LightEditorDialog(
    initial: LightCardConfig?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, token: String, pin: String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var token by remember { mutableStateOf(initial?.authToken ?: "") }
    var pin by remember { mutableStateOf(initial?.virtualPin ?: "V0") }
    var showToken by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = OffWhiteText,
        textContentColor = SlateMuted,
        title = {
            Text(
                text = if (initial == null) "Add Light Card" else "Edit Light Card",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Light Name") },
                    placeholder = { Text("Living Room Ceiling") },
                    singleLine = true,
                    colors = darkFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("light_name_input"),
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Blynk AuthToken") },
                    placeholder = { Text("32-character device token") },
                    singleLine = true,
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                imageVector = if (showToken) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showToken) "Hide token" else "Show token",
                                tint = SlateMuted,
                            )
                        }
                    },
                    colors = darkFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("light_token_input"),
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Virtual Pin") },
                    placeholder = { Text("V0") },
                    singleLine = true,
                    colors = darkFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("light_pin_input"),
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf("V0", "V1", "V2", "V3", "V4", "V5").forEach { presetPin ->
                        FilterChip(
                            selected = pin.trim().equals(presetPin, ignoreCase = true),
                            onClick = { pin = presetPin },
                            label = { Text(presetPin, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AmberGlow.copy(alpha = 0.22f),
                                selectedLabelColor = AmberGlow,
                                labelColor = SlateMuted,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, token, pin) },
                enabled = name.isNotBlank() && token.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberGlow,
                    contentColor = Color(0xFF1F1400),
                ),
                modifier = Modifier.testTag("save_light_dialog_button"),
            ) {
                Text(if (initial == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = OffWhiteText)
            }
        },
    )
}

@Composable
private fun darkFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = OffWhiteText,
    unfocusedTextColor = OffWhiteText,
    focusedBorderColor = AmberGlow,
    unfocusedBorderColor = ObsidianBorder,
    focusedLabelColor = AmberGlow,
    unfocusedLabelColor = SlateMuted,
    cursorColor = AmberGlow,
)
