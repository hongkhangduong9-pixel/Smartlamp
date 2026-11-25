package com.example.data

import android.content.ComponentName
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.service.quicksettings.TileService
import com.example.model.CloudStatus
import com.example.model.LightCardConfig
import com.example.model.LightPowerState
import com.example.model.LightRuntimeState
import com.example.network.BlynkApi
import com.example.network.BlynkApiClient
import com.example.network.BlynkApiException
import com.example.network.ConnectivityTestResult
import com.example.tiles.TileRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BlynkController(
    private val context: Context,
    private val repository: SettingsRepository,
    private val api: BlynkApi,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()

    private val _lightStates = MutableStateFlow<Map<String, LightRuntimeState>>(emptyMap())
    val lightStates: StateFlow<Map<String, LightRuntimeState>> = _lightStates.asStateFlow()

    private val _cloudStatus = MutableStateFlow(CloudStatus.CHECKING)
    val cloudStatus: StateFlow<CloudStatus> = _cloudStatus.asStateFlow()

    private val _lastErrorBanner = MutableStateFlow<String?>(null)
    val lastErrorBanner: StateFlow<String?> = _lastErrorBanner.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(checkCurrentConnectivity())
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private var syncJob: Job? = null
    private var consecutiveFailures = 0

    init {
        registerNetworkCallback()
    }

    fun startForegroundSync() {
        if (syncJob?.isActive == true) return
        syncJob = scope.launch {
            while (isActive) {
                val settings = repository.getSettingsSnapshot()
                val lights = repository.getLightsSnapshot()

                if (!_isNetworkAvailable.value) {
                    _cloudStatus.value = CloudStatus.OFFLINE
                    delay(1000L)
                    continue
                }

                if (lights.isNotEmpty()) {
                    syncMutex.withLock {
                        performBatchSync(settings.blynkServerUrl, lights)
                    }
                } else {
                    // Verify server status occasionally only if needed; idle gracefully when no lights exist
                    _cloudStatus.value = if (_isNetworkAvailable.value) {
                        CloudStatus.ONLINE
                    } else {
                        CloudStatus.OFFLINE
                    }
                }

                val baseDelay = settings.syncIntervalMs
                val effectiveDelay = if (consecutiveFailures > 0) {
                    // Back off gently when offline/unreachable to avoid spamming requests
                    (baseDelay * (1L shl consecutiveFailures.coerceAtMost(4)))
                        .coerceIn(1500L, 5000L)
                } else {
                    baseDelay
                }
                delay(effectiveDelay)
            }
        }
    }

    fun stopForegroundSync() {
        syncJob?.cancel()
        syncJob = null
    }

    fun triggerImmediateSync() {
        scope.launch {
            if (!_isNetworkAvailable.value) {
                _cloudStatus.value = CloudStatus.OFFLINE
                return@launch
            }
            val settings = repository.getSettingsSnapshot()
            val lights = repository.getLightsSnapshot()
            if (lights.isEmpty()) {
                val test = api.testConnectivity(settings.blynkServerUrl, null)
                _cloudStatus.value = if (test.serverReachable) CloudStatus.ONLINE else CloudStatus.OFFLINE
                return@launch
            }
            syncMutex.withLock {
                performBatchSync(settings.blynkServerUrl, lights)
            }
        }
    }

    fun dismissErrorBanner() {
        _lastErrorBanner.value = null
    }

    suspend fun toggleLight(lightId: String): Result<LightPowerState> {
        val lights = repository.getLightsSnapshot()
        val card = lights.firstOrNull { it.id == lightId }
            ?: return Result.failure(IllegalArgumentException("Light configuration not found"))

        if (card.authToken.isBlank()) {
            val msg = "${card.name}: Missing Blynk AuthToken"
            updateCardState(card.id) { it.copy(lastError = msg, isToggling = false) }
            _lastErrorBanner.value = msg
            return Result.failure(BlynkApiException(msg))
        }

        val serverUrl = repository.getSettingsSnapshot().blynkServerUrl
        updateCardState(card.id) { it.copy(isToggling = true, lastError = null) }

        val result = api.toggleLight(serverUrl, card.authToken, card.virtualPin)
        result.fold(
            onSuccess = { newState ->
                consecutiveFailures = 0
                _cloudStatus.value = CloudStatus.ONLINE
                _lastErrorBanner.value = null
                updateCardState(card.id) {
                    it.copy(
                        powerState = newState,
                        isToggling = false,
                        lastError = null,
                        lastUpdatedMs = System.currentTimeMillis(),
                    )
                }
                requestTileRefresh()
            },
            onFailure = { error ->
                // Never revert or assume OFF on failure: keep existing powerState untouched!
                val isOfflineError = (error as? BlynkApiException)?.isConnectivityFailure == true
                if (isOfflineError) {
                    _cloudStatus.value = CloudStatus.OFFLINE
                }
                val errMsg = error.message ?: "Toggle command failed"
                updateCardState(card.id) {
                    it.copy(
                        isToggling = false,
                        lastError = errMsg,
                    )
                }
                _lastErrorBanner.value = "${card.name}: $errMsg"
            },
        )
        return result
    }

    suspend fun testServerConnectivity(overrideUrl: String? = null): ConnectivityTestResult {
        val settings = repository.getSettingsSnapshot()
        val targetUrl = overrideUrl?.takeIf { it.isNotBlank() } ?: settings.blynkServerUrl
        val sampleToken = repository.getLightsSnapshot().firstOrNull { it.authToken.isNotBlank() }?.authToken
        val result = api.testConnectivity(targetUrl, sampleToken)
        _cloudStatus.value = if (result.serverReachable) CloudStatus.ONLINE else CloudStatus.OFFLINE
        return result
    }

    fun requestTileRefresh() {
        try {
            TileRegistry.allTileClasses.forEach { clazz ->
                TileService.requestListeningState(
                    context.applicationContext,
                    ComponentName(context.applicationContext, clazz),
                )
            }
        } catch (_: Throwable) {
            // Ignore in test environments
        }
    }

    private suspend fun performBatchSync(serverUrl: String, lights: List<LightCardConfig>) {
        val validLights = lights.filter { it.authToken.isNotBlank() }
        if (validLights.isEmpty()) {
            _cloudStatus.value = if (_isNetworkAvailable.value) CloudStatus.ONLINE else CloudStatus.OFFLINE
            return
        }

        // Group cards by AuthToken so multiple pins on one ESP32 share ONE HTTP request
        val groupedByToken = validLights.groupBy { it.authToken.trim() }
        var anySuccess = false
        var anyConnectivityFailure = false
        var lastErrorMsg: String? = null

        for ((token, cards) in groupedByToken) {
            val pins = cards.map { BlynkApiClient.normalizePinForApi(it.virtualPin) }
            val batchResult = api.readMultiplePins(serverUrl, token, pins)
            batchResult.fold(
                onSuccess = { pinMap ->
                    anySuccess = true
                    val now = System.currentTimeMillis()
                    cards.forEach { card ->
                        val normalizedPin = BlynkApiClient.normalizePinForApi(card.virtualPin)
                        val newPower = pinMap[normalizedPin]
                        if (newPower != null) {
                            updateCardState(card.id) { current ->
                                if (current.isToggling) {
                                    current
                                } else {
                                    current.copy(
                                        powerState = newPower,
                                        lastError = null,
                                        lastUpdatedMs = now,
                                    )
                                }
                            }
                        }
                    }
                },
                onFailure = { error ->
                    val blynkErr = error as? BlynkApiException
                    if (blynkErr?.isConnectivityFailure == true) {
                        anyConnectivityFailure = true
                    }
                    lastErrorMsg = error.message
                },
            )
        }

        if (anySuccess && !anyConnectivityFailure) {
            consecutiveFailures = 0
            _cloudStatus.value = CloudStatus.ONLINE
        } else if (anyConnectivityFailure || !anySuccess) {
            consecutiveFailures = (consecutiveFailures + 1).coerceAtMost(10)
            _cloudStatus.value = CloudStatus.OFFLINE
            if (lastErrorMsg != null && _lastErrorBanner.value == null && consecutiveFailures == 1) {
                // Keep status bar clear; avoid noisy persistent banner unless command failed
            }
        }
    }

    private fun updateCardState(
        lightId: String,
        transform: (LightRuntimeState) -> LightRuntimeState,
    ) {
        _lightStates.update { map ->
            val current = map[lightId] ?: LightRuntimeState()
            map + (lightId to transform(current))
        }
    }

    private fun checkCurrentConnectivity(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return true
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) {
            true
        }
    }

    private fun registerNetworkCallback() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isNetworkAvailable.value = true
                    consecutiveFailures = 0
                    triggerImmediateSync()
                }

                override fun onLost(network: Network) {
                    val stillConnected = checkCurrentConnectivity()
                    _isNetworkAvailable.value = stillConnected
                    if (!stillConnected) {
                        _cloudStatus.value = CloudStatus.OFFLINE
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(callback)
            }
        } catch (_: Throwable) {
            // Ignore in Robolectric/restricted environments
        }
    }
}
