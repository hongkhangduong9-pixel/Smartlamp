package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.CyanAccent
import com.example.util.AppIconManager
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LightViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cài Đặt Hệ Thống",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Đóng")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Giao Diện & Icon", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Control Center", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Công Tắc Vật Lý", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Virtual Pin", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Kết Nối Blynk", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> ThemeAndIconSettingsTab(viewModel = viewModel, context = context)
                1 -> ControlCenterSettingsTab(viewModel = viewModel)
                2 -> PhysicalSwitchSettingsTab(viewModel = viewModel, context = context)
                3 -> VirtualPinSettingsTab(viewModel = viewModel)
                4 -> ConnectSettingsTab(viewModel = viewModel, context = context)
            }
        }
    }
}

/**
 * Tab 0: Giao diện Theme 9:16 & Đổi Icon chính của ứng dụng
 */
@Composable
fun ThemeAndIconSettingsTab(
    viewModel: LightViewModel,
    context: Context
) {
    val currentImageUri by viewModel.bgImageUri.collectAsState()
    val currentBlurFraction by viewModel.bgBlurFraction.collectAsState()
    val currentCropScale by viewModel.bgCropScale.collectAsState()
    val currentCropOffsetY by viewModel.bgCropOffsetY.collectAsState()
    val activeAppIcon by viewModel.activeAppIcon.collectAsState()

    var imageUri by remember(currentImageUri) { mutableStateOf(currentImageUri) }
    var blurFraction by remember(currentBlurFraction) { mutableStateOf(currentBlurFraction) }
    var cropScale by remember(currentCropScale) { mutableStateOf(currentCropScale) }
    var cropOffsetY by remember(currentCropOffsetY) { mutableStateOf(currentCropOffsetY) }

    val photoPickerLauncherBg = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri.toString()
            viewModel.saveBackgroundSettings(imageUri, blurFraction, cropScale, cropOffsetY)
            Toast.makeText(context, "Đã chọn ảnh nền theme 9:16 mới", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =========================================================================
        // SECTION 1: ĐỔI ICON CHÍNH ỨNG DỤNG (Không tạo icon phụ)
        // =========================================================================
        item {
            Text(
                text = "THAY ĐỔI BIỂU TƯỢNG ỨNG DỤNG CHÍNH (APP ICON)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Bấm vào kiểu icon bạn thích để đổi trực tiếp biểu tượng chính của app ngoài màn hình launcher điện thoại:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppIconManager.ICONS.forEach { opt ->
                    val isSelected = activeAppIcon == opt.key
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.changeAppIcon(opt.key) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(opt.previewColor),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = opt.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "ĐANG DÙNG",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = opt.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Đang chọn",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // SECTION 2: ẢNH NỀN THEME TỈ LỆ 9:16 (MÀN HÌNH CHÍNH)
        // =========================================================================
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "ẢNH NỀN THEME TỈ LỆ 9:16 (TOÀN MÀN HÌNH)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Ảnh nền chuẩn tỉ lệ 9:16 hiển thị chìm phía sau giao diện màn hình chính:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Live Preview Box in true 9:16 Vertical Smartphone Ratio
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(185.dp)
                        .aspectRatio(9f / 16f)
                        .shadow(16.dp, RoundedCornerShape(28.dp), spotColor = AmberPrimary),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val surfaceColor = MaterialTheme.colorScheme.surface
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(26.dp))
                        ) {
                            val previewMod = Modifier
                                .fillMaxSize()
                                .offset { IntOffset(0, cropOffsetY.roundToInt()) }

                            if (imageUri.isNotBlank()) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Xem trước nền dọc 9:16",
                                    contentScale = ContentScale.Crop,
                                    modifier = previewMod
                                )
                            } else {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(id = R.drawable.home_illustration_1791113385242),
                                    contentDescription = "Ảnh theme 9:16 mặc định",
                                    contentScale = ContentScale.Crop,
                                    modifier = previewMod
                                )
                            }
                        }

                        // Blur gradient 3/4
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Transparent,
                                            (blurFraction * 0.6f).coerceIn(0f, 1f) to Color.Transparent,
                                            (blurFraction * 0.85f).coerceIn(0f, 1f) to surfaceColor.copy(alpha = 0.55f),
                                            blurFraction.coerceIn(0f, 1f) to surfaceColor.copy(alpha = 0.85f),
                                            1.0f to surfaceColor
                                        )
                                    )
                                )
                        )

                        // Phone speaker notch
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Black.copy(alpha = 0.6f)
                        ) {
                            Box(modifier = Modifier.size(width = 44.dp, height = 5.dp))
                        }

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = "9:16 • Mờ ${(blurFraction * 100).toInt()}% (3/4)",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Actions: Đổi ảnh nền
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        photoPickerLauncherBg.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Đổi Ảnh Theme 9:16", fontSize = 13.sp)
                }

                if (imageUri.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            imageUri = ""
                            viewModel.saveBackgroundSettings("", blurFraction, cropScale, cropOffsetY)
                            Toast.makeText(context, "Đã đặt về ảnh theme gốc", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Mặc Định", fontSize = 13.sp)
                    }
                }
            }
        }

        // Section: Khả năng blurr dần từ 3/4 ảnh trở xuống
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vị trí bắt đầu làm mờ (Blur/Fade):",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if ((blurFraction * 100).roundToInt() == 75) "3/4 (75%)" else "${(blurFraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = blurFraction,
                        onValueChange = { blurFraction = it },
                        onValueChangeFinished = {
                            viewModel.saveBackgroundSettings(imageUri, blurFraction, cropScale, cropOffsetY)
                        },
                        valueRange = 0.4f..0.95f,
                        steps = 11
                    )
                }
            }
        }

        // Section: Khả năng crop ảnh & dịch chuyển
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Crop, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Căn chỉnh vị trí dọc (Pan Y):",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dịch chuyển:", style = MaterialTheme.typography.labelSmall)
                        Text("${cropOffsetY.roundToInt()} px", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    Slider(
                        value = cropOffsetY,
                        onValueChange = { cropOffsetY = it },
                        onValueChangeFinished = {
                            viewModel.saveBackgroundSettings(imageUri, blurFraction, cropScale, cropOffsetY)
                        },
                        valueRange = -180f..180f
                    )

                    OutlinedButton(
                        onClick = {
                            cropOffsetY = 0f
                            cropScale = 1.0f
                            viewModel.saveBackgroundSettings(imageUri, blurFraction, 1.0f, 0f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Căn Giữa (Reset)", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Cài đặt và thêm ô vào Control Center (Quick Settings)
 */
@Composable
fun ControlCenterSettingsTab(viewModel: LightViewModel) {
    val light1Name by viewModel.light1Name.collectAsState()
    val light2Name by viewModel.light2Name.collectAsState()
    val light1Pin by viewModel.light1Pin.collectAsState()
    val light2Pin by viewModel.light2Pin.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "THÊM VÀO THANH CONTROL CENTER / QUICK SETTINGS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Bật/tắt đèn siêu tốc trực tiếp từ thanh Cài đặt nhanh của điện thoại ngay cả khi khóa màn hình mà không cần mở app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Add Tile Buttons Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Thêm Phím Điều Khiển Vào Control Center:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Button Add Light 1 Tile
                    Button(
                        onClick = { viewModel.requestAddTile(1) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ Thêm $light1Name (${light1Pin.uppercase()}) Vào Control Center",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Button Add Light 2 Tile
                    Button(
                        onClick = { viewModel.requestAddTile(2) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ Thêm $light2Name (${light2Pin.uppercase()}) Vào Control Center",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Step by step guide card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Hướng Dẫn Kéo Thả Thủ Công:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Nếu điện thoại không tự thêm được qua nút bấm trên, bạn có thể thêm thủ công như sau:\n\n" +
                                "1. Vuốt từ mép trên cùng của màn hình điện thoại xuống 2 lần để mở toàn bộ thanh Quick Settings / Control Center.\n" +
                                "2. Nhấn vào biểu tượng cây bút chỉnh sửa ✏️ hoặc nút \"Sửa\" ở góc trên.\n" +
                                "3. Cuộn xuống phần ô chức năng có sẵn, tìm 2 ô \"Đèn 1\" và \"Đèn 2\" của ứng dụng Blynk Light.\n" +
                                "4. Giữ và kéo thả 2 ô này lên vị trí đầu tiên để điều khiển thuận tiện nhất!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

/**
 * Tab 2: Quản lý Công Tắc Vật Lý & Mã Nguồn ESP32
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhysicalSwitchSettingsTab(
    viewModel: LightViewModel,
    context: Context
) {
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsState()
    val autoSyncIntervalMs by viewModel.autoSyncIntervalMs.collectAsState()
    val notifyPhysicalSwitch by viewModel.notifyPhysicalSwitch.collectAsState()
    val lastPhysicalEvent by viewModel.lastPhysicalEventText.collectAsState()
    val token by viewModel.savedAuthToken.collectAsState()
    val templateId by viewModel.templateId.collectAsState()
    val templateName by viewModel.templateName.collectAsState()
    val pin1 by viewModel.light1Pin.collectAsState()
    val pin2 by viewModel.light2Pin.collectAsState()

    var enabled by remember(autoSyncEnabled) { mutableStateOf(autoSyncEnabled) }
    var intervalMs by remember(autoSyncIntervalMs) { mutableIntStateOf(autoSyncIntervalMs) }
    var notify by remember(notifyPhysicalSwitch) { mutableStateOf(notifyPhysicalSwitch) }

    val sampleEsp32Code = remember(token, templateId, templateName, pin1, pin2) {
        """
/*
   Blynk 2.0 - 2 Công Tắc Vật Lý (Physical Switches) & 2 Relay ESP32
   Đồng bộ 2 chiều tức thì với App & Quick Settings Tile
*/

#define BLYNK_TEMPLATE_ID "$templateId"
#define BLYNK_TEMPLATE_NAME "$templateName"
#define BLYNK_AUTH_TOKEN "${if (token.isNotBlank()) token else "YOUR_BLYNK_AUTH_TOKEN"}"

#include <WiFi.h>
#include <WiFiClient.h>
#include <BlynkSimpleEsp32.h>

char ssid[] = "YOUR_WIFI_NAME";
char pass[] = "YOUR_WIFI_PASSWORD";

// Chân GPIO kết nối Relay (Active LOW hoặc HIGH tùy module)
#define RELAY_1_PIN 26
#define RELAY_2_PIN 27

// Chân GPIO kết nối 2 công tắc vật lý (nối nút bấm xuống GND)
#define SWITCH_1_PIN 18
#define SWITCH_2_PIN 19

// Chân ảo Virtual Pin
#define VPIN_LIGHT_1 ${pin1.uppercase()}
#define VPIN_LIGHT_2 ${pin2.uppercase()}

int state1 = 0;
int state2 = 0;
int lastBtn1State = HIGH;
int lastBtn2State = HIGH;
unsigned long lastDebounce1 = 0;
unsigned long lastDebounce2 = 0;
const unsigned long debounceDelay = 50;

BlynkTimer timer;

// Nhận lệnh từ App hoặc Quick Settings Tile cho Đèn 1
BLYNK_WRITE(VPIN_LIGHT_1) {
  state1 = param.asInt();
  digitalWrite(RELAY_1_PIN, state1 ? LOW : HIGH); // Module Relay Active-Low
}

// Nhận lệnh từ App hoặc Quick Settings Tile cho Đèn 2
BLYNK_WRITE(VPIN_LIGHT_2) {
  state2 = param.asInt();
  digitalWrite(RELAY_2_PIN, state2 ? LOW : HIGH);
}

// Quét nút bấm vật lý để cập nhật lên Cloud và Relay
void checkPhysicalSwitches() {
  int reading1 = digitalRead(SWITCH_1_PIN);
  if (reading1 != lastBtn1State) {
    lastDebounce1 = millis();
  }
  if ((millis() - lastDebounce1) > debounceDelay) {
    if (reading1 == LOW && lastBtn1State == HIGH) {
      state1 = !state1;
      digitalWrite(RELAY_1_PIN, state1 ? LOW : HIGH);
      Blynk.virtualWrite(VPIN_LIGHT_1, state1); // Gửi cập nhật lên Blynk Cloud
    }
  }
  lastBtn1State = reading1;

  int reading2 = digitalRead(SWITCH_2_PIN);
  if (reading2 != lastBtn2State) {
    lastDebounce2 = millis();
  }
  if ((millis() - lastDebounce2) > debounceDelay) {
    if (reading2 == LOW && lastBtn2State == HIGH) {
      state2 = !state2;
      digitalWrite(RELAY_2_PIN, state2 ? LOW : HIGH);
      Blynk.virtualWrite(VPIN_LIGHT_2, state2); // Gửi cập nhật lên Blynk Cloud
    }
  }
  lastBtn2State = reading2;
}

void setup() {
  Serial.begin(115200);
  pinMode(RELAY_1_PIN, OUTPUT);
  pinMode(RELAY_2_PIN, OUTPUT);
  digitalWrite(RELAY_1_PIN, HIGH); // Mặc định tắt
  digitalWrite(RELAY_2_PIN, HIGH);

  pinMode(SWITCH_1_PIN, INPUT_PULLUP);
  pinMode(SWITCH_2_PIN, INPUT_PULLUP);

  Blynk.begin(BLYNK_AUTH_TOKEN, ssid, pass);
  timer.setInterval(100L, checkPhysicalSwitches);
}

void loop() {
  Blynk.run();
  timer.run();
}
        """.trimIndent()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "ĐỒNG BỘ CÔNG TẮC VẬT LÝ ESP32",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Khi bạn bật/tắt công tắc vật lý gắn trên tường hoặc mạch ESP32, ứng dụng sẽ tự động cập nhật trạng thái đèn theo thời gian thực.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Live status of last physical event
        if (lastPhysicalEvent != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberPrimary.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = AmberPrimary)
                        Text(
                            text = "Sự kiện gần nhất: $lastPhysicalEvent",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Auto Sync Toggle Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tự Động Đồng Bộ (Live Auto-Sync)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Liên tục nhận trạng thái khi công tắc vật lý thay đổi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enabled,
                            onCheckedChange = {
                                enabled = it
                                viewModel.saveAutoSyncSettings(enabled, intervalMs, notify)
                            }
                        )
                    }

                    if (enabled) {
                        Text(
                            text = "Tần số đồng bộ nền khi mở app:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                300 to "0.3s (Siêu Tốc ⚡)",
                                500 to "0.5s",
                                1000 to "1.0s",
                                2000 to "2.0s",
                                3000 to "3.0s"
                            ).forEach { (ms, label) ->
                                FilterChip(
                                    selected = intervalMs == ms,
                                    onClick = {
                                        intervalMs = ms
                                        viewModel.saveAutoSyncSettings(enabled, intervalMs, notify)
                                    },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = if (ms == 300) FontWeight.Bold else FontWeight.Normal) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Thông báo khi công tắc vật lý được bấm:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Switch(
                                checked = notify,
                                onCheckedChange = {
                                    notify = it
                                    viewModel.saveAutoSyncSettings(enabled, intervalMs, notify)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ESP32 Arduino Source Code Snippet
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = CyanAccent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mã Nguồn ESP32 Chuẩn (Arduino C++)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Mã này đã điền sẵn Template ID, Virtual Pin và xử lý chống dội phím (debounce) cho 2 công tắc vật lý.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("ESP32 Code", sampleEsp32Code))
                            Toast.makeText(context, "Đã sao chép mã nguồn ESP32 vào clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sao Chép Toàn Bộ Mã Nguồn ESP32", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: Edit virtual pin (gán cho nút V0, V1, V2, V3...)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VirtualPinSettingsTab(viewModel: LightViewModel) {
    val currentPin1 by viewModel.light1Pin.collectAsState()
    val currentPin2 by viewModel.light2Pin.collectAsState()
    val currentName1 by viewModel.light1Name.collectAsState()
    val currentName2 by viewModel.light2Name.collectAsState()

    var pin1 by remember(currentPin1) { mutableStateOf(currentPin1) }
    var pin2 by remember(currentPin2) { mutableStateOf(currentPin2) }
    var name1 by remember(currentName1) { mutableStateOf(currentName1) }
    var name2 by remember(currentName2) { mutableStateOf(currentName2) }

    val commonPins = listOf("v0", "v1", "v2", "v3", "v4", "v5", "v6", "v7", "v8")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "GÁN VIRTUAL PIN & TÊN HIỂN THỊ",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Tùy biến chân ảo tương ứng với firmware ESP32 của bạn. Quick Settings Tiles sẽ tự động cập nhật theo cấu hình này.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Light 1 Configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "💡 Cấu Hình Đèn Số 1",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = name1,
                        onValueChange = { name1 = it },
                        label = { Text("Tên hiển thị Đèn 1") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text(
                        text = "Chọn Virtual Pin cho Đèn 1: (Hiện tại: ${pin1.uppercase()})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonPins.forEach { p ->
                            FilterChip(
                                selected = pin1.equals(p, ignoreCase = true),
                                onClick = { pin1 = p },
                                label = { Text(p.uppercase(), fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = pin1,
                        onValueChange = { pin1 = it.lowercase().trim() },
                        label = { Text("Hoặc nhập Virtual Pin tùy ý (vd: v0, v12)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Light 2 Configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "💡 Cấu Hình Đèn Số 2",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = name2,
                        onValueChange = { name2 = it },
                        label = { Text("Tên hiển thị Đèn 2") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text(
                        text = "Chọn Virtual Pin cho Đèn 2: (Hiện tại: ${pin2.uppercase()})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonPins.forEach { p ->
                            FilterChip(
                                selected = pin2.equals(p, ignoreCase = true),
                                onClick = { pin2 = p },
                                label = { Text(p.uppercase(), fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = pin2,
                        onValueChange = { pin2 = it.lowercase().trim() },
                        label = { Text("Hoặc nhập Virtual Pin tùy ý (vd: v1, v13)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Save Button
        item {
            Button(
                onClick = {
                    viewModel.saveVirtualPins(pin1, name1, pin2, name2)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_pins_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lưu Virtual Pin & Cập Nhật Quick Settings")
            }
        }
    }
}

/**
 * Tab 4: Edit connect (chổ để nhập token, id, name của project blynk)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectSettingsTab(
    viewModel: LightViewModel,
    context: Context
) {
    val savedToken by viewModel.savedAuthToken.collectAsState()
    val savedServer by viewModel.savedServerHost.collectAsState()
    val savedTemplateId by viewModel.templateId.collectAsState()
    val savedTemplateName by viewModel.templateName.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()
    val isConnected by viewModel.isDeviceConnected.collectAsState()
    val lastLatency by viewModel.lastLatencyMs.collectAsState()

    var token by remember(savedToken) { mutableStateOf(savedToken) }
    var server by remember(savedServer) { mutableStateOf(savedServer) }
    var templateId by remember(savedTemplateId) { mutableStateOf(savedTemplateId) }
    var templateName by remember(savedTemplateName) { mutableStateOf(savedTemplateName) }
    var isTokenVisible by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "THÔNG TIN DỰ ÁN BLYNK CLOUD",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }

        // Connection Status Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isConnected == true) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    when (isConnected) {
                                        true -> Color(0xFF10B981)
                                        false -> Color(0xFFEF4444)
                                        null -> Color.Gray
                                    }
                                )
                        )
                        Text(
                            text = when (isConnected) {
                                true -> "ESP32 Đang Online"
                                false -> "ESP32 Offline (Chưa kết nối)"
                                null -> "Chưa kiểm tra kết nối"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (lastLatency != null && lastLatency!! > 0) {
                        Text(
                            text = "${lastLatency}ms",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Auth Token Input
        item {
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Blynk Device Auth Token") },
                placeholder = { Text("Nhập Auth Token từ Blynk Console") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_auth_token_input"),
                singleLine = true,
                visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                            Icon(
                                imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isTokenVisible) "Ẩn" else "Hiện"
                            )
                        }
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                token = clip.getItemAt(0).text?.toString().orEmpty().trim()
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Dán")
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Template ID Input
        item {
            OutlinedTextField(
                value = templateId,
                onValueChange = { templateId = it },
                label = { Text("BLYNK_TEMPLATE_ID") },
                placeholder = { Text("Mặc định: TMPL6mWdFodq6") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Template ID", templateId))
                        Toast.makeText(context, "Đã sao chép Template ID", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Template Name Input
        item {
            OutlinedTextField(
                value = templateName,
                onValueChange = { templateName = it },
                label = { Text("BLYNK_TEMPLATE_NAME") },
                placeholder = { Text("Mặc định: Đèn Thông Minh") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Server Host Chips
        item {
            Text(
                text = "Máy chủ Blynk Cloud:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("blynk.cloud", "sgp1.blynk.cloud", "fra1.blynk.cloud", "lon1.blynk.cloud").forEach { s ->
                    FilterChip(
                        selected = server.equals(s, ignoreCase = true),
                        onClick = { server = s },
                        label = { Text(s, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Save & Test Button
        item {
            Button(
                onClick = {
                    viewModel.saveConnectionSettings(token, server, templateId, templateName)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_connect_button"),
                shape = RoundedCornerShape(12.dp),
                enabled = !isTesting
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang kiểm tra kết nối...")
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lưu & Kiểm Tra Kết Nối")
                }
            }
        }
    }
}
