package com.example.ui.settings

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppSettings
import com.example.model.LightCardConfig
import com.example.model.ThemeSettings
import com.example.tiles.TileRegistry
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.BlurColorPresets
import com.example.ui.theme.CoralOffline
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.OffWhiteText
import com.example.ui.theme.SkyAccent
import com.example.ui.theme.SlateMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsTab(
    theme: ThemeSettings,
    onSelectBackgroundUri: (Uri, (Result<String>) -> Unit) -> Unit,
    onUpdateTheme: ((ThemeSettings) -> ThemeSettings) -> Unit,
    onClearBackground: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onSelectBackgroundUri(uri) { res ->
                    statusMessage = res.fold(
                        onSuccess = { "Background saved locally. Drag or pinch below to position." },
                        onFailure = { "Could not load photo: ${it.message}" },
                    )
                }
            }
        },
    )

    val previewBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = theme.backgroundImagePath,
    ) {
        val path = theme.backgroundImagePath
        value = if (!path.isNullOrBlank() && File(path).exists()) {
            withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(path)?.asImageBitmap()
            }
        } else {
            null
        }
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
                    Text(
                        text = "Home Screen Background",
                        style = MaterialTheme.typography.titleLarge,
                        color = OffWhiteText,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pick a photo from your gallery. Images are stored privately inside the app so they persist after reboots.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateMuted,
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberGlow,
                                contentColor = Color(0xFF1F1400),
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pick_background_button"),
                        ) {
                            Icon(Icons.Outlined.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Choose Photo", fontWeight = FontWeight.SemiBold)
                        }

                        if (theme.backgroundImagePath != null) {
                            OutlinedButton(
                                onClick = {
                                    onClearBackground()
                                    statusMessage = "Restored default smart-home theme."
                                },
                                border = BorderStroke(1.dp, CoralOffline.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = "Remove background",
                                    tint = CoralOffline,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset", color = CoralOffline)
                            }
                        }
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = statusMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyAccent,
                        )
                    }

                    if (previewBitmap != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Crop & Position (Pinch / Drag)",
                                style = MaterialTheme.typography.labelLarge,
                                color = OffWhiteText,
                            )
                            TextButton(
                                onClick = {
                                    onUpdateTheme {
                                        it.copy(scale = 1f, offsetXFraction = 0f, offsetYFraction = 0f)
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Rounded.RestartAlt,
                                    contentDescription = null,
                                    tint = SkyAccent,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Center", color = SkyAccent)
                            }
                        }

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth(0.62f)
                                .align(Alignment.CenterHorizontally)
                                .aspectRatio(9f / 16f)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.5.dp, AmberGlow.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                .background(Color.Black),
                        ) {
                            val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                            val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                            Image(
                                bitmap = previewBitmap!!,
                                contentDescription = "Crop preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            onUpdateTheme { cur ->
                                                val newScale = (cur.scale * zoom).coerceIn(1f, 3.5f)
                                                val maxOffset = (newScale - 1f) * 0.5f + 0.35f
                                                val newX = (cur.offsetXFraction + pan.x / wPx)
                                                    .coerceIn(-maxOffset, maxOffset)
                                                val newY = (cur.offsetYFraction + pan.y / hPx)
                                                    .coerceIn(-maxOffset, maxOffset)
                                                cur.copy(
                                                    scale = newScale,
                                                    offsetXFraction = newX,
                                                    offsetYFraction = newY,
                                                )
                                            }
                                        }
                                    }
                                    .graphicsLayer {
                                        scaleX = theme.scale
                                        scaleY = theme.scale
                                        translationX = theme.offsetXFraction * wPx
                                        translationY = theme.offsetYFraction * hPx
                                    },
                            )

                            // Live scrim preview indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Color(theme.blurColorArgb).copy(alpha = theme.overlayOpacity)
                                    ),
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Zoom: ${String.format("%.2fx", theme.scale)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SlateMuted,
                        )
                        Slider(
                            value = theme.scale,
                            onValueChange = { s ->
                                onUpdateTheme { it.copy(scale = s.coerceIn(1f, 3.5f)) }
                            },
                            valueRange = 1f..3.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = AmberGlow,
                                activeTrackColor = AmberGlow,
                                inactiveTrackColor = ObsidianSurfaceElevated,
                            ),
                        )
                    }
                }
            }
        }

        // Progressive Blur & Color Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Progressive Blur & Overlay",
                        style = MaterialTheme.typography.titleLarge,
                        color = OffWhiteText,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Softly blurs the top and bottom edges of your wallpaper while keeping status indicators and light cards legible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateMuted,
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Upper Area Blur (${theme.topBlurDp.roundToInt()} dp)",
                        style = MaterialTheme.typography.labelLarge,
                        color = OffWhiteText,
                    )
                    Slider(
                        value = theme.topBlurDp,
                        onValueChange = { v -> onUpdateTheme { it.copy(topBlurDp = v) } },
                        valueRange = 0f..40f,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberGlow,
                            activeTrackColor = AmberGlow,
                            inactiveTrackColor = ObsidianSurfaceElevated,
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Lower Area Blur (${theme.bottomBlurDp.roundToInt()} dp)",
                        style = MaterialTheme.typography.labelLarge,
                        color = OffWhiteText,
                    )
                    Slider(
                        value = theme.bottomBlurDp,
                        onValueChange = { v -> onUpdateTheme { it.copy(bottomBlurDp = v) } },
                        valueRange = 0f..40f,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberGlow,
                            activeTrackColor = AmberGlow,
                            inactiveTrackColor = ObsidianSurfaceElevated,
                        ),
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Readability Overlay (${(theme.overlayOpacity * 100).roundToInt()}%)",
                        style = MaterialTheme.typography.labelLarge,
                        color = OffWhiteText,
                    )
                    Slider(
                        value = theme.overlayOpacity,
                        onValueChange = { v -> onUpdateTheme { it.copy(overlayOpacity = v) } },
                        valueRange = 0f..0.8f,
                        colors = SliderDefaults.colors(
                            thumbColor = SkyAccent,
                            activeTrackColor = SkyAccent,
                            inactiveTrackColor = ObsidianSurfaceElevated,
                        ),
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Blur & Tint Color",
                        style = MaterialTheme.typography.labelLarge,
                        color = OffWhiteText,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BlurColorPresets.forEach { argb ->
                            val isSelected = theme.blurColorArgb == argb
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) AmberGlow else Color.White.copy(alpha = 0.25f),
                                        shape = CircleShape,
                                    )
                                    .clickable {
                                        onUpdateTheme { it.copy(blurColorArgb = argb) }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected color",
                                        tint = if (argb == 0xFFE2E8F0L) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickTilesTab(
    lights: List<LightCardConfig>,
    assignments: Map<Int, String>,
    onAssignTile: (Int, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var tileFeedback by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                            imageVector = Icons.Rounded.Widgets,
                            contentDescription = null,
                            tint = AmberGlow,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Android Quick Settings Tiles",
                            style = MaterialTheme.typography.titleLarge,
                            color = OffWhiteText,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toggle lights directly from your phone's notification shade without opening the app. Each tap reads the live pin and switches to the opposite state.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateMuted,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianSurfaceElevated,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (TileRegistry.canRequestAddTile()) {
                                "How to add: Tap \"Add Tile\" on any slot below, or swipe down Quick Settings twice -> tap the Edit (pencil) icon -> drag the light tile into your active panel."
                            } else {
                                "How to add: Swipe down your notification shade twice to open Quick Settings -> tap the Edit (pencil) icon -> scroll to find \"Light 1\"..\"Light 8\" and drag them into your active Quick Settings."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = OffWhiteText,
                            modifier = Modifier.padding(12.dp),
                        )
                    }

                    if (tileFeedback != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = tileFeedback.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyAccent,
                        )
                    }
                }
            }
        }

        items(AppSettings.QUICK_TILE_COUNT) { index ->
            val slot = index + 1
            val explicitId = assignments[slot]
            val resolvedLight = if (!explicitId.isNullOrBlank()) {
                lights.firstOrNull { it.id == explicitId }
            } else {
                lights.getOrNull(index)
            }
            var menuExpanded by remember { mutableStateOf(false) }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Quick Tile $slot",
                            style = MaterialTheme.typography.labelMedium,
                            color = SlateMuted,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box {
                            Text(
                                text = when {
                                    resolvedLight != null && explicitId != null ->
                                        "${resolvedLight.name} (${resolvedLight.virtualPin})"
                                    resolvedLight != null ->
                                        "Auto: ${resolvedLight.name} (${resolvedLight.virtualPin})"
                                    else -> "Unassigned"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = if (resolvedLight != null) OffWhiteText else SlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { menuExpanded = true }
                                    .padding(vertical = 4.dp),
                            )

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                containerColor = ObsidianSurfaceElevated,
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Auto (match light #$slot)", color = OffWhiteText) },
                                    onClick = {
                                        onAssignTile(slot, null)
                                        menuExpanded = false
                                    },
                                )
                                lights.forEach { light ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "${light.name} (${light.virtualPin})",
                                                color = OffWhiteText,
                                            )
                                        },
                                        onClick = {
                                            onAssignTile(slot, light.id)
                                            menuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { menuExpanded = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("Assign", color = OffWhiteText)
                    }

                    if (TileRegistry.canRequestAddTile() && resolvedLight != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                TileRegistry.requestAddTileToQuickSettings(
                                    context = context,
                                    slotIndex = slot,
                                    label = resolvedLight.name,
                                ) { msg -> tileFeedback = msg }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberGlow,
                                contentColor = Color(0xFF1F1400),
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.AddToHomeScreen,
                                contentDescription = "Add ${resolvedLight.name} tile",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
