package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldConnected
import com.example.ui.theme.RoseDisconnected
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: LightViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showSettingsScreen by remember { mutableStateOf(false) }

    val light1On by viewModel.light1State.collectAsState()
    val light2On by viewModel.light2State.collectAsState()
    val isConnected by viewModel.isDeviceConnected.collectAsState()
    val lastLatency by viewModel.lastLatencyMs.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastPhysicalEvent by viewModel.lastPhysicalEventText.collectAsState()

    val light1Pin by viewModel.light1Pin.collectAsState()
    val light2Pin by viewModel.light2Pin.collectAsState()
    val light1Name by viewModel.light1Name.collectAsState()
    val light2Name by viewModel.light2Name.collectAsState()

    val bgImageUri by viewModel.bgImageUri.collectAsState()
    val blurFraction by viewModel.bgBlurFraction.collectAsState()
    val cropOffsetY by viewModel.bgCropOffsetY.collectAsState()

    val notification by viewModel.notification.collectAsState()

    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.clearNotification()
        }
    }

    if (showSettingsScreen) {
        BackHandler { showSettingsScreen = false }
        SettingsScreen(
            viewModel = viewModel,
            onClose = { showSettingsScreen = false }
        )
        return
    }

    val surfaceBg = MaterialTheme.colorScheme.background

    // Entire Screen: 9:16 Background Theme with full-height fluid layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceBg)
    ) {
        // =========================================================================
        // BACKGROUND THEME LAYER: TỈ LỆ 9:16 CHUẨN ĐIỆN THOẠI
        // Giữ trọn cơ chế đổi theme, crop offset Y và làm mờ dần 3/4 ảnh xuống dưới
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .align(Alignment.TopCenter)
        ) {
            val imgModifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, cropOffsetY.roundToInt()) }

            if (bgImageUri.isNotBlank()) {
                AsyncImage(
                    model = bgImageUri,
                    contentDescription = "Ảnh nền theme 9:16",
                    contentScale = ContentScale.Crop,
                    modifier = imgModifier
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.home_illustration_1791113385242),
                    contentDescription = "Ảnh nền theme 9:16 mặc định",
                    contentScale = ContentScale.Crop,
                    modifier = imgModifier
                )
            }

            // Smooth blur & fade gradient starting at 3/4 (mặc định 75%)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                (blurFraction * 0.6f).coerceIn(0f, 1f) to Color.Transparent,
                                (blurFraction * 0.85f).coerceIn(0f, 1f) to surfaceBg.copy(alpha = 0.6f),
                                blurFraction.coerceIn(0f, 1f) to surfaceBg.copy(alpha = 0.9f),
                                1.0f to surfaceBg
                            )
                        )
                    )
            )
        }

        // =========================================================================
        // FOREGROUND CONTENT LAYER: Redesigned Home Screen UI
        // =========================================================================
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Glowing Bulb App Brand Icon
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AmberPrimary.copy(alpha = 0.2f),
                                border = BorderStroke(1.5.dp, AmberPrimary),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Blynk Light",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )

                                // Connection Status Pill with Ping
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (isConnected) {
                                                    true -> EmeraldConnected
                                                    false -> RoseDisconnected
                                                    null -> Color.Gray
                                                }
                                            )
                                    )
                                    Text(
                                        text = when (isConnected) {
                                            true -> "ESP32 Online"
                                            false -> "ESP32 Offline"
                                            null -> "Chưa kết nối"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = when (isConnected) {
                                            true -> EmeraldConnected
                                            false -> RoseDisconnected
                                            null -> Color.Gray
                                        }
                                    )
                                    if (lastLatency != null && lastLatency!! > 0) {
                                        Text(
                                            text = "• ${lastLatency}ms",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    },
                    actions = {
                        // Refresh Button with Spin Animation
                        val infiniteTransition = rememberInfiniteTransition(label = "sync_rotate")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotate_angle"
                        )
                        IconButton(
                            onClick = { viewModel.syncAll() },
                            enabled = !isSyncing,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("sync_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Đồng bộ",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = if (isSyncing) Modifier.rotate(rotation) else Modifier
                            )
                        }

                        // Settings Gear Button
                        IconButton(
                            onClick = { showSettingsScreen = true },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("settings_button")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Cài đặt",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Active Lights Status Strip
                val activeCount = (if (light1On) 1 else 0) + (if (light2On) 1 else 0)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (activeCount > 0) AmberPrimary.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = if (activeCount > 0) AmberPrimary else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = if (activeCount > 0) "$activeCount / 2 Đèn Đang Bật" else "Tất Cả Đèn Đang Tắt",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Virtual Pin: ${light1Pin.uppercase()} • ${light2Pin.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.LightGray
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (activeCount > 0) AmberPrimary else Color.DarkGray
                        ) {
                            Text(
                                text = if (activeCount > 0) "ACTIVE" else "STANDBY",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (activeCount > 0) Color.Black else Color.LightGray
                            )
                        }
                    }
                }

                // Physical Switch Event Banner (Hiển thị khi công tắc tường vật lý được bấm)
                AnimatedVisibility(
                    visible = lastPhysicalEvent != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    lastPhysicalEvent?.let { eventText ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = AmberPrimary.copy(alpha = 0.18f)),
                            border = BorderStroke(1.5.dp, AmberPrimary)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = eventText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // REDESIGNED LIGHT CONTROL CARDS
                // ==========================================
                Text(
                    text = "ĐIỀU KHIỂN THIẾT BỊ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )

                // Light 1 Control Card
                ModernLightCard(
                    lightNumber = 1,
                    lightName = light1Name,
                    pinName = light1Pin,
                    isOn = light1On,
                    onToggle = { viewModel.toggleLight(1) },
                    accentColor = AmberPrimary,
                    glowColor = AmberGlow
                )

                // Light 2 Control Card
                ModernLightCard(
                    lightNumber = 2,
                    lightName = light2Name,
                    pinName = light2Pin,
                    isOn = light2On,
                    onToggle = { viewModel.toggleLight(2) },
                    accentColor = CyanAccent,
                    glowColor = Color(0xFF67E8F9)
                )

                // Bottom Quick Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!light1On) viewModel.toggleLight(1)
                            if (!light2On) viewModel.toggleLight(2)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("turn_all_on_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AmberPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bật Tất Cả", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            if (light1On) viewModel.toggleLight(1)
                            if (light2On) viewModel.toggleLight(2)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("turn_all_off_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tắt Tất Cả", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Premium Modern Light Control Card with glowing bulb animation and tactile switch.
 */
@Composable
fun ModernLightCard(
    lightNumber: Int,
    lightName: String,
    pinName: String,
    isOn: Boolean,
    onToggle: () -> Unit,
    accentColor: Color,
    glowColor: Color
) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (isOn) 1.015f else 1.0f,
        animationSpec = tween(220),
        label = "bulb_card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleAnim)
            .shadow(
                elevation = if (isOn) 14.dp else 3.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = if (isOn) glowColor else Color.Black,
                spotColor = if (isOn) glowColor else Color.Black
            )
            .clickable(
                interactionSource = null,
                indication = ripple(bounded = true, color = accentColor)
            ) { onToggle() }
            .testTag("light_card_$lightNumber"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOn) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
            }
        ),
        border = BorderStroke(
            width = if (isOn) 2.dp else 1.dp,
            color = if (isOn) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bulb Icon with glowing ambient circular frame
                Surface(
                    shape = CircleShape,
                    color = if (isOn) accentColor.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(
                        width = if (isOn) 2.dp else 1.dp,
                        color = if (isOn) accentColor else Color.Transparent
                    ),
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isOn) Icons.Default.Lightbulb else Icons.Outlined.Lightbulb,
                            contentDescription = lightName,
                            tint = if (isOn) accentColor else Color.Gray,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = lightName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = pinName.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isOn) accentColor.copy(alpha = 0.18f) else Color.Transparent
                        ) {
                            Text(
                                text = if (isOn) "ĐANG BẬT" else "ĐANG TẮT",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOn) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isOn) FontWeight.ExtraBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Material 3 Switch with custom colors
            Switch(
                checked = isOn,
                onCheckedChange = { onToggle() },
                modifier = Modifier.testTag("switch_light_$lightNumber"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}
