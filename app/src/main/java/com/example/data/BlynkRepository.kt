package com.example.data

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.example.tile.Light1TileService
import com.example.tile.Light2TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repository acting as single source of truth for ESP32 lights and Blynk Cloud.
 * Dynamically resolves user-configured Virtual Pins (V0, V1, V2, V3...).
 */
class BlynkRepository(private val context: Context) {

    private val preferences = BlynkPreferences.getInstance(context)
    private val apiClient = BlynkApiClient.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _light1State = MutableStateFlow(false)
    val light1State: StateFlow<Boolean> = _light1State.asStateFlow()

    private val _light2State = MutableStateFlow(false)
    val light2State: StateFlow<Boolean> = _light2State.asStateFlow()

    private val _isDeviceConnected = MutableStateFlow<Boolean?>(null)
    val isDeviceConnected: StateFlow<Boolean?> = _isDeviceConnected.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow<Long?>(null)
    val lastLatencyMs: StateFlow<Long?> = _lastLatencyMs.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: BlynkRepository? = null

        fun getInstance(context: Context): BlynkRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BlynkRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        // Load cached states on startup
        scope.launch {
            _light1State.value = preferences.getCachedLightState(1)
            _light2State.value = preferences.getCachedLightState(2)
        }
    }

    /**
     * Toggles a light with optimistic UI update and automatic rollback on failure.
     * @param lightIndex 1 for Light 1, 2 for Light 2
     * @param targetState true to turn on (1), false to turn off (0)
     */
    suspend fun setLightState(lightIndex: Int, targetState: Boolean): Result<Unit> {
        val previousState = if (lightIndex == 1) _light1State.value else _light2State.value
        val pin = if (lightIndex == 1) preferences.getLight1Pin() else preferences.getLight2Pin()

        // 1. Optimistic Update (Immediate)
        updateLocalState(lightIndex, targetState)
        preferences.saveLightState(lightIndex, targetState)
        requestTileUpdate(lightIndex)

        // 2. Perform Network Call
        val token = preferences.getAuthToken()
        val server = preferences.getServerHost()

        val valueToSend = if (targetState) 1 else 0
        val result = apiClient.updatePin(server, token, pin, valueToSend)

        return when (result) {
            is BlynkApiClient.ApiResult.Success -> {
                _lastLatencyMs.value = result.latencyMs
                Result.success(Unit)
            }
            is BlynkApiClient.ApiResult.Error -> {
                // 3. Rollback on failure
                updateLocalState(lightIndex, previousState)
                preferences.saveLightState(lightIndex, previousState)
                requestTileUpdate(lightIndex)
                Result.failure(Exception(result.message))
            }
        }
    }

    /**
     * Synchronizes current light states and hardware connectivity from Blynk Cloud.
     */
    suspend fun syncAll(): Result<Unit> {
        val token = preferences.getAuthToken()
        if (token.isBlank()) {
            return Result.failure(Exception("Vui lòng cấu hình Auth Token trong cài đặt"))
        }
        val server = preferences.getServerHost()
        val pin1 = preferences.getLight1Pin()
        val pin2 = preferences.getLight2Pin()

        _isSyncing.value = true
        return try {
            val deferredV0 = scope.async { apiClient.getPin(server, token, pin1) }
            val deferredV1 = scope.async { apiClient.getPin(server, token, pin2) }
            val deferredHardware = scope.async { apiClient.isHardwareConnected(server, token) }

            val resV0 = deferredV0.await()
            val resV1 = deferredV1.await()
            val resHw = deferredHardware.await()

            if (resV0 is BlynkApiClient.ApiResult.Success) {
                val isOn = resV0.data == 1
                _light1State.value = isOn
                preferences.saveLightState(1, isOn)
            }
            if (resV1 is BlynkApiClient.ApiResult.Success) {
                val isOn = resV1.data == 1
                _light2State.value = isOn
                preferences.saveLightState(2, isOn)
            }
            if (resHw is BlynkApiClient.ApiResult.Success) {
                _isDeviceConnected.value = resHw.data
                _lastLatencyMs.value = resHw.latencyMs
            }

            requestTileUpdate(1)
            requestTileUpdate(2)

            if (resV0 is BlynkApiClient.ApiResult.Error) {
                Result.failure(Exception(resV0.message))
            } else if (resV1 is BlynkApiClient.ApiResult.Error) {
                Result.failure(Exception(resV1.message))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Tests connection with Blynk Cloud and measures latency.
     */
    suspend fun testConnection(server: String, token: String): Result<String> {
        if (token.isBlank()) {
            return Result.failure(Exception("Auth Token không được để trống"))
        }

        val hwResult = apiClient.isHardwareConnected(server, token)
        return when (hwResult) {
            is BlynkApiClient.ApiResult.Success -> {
                _isDeviceConnected.value = hwResult.data
                _lastLatencyMs.value = hwResult.latencyMs
                val statusText = if (hwResult.data) "ESP32 Đang Online" else "Blynk Cloud kết nối thành công (ESP32 đang Offline)"
                Result.success("$statusText • ${hwResult.latencyMs}ms")
            }
            is BlynkApiClient.ApiResult.Error -> {
                Result.failure(Exception(hwResult.message))
            }
        }
    }

    private fun updateLocalState(lightIndex: Int, state: Boolean) {
        if (lightIndex == 1) {
            _light1State.value = state
        } else {
            _light2State.value = state
        }
    }

    fun requestTileUpdate(lightIndex: Int) {
        try {
            when (lightIndex) {
                1 -> TileService.requestListeningState(
                    context,
                    ComponentName(context, Light1TileService::class.java)
                )
                2 -> TileService.requestListeningState(
                    context,
                    ComponentName(context, Light2TileService::class.java)
                )
            }
        } catch (_: Exception) {
            // System will refresh tile on next display
        }
    }
}
