package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BlynkLightApplication
import com.example.data.BlynkController
import com.example.data.SettingsRepository
import com.example.model.AppSettings
import com.example.model.CloudStatus
import com.example.model.LightCardConfig
import com.example.model.LightRuntimeState
import com.example.model.ThemeSettings
import com.example.network.ConnectivityTestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val lights: List<LightCardConfig> = emptyList(),
    val runtimeStates: Map<String, LightRuntimeState> = emptyMap(),
    val settings: AppSettings = AppSettings(),
    val cloudStatus: CloudStatus = CloudStatus.CHECKING,
    val errorBanner: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BlynkLightApplication
    private val repository: SettingsRepository = app.settingsRepository
    private val controller: BlynkController = app.blynkController

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.lightsFlow,
        controller.lightStates,
        repository.settingsFlow,
        controller.cloudStatus,
        controller.lastErrorBanner,
    ) { lights, states, settings, cloudStatus, errorBanner ->
        DashboardUiState(
            lights = lights,
            runtimeStates = states,
            settings = settings,
            cloudStatus = cloudStatus,
            errorBanner = errorBanner,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(),
    )

    private val _connectivityTestResult = MutableStateFlow<ConnectivityTestResult?>(null)
    val connectivityTestResult: StateFlow<ConnectivityTestResult?> = _connectivityTestResult.asStateFlow()

    private val _isTestingConnectivity = MutableStateFlow(false)
    val isTestingConnectivity: StateFlow<Boolean> = _isTestingConnectivity.asStateFlow()

    fun onToggleLight(lightId: String) {
        viewModelScope.launch {
            controller.toggleLight(lightId)
        }
    }

    fun onRefreshNow() {
        controller.triggerImmediateSync()
    }

    fun dismissErrorBanner() {
        controller.dismissErrorBanner()
    }

    fun addLight(name: String, authToken: String, virtualPin: String) {
        viewModelScope.launch {
            repository.addLight(name, authToken, virtualPin)
            controller.requestTileRefresh()
            controller.triggerImmediateSync()
        }
    }

    fun updateLight(updated: LightCardConfig) {
        viewModelScope.launch {
            repository.updateLight(updated)
            controller.requestTileRefresh()
            controller.triggerImmediateSync()
        }
    }

    fun deleteLight(lightId: String) {
        viewModelScope.launch {
            repository.deleteLight(lightId)
            controller.requestTileRefresh()
        }
    }

    fun updateServerUrl(url: String) {
        viewModelScope.launch {
            repository.updateServerUrl(url)
            controller.triggerImmediateSync()
        }
    }

    fun testServerConnectivity(overrideUrl: String) {
        viewModelScope.launch {
            _isTestingConnectivity.value = true
            _connectivityTestResult.value = controller.testServerConnectivity(overrideUrl)
            _isTestingConnectivity.value = false
        }
    }

    fun clearConnectivityTestResult() {
        _connectivityTestResult.value = null
    }

    fun updateSyncIntervalMs(intervalMs: Long) {
        viewModelScope.launch {
            repository.updateSyncIntervalMs(intervalMs)
        }
    }

    fun updateTileAssignment(slotIndex: Int, lightId: String?) {
        viewModelScope.launch {
            repository.updateTileAssignment(slotIndex, lightId)
            controller.requestTileRefresh()
        }
    }

    fun selectBackgroundImage(uri: Uri, onComplete: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = repository.saveBackgroundFromUri(uri)
            onComplete(result)
        }
    }

    fun updateTheme(transform: (ThemeSettings) -> ThemeSettings) {
        viewModelScope.launch {
            repository.updateThemeSettings(transform)
        }
    }

    fun clearBackgroundImage() {
        viewModelScope.launch {
            repository.clearBackgroundImage()
        }
    }
}
