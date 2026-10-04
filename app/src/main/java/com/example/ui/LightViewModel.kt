package com.example.ui

import android.app.Application
import android.app.StatusBarManager
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.BlynkPreferences
import com.example.data.BlynkRepository
import com.example.tile.Light1TileService
import com.example.tile.Light2TileService
import com.example.util.AppIconManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

data class UiNotification(
    val message: String,
    val isError: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class LightViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = BlynkPreferences.getInstance(application)
    private val repository = BlynkRepository.getInstance(application)

    val light1State: StateFlow<Boolean> = repository.light1State
    val light2State: StateFlow<Boolean> = repository.light2State
    val isDeviceConnected: StateFlow<Boolean?> = repository.isDeviceConnected
    val lastLatencyMs: StateFlow<Long?> = repository.lastLatencyMs
    val isSyncing: StateFlow<Boolean> = repository.isSyncing

    val light1Pin: StateFlow<String> = preferences.light1PinFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlynkPreferences.DEFAULT_PIN_1)

    val light2Pin: StateFlow<String> = preferences.light2PinFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlynkPreferences.DEFAULT_PIN_2)

    val light1Name: StateFlow<String> = preferences.light1NameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlynkPreferences.DEFAULT_NAME_1)

    val light2Name: StateFlow<String> = preferences.light2NameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlynkPreferences.DEFAULT_NAME_2)

    val templateId: StateFlow<String> = preferences.templateIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlynkPreferences.DEFAULT_TEMPLATE_ID)

    val templateName: StateFlow<String> = preferences.templateNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlynkPreferences.DEFAULT_TEMPLATE_NAME)

    val savedAuthToken: StateFlow<String> = preferences.authTokenFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val savedServerHost: StateFlow<String> = preferences.serverHostFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlynkPreferences.DEFAULT_SERVER)

    val bgImageUri: StateFlow<String> = preferences.bgImageUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val bgBlurFraction: StateFlow<Float> = preferences.bgBlurFractionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlynkPreferences.DEFAULT_BLUR_FRACTION)

    val bgCropScale: StateFlow<Float> = preferences.bgCropScaleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val bgCropOffsetY: StateFlow<Float> = preferences.bgCropOffsetYFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val bgAspectRatio: StateFlow<String> = preferences.bgAspectRatioFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "cover")

    val customIconUri: StateFlow<String> = preferences.customIconUriFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val themeMode: StateFlow<String> = preferences.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "dark")

    val autoSyncEnabled: StateFlow<Boolean> = preferences.autoSyncEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoSyncIntervalMs: StateFlow<Int> = preferences.autoSyncIntervalMsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BlynkPreferences.DEFAULT_AUTO_SYNC_INTERVAL_MS)

    val notifyPhysicalSwitch: StateFlow<Boolean> = preferences.notifyPhysicalSwitchFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val activeAppIcon: StateFlow<String> = preferences.activeAppIconFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "default")

    private val _notification = MutableStateFlow<UiNotification?>(null)
    val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

    private val _lastPhysicalEventText = MutableStateFlow<String?>(null)
    val lastPhysicalEventText: StateFlow<String?> = _lastPhysicalEventText.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private var autoSyncJob: Job? = null

    init {
        viewModelScope.launch {
            val token = preferences.getAuthToken()
            if (token.isNotBlank()) {
                repository.syncAll()
            }
        }
        startAutoSyncLoop()
    }

    /**
     * Live Polling loop to detect changes from physical wall switches on ESP32.
     */
    private fun startAutoSyncLoop() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch {
            while (isActive) {
                val isEnabled = autoSyncEnabled.value
                val intervalMs = autoSyncIntervalMs.value.coerceIn(200, 10000).toLong()

                if (isEnabled) {
                    val token = preferences.getAuthToken()
                    if (token.isNotBlank()) {
                        val prev1 = light1State.value
                        val prev2 = light2State.value

                        val result = repository.syncAll()
                        if (result.isSuccess) {
                            val curr1 = light1State.value
                            val curr2 = light2State.value

                            // Detect physical switch toggle
                            if (prev1 != curr1) {
                                val name = light1Name.value
                                val action = if (curr1) "BẬT" else "TẮT"
                                val eventMsg = "$name vừa được $action (từ công tắc vật lý)"
                                _lastPhysicalEventText.value = eventMsg
                                if (notifyPhysicalSwitch.value) {
                                    _notification.value = UiNotification(eventMsg)
                                }
                            }

                            if (prev2 != curr2) {
                                val name = light2Name.value
                                val action = if (curr2) "BẬT" else "TẮT"
                                val eventMsg = "$name vừa được $action (từ công tắc vật lý)"
                                _lastPhysicalEventText.value = eventMsg
                                if (notifyPhysicalSwitch.value) {
                                    _notification.value = UiNotification(eventMsg)
                                }
                            }
                        }
                    }
                }
                delay(intervalMs)
            }
        }
    }

    fun toggleLight(lightIndex: Int) {
        viewModelScope.launch {
            val currentState = if (lightIndex == 1) light1State.value else light2State.value
            val targetState = !currentState
            val result = repository.setLightState(lightIndex, targetState)
            if (result.isFailure) {
                _notification.value = UiNotification(
                    message = "Lỗi điều khiển: ${result.exceptionOrNull()?.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            val result = repository.syncAll()
            if (result.isSuccess) {
                _notification.value = UiNotification("Đã đồng bộ trạng thái thành công")
            } else {
                _notification.value = UiNotification(
                    message = "Lỗi đồng bộ: ${result.exceptionOrNull()?.localizedMessage}",
                    isError = true
                )
            }
        }
    }

    fun saveConnectionSettings(token: String, server: String, templateId: String, templateName: String) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            try {
                preferences.saveAuthToken(token)
                preferences.saveServerHost(server)
                preferences.saveTemplateInfo(templateId, templateName)

                if (token.isNotBlank()) {
                    val result = repository.testConnection(server, token)
                    if (result.isSuccess) {
                        _notification.value = UiNotification("Đã lưu & kết nối thành công: ${result.getOrNull()}")
                        repository.syncAll()
                    } else {
                        _notification.value = UiNotification(
                            message = "Đã lưu, nhưng kiểm tra thất bại: ${result.exceptionOrNull()?.localizedMessage}",
                            isError = true
                        )
                    }
                } else {
                    _notification.value = UiNotification("Đã lưu thông tin cấu hình")
                }
            } catch (e: Exception) {
                _notification.value = UiNotification("Lỗi lưu cấu hình: ${e.localizedMessage}", isError = true)
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    fun saveVirtualPins(pin1: String, name1: String, pin2: String, name2: String) {
        viewModelScope.launch {
            preferences.saveVirtualPins(pin1, name1, pin2, name2)
            repository.requestTileUpdate(1)
            repository.requestTileUpdate(2)
            _notification.value = UiNotification("Đã cập nhật Virtual Pin: $pin1 ($name1) & $pin2 ($name2)")
            syncAll()
        }
    }

    fun saveBackgroundSettings(imageUri: String, blurFraction: Float, cropScale: Float, cropOffsetY: Float) {
        viewModelScope.launch {
            preferences.saveBackgroundSettings(imageUri, blurFraction, cropScale, cropOffsetY)
            _notification.value = UiNotification("Đã cập nhật hình nền theme 9:16")
        }
    }

    fun saveThemeMode(mode: String) {
        viewModelScope.launch {
            preferences.saveThemeMode(mode)
        }
    }

    fun saveAutoSyncSettings(enabled: Boolean, intervalMs: Int, notify: Boolean) {
        viewModelScope.launch {
            preferences.saveAutoSyncSettings(enabled, intervalMs, notify)
            val timeText = if (intervalMs < 1000) "${intervalMs / 1000f}s (Siêu tốc)" else "${intervalMs / 1000}s"
            _notification.value = UiNotification(
                if (enabled) "Đã bật tự động đồng bộ công tắc ($timeText)" else "Đã tắt tự động đồng bộ"
            )
            startAutoSyncLoop()
        }
    }

    fun changeAppIcon(iconKey: String) {
        viewModelScope.launch {
            val success = AppIconManager.applyIcon(getApplication(), iconKey)
            if (success) {
                preferences.saveActiveAppIcon(iconKey)
                val iconName = AppIconManager.ICONS.find { it.key == iconKey }?.name ?: iconKey
                _notification.value = UiNotification("Đã đổi icon ứng dụng sang: $iconName")
            } else {
                _notification.value = UiNotification("Không thể đổi icon ứng dụng", isError = true)
            }
        }
    }

    fun clearNotification() {
        _notification.value = null
    }

    fun requestAddTile(tileNumber: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val context = getApplication<Application>()
            val statusBarManager = context.getSystemService(StatusBarManager::class.java) ?: return false

            val (cls, label) = if (tileNumber == 1) {
                Light1TileService::class.java to light1Name.value
            } else {
                Light2TileService::class.java to light2Name.value
            }

            val component = ComponentName(context, cls)
            val icon = Icon.createWithResource(context, R.drawable.ic_lightbulb)

            statusBarManager.requestAddTileService(
                component,
                label,
                icon,
                Executors.newSingleThreadExecutor()
            ) { resultCode ->
                val msg = when (resultCode) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Đã thêm $label vào Control Center!"
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "$label đã có trong Control Center."
                    else -> "Yêu cầu thêm $label đã bị hủy."
                }
                viewModelScope.launch {
                    _notification.value = UiNotification(msg)
                }
            }
            return true
        }
        return false
    }
}
